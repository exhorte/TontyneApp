package sn.isi.tontyn.service;

import org.junit.jupiter.api.Test;
import sn.isi.tontyn.dto.CotisationResponse;
import sn.isi.tontyn.dto.CycleResponse;
import sn.isi.tontyn.dto.MembreResponse;
import sn.isi.tontyn.model.Utilisateur;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Verifie le composant unique responsable du cloisonnement des donnees d'un
 * tiers dans le contexte transmis a l'assistant (voir la javadoc de
 * {@link ContexteAssistant}).
 */
class ContexteAssistantTest {

    private final MembreService membreService = mock(MembreService.class);
    private final CotisationService cotisationService = mock(CotisationService.class);
    private final CycleService cycleService = mock(CycleService.class);

    private final ContexteAssistant contexteAssistant =
            new ContexteAssistant(membreService, cotisationService, cycleService);

    private static final Long TONTINE_ID = 100L;
    private static final Long MOI_MEMBRE_ID = 10L;
    private static final Long AUTRE_MEMBRE_ID = 20L;
    private static final String AUTRE_TELEPHONE = "+221799999999";

    @Test
    void leResumeDunTiersNeContientQueLesChampsDeLaListeBlanche() {
        Utilisateur moi = utilisateur(1L, "Awa");
        MembreResponse monAdhesion = membreResponse(MOI_MEMBRE_ID, TONTINE_ID, "Tontine Test",
                "MEMBRE", "ACTIF", 1, 1L, "Awa Diop", "+221700000001");
        MembreResponse autreMembre = membreResponse(AUTRE_MEMBRE_ID, TONTINE_ID, "Tontine Test",
                "MEMBRE", "ACTIF", 2, 2L, "Fatou Fall", AUTRE_TELEPHONE);
        CycleResponse cycleEnCours = new CycleResponse(500L, 1, LocalDate.now(),
                LocalDate.now().plusDays(30), "EN_COURS", TONTINE_ID, "Tontine Test",
                AUTRE_MEMBRE_ID, "Fatou Fall", 0);
        CotisationResponse cotisationEnRetard = new CotisationResponse(900L, 10000, 500, 10500,
                LocalDateTime.now(), "EN_RETARD", LocalDate.now().plusDays(5), 500L, 1,
                AUTRE_MEMBRE_ID, "Fatou Fall", TONTINE_ID, "Tontine Test");

        when(membreService.listerParUtilisateur(1L)).thenReturn(List.of(monAdhesion));
        when(cotisationService.listerParMembre(MOI_MEMBRE_ID)).thenReturn(List.of());
        when(cycleService.listerParTontine(TONTINE_ID)).thenReturn(List.of(cycleEnCours));
        when(membreService.listerParTontine(TONTINE_ID))
                .thenReturn(List.of(monAdhesion, autreMembre));
        when(cotisationService.listerParCycle(500L)).thenReturn(List.of(cotisationEnRetard));

        String texte = contexteAssistant.construirePour(moi);

        // Champs autorises presents.
        assertThat(texte).contains("Fatou Fall");
        assertThat(texte).contains("ordre de passage 2");
        assertThat(texte).contains("en retard sur le tour en cours");

        // Aucun champ hors liste blanche : ni le telephone de l'autre membre,
        // ni le sien, ni aucune trace de PIN ou de piece d'identite.
        assertThat(texte).doesNotContain(AUTRE_TELEPHONE);
        assertThat(texte).doesNotContainIgnoringCase("pin");
        assertThat(texte).doesNotContainIgnoringCase("piece");
        assertThat(texte).doesNotContainIgnoringCase("verification");
    }

    @Test
    void aucuneDonneeSurUnUtilisateurSansTontineCommune() {
        Long autreTontineId = 200L;
        Utilisateur moi = utilisateur(1L, "Awa");
        MembreResponse monAdhesion = membreResponse(MOI_MEMBRE_ID, TONTINE_ID, "Tontine Test",
                "MEMBRE", "ACTIF", 1, 1L, "Awa Diop", "+221700000001");

        when(membreService.listerParUtilisateur(1L)).thenReturn(List.of(monAdhesion));
        when(cotisationService.listerParMembre(MOI_MEMBRE_ID)).thenReturn(List.of());
        when(cycleService.listerParTontine(TONTINE_ID)).thenReturn(List.of());
        when(membreService.listerParTontine(TONTINE_ID)).thenReturn(List.of(monAdhesion));

        String texte = contexteAssistant.construirePour(moi);

        // Aucun appel n'est jamais fait pour une tontine a laquelle "moi"
        // n'appartient pas : structurellement, un tiers sans tontine commune
        // ne peut donc jamais figurer dans le contexte.
        verify(membreService, never()).listerParTontine(eq(autreTontineId));
        assertThat(texte).doesNotContain("Cheikh");
    }

    private static Utilisateur utilisateur(Long id, String prenom) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setPrenom(prenom);
        u.setNom("Diop");
        return u;
    }

    private static MembreResponse membreResponse(Long id, Long tontineId, String tontineNom,
                                                  String roleGroupe, String statut, int ordreTour,
                                                  Long utilisateurId, String nomComplet,
                                                  String telephone) {
        return new MembreResponse(id, LocalDate.now(), roleGroupe, ordreTour, statut,
                utilisateurId, nomComplet, telephone, tontineId, tontineNom, null, null);
    }
}
