package sn.isi.tontyn.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sn.isi.tontyn.dto.AssistantQuestionRequest;
import sn.isi.tontyn.dto.AssistantResponse;
import sn.isi.tontyn.dto.MembreResponse;
import sn.isi.tontyn.exception.AssistantIndisponibleException;
import sn.isi.tontyn.model.Utilisateur;
import sn.isi.tontyn.security.SecuriteTontine;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Verifie, au niveau du point d'entree de l'assistant, que le message
 * effectivement envoye au modele de langage ne contient jamais les donnees
 * sensibles d'un autre membre — meme quand la question posee les demande
 * explicitement — et que la persistance de l'historique (CU-06) se declenche
 * uniquement apres un appel reussi.
 */
class AssistantServiceTest {

    private static final Long TONTINE_ID = 100L;
    private static final Long MOI_MEMBRE_ID = 10L;
    private static final Long AUTRE_MEMBRE_ID = 20L;
    private static final String AUTRE_TELEPHONE = "+221799999999";

    private final MembreService membreService = mock(MembreService.class);
    private final CotisationService cotisationService = mock(CotisationService.class);
    private final CycleService cycleService = mock(CycleService.class);
    private final ContexteAssistant contexteAssistant =
            new ContexteAssistant(membreService, cotisationService, cycleService);

    private final SecuriteTontine securite = mock(SecuriteTontine.class);
    private final ClientDeepSeek clientDeepSeek = mock(ClientDeepSeek.class);
    private final HistoriqueConversationService historiqueConversationService =
            mock(HistoriqueConversationService.class);

    private final AssistantService assistantService = new AssistantService(
            securite, contexteAssistant, clientDeepSeek, historiqueConversationService);

    @Test
    void laReponseSurUnAutreMembreNeTransmetJamaisSesDonneesSensibles() throws Exception {
        Utilisateur moi = utilisateur(1L, "Awa");
        MembreResponse monAdhesion = membreResponse(MOI_MEMBRE_ID, "Awa Diop", "+221700000001", 1);
        MembreResponse autreMembre = membreResponse(AUTRE_MEMBRE_ID, "Fatou Fall", AUTRE_TELEPHONE, 2);

        when(membreService.listerParUtilisateur(1L)).thenReturn(List.of(monAdhesion));
        when(cotisationService.listerParMembre(MOI_MEMBRE_ID)).thenReturn(List.of());
        when(cycleService.listerParTontine(TONTINE_ID)).thenReturn(List.of());
        when(membreService.listerParTontine(TONTINE_ID))
                .thenReturn(List.of(monAdhesion, autreMembre));

        when(securite.utilisateurCourant()).thenReturn(Optional.of(moi));
        when(clientDeepSeek.repondre(any(), any())).thenReturn("Je n'ai pas cette information.");
        forcerParametresParDefaut();

        AssistantResponse reponse = assistantService.repondre(
                new AssistantQuestionRequest("Quel est le numero de telephone de Fatou ?"));

        assertThat(reponse.disponible()).isTrue();

        ArgumentCaptor<String> messageEnvoye = ArgumentCaptor.forClass(String.class);
        verify(clientDeepSeek).repondre(any(), messageEnvoye.capture());

        assertThat(messageEnvoye.getValue()).contains("Fatou Fall");
        assertThat(messageEnvoye.getValue()).doesNotContain(AUTRE_TELEPHONE);

        verify(historiqueConversationService).enregistrerEchange(
                eq(moi), eq("Quel est le numero de telephone de Fatou ?"),
                eq("Je n'ai pas cette information."));
    }

    @Test
    void aucunAppelReussi_aucunePersistance() throws Exception {
        Utilisateur moi = utilisateur(1L, "Awa");
        when(membreService.listerParUtilisateur(1L)).thenReturn(List.of());
        when(securite.utilisateurCourant()).thenReturn(Optional.of(moi));
        when(clientDeepSeek.repondre(any(), any()))
                .thenThrow(new AssistantIndisponibleException("Panne simulee."));
        forcerParametresParDefaut();

        AssistantResponse reponse = assistantService.repondre(new AssistantQuestionRequest("Bonjour"));

        assertThat(reponse.disponible()).isFalse();
        verify(historiqueConversationService, never()).enregistrerEchange(any(), any(), any());
    }

    /** Champs @Value non injectes hors contexte Spring : valeurs par defaut forcees ici. */
    private void forcerParametresParDefaut() throws Exception {
        set("actif", true);
        set("cleApi", "cle-test");
        set("maxLongueurQuestion", 300);
        set("limiteAppels", 10);
        set("limiteFenetreMinutes", 10);
    }

    private void set(String champ, Object valeur) throws Exception {
        Field f = AssistantService.class.getDeclaredField(champ);
        f.setAccessible(true);
        f.set(assistantService, valeur);
    }

    private static Utilisateur utilisateur(Long id, String prenom) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setPrenom(prenom);
        u.setNom("Diop");
        return u;
    }

    private static MembreResponse membreResponse(Long id, String nomComplet, String telephone,
                                                  int ordreTour) {
        return new MembreResponse(id, LocalDate.now(), "MEMBRE", ordreTour, "ACTIF",
                id, nomComplet, telephone, TONTINE_ID, "Tontine Test", null, null);
    }
}
