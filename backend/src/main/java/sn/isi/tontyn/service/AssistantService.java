package sn.isi.tontyn.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sn.isi.tontyn.dto.AssistantQuestionRequest;
import sn.isi.tontyn.dto.AssistantResponse;
import sn.isi.tontyn.dto.MessageConversationResponse;
import sn.isi.tontyn.exception.AssistantIndisponibleException;
import sn.isi.tontyn.model.Utilisateur;
import sn.isi.tontyn.security.SecuriteTontine;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Assistant conversationnel : construit le contexte, interroge DeepSeek,
 * applique les garde-fous. Point d'entree unique pour
 * {@code AssistantController}.
 *
 * <p><strong>Cloisonnement des donnees — regle absolue de cette
 * fonctionnalite, a ne jamais contourner :</strong></p>
 * <ul>
 *   <li>Le contexte envoye a DeepSeek est construit cote serveur, a partir de
 *   {@code SecuriteTontine.utilisateurCourant()}, et de lui seul.</li>
 *   <li>Le modele ne requete rien. Pas d'appel d'outil, pas de generation de
 *   SQL, pas de "function calling" vers nos services. Il recoit un texte de
 *   contexte deja filtre ({@link ContexteAssistant}) et redige une reponse,
 *   rien d'autre.</li>
 *   <li>L'identifiant de l'utilisateur ne vient jamais du corps de la requete
 *   HTTP : toujours du jeton JWT, via {@code SecuriteTontine}.
 *   {@link AssistantQuestionRequest} ne porte d'ailleurs aucun champ
 *   d'identite — accepter un {@code utilisateurId} en parametre permettrait a
 *   n'importe qui de lire les donnees de n'importe qui.</li>
 *   <li>Le contexte ne contient ni numero de telephone, ni adresse
 *   electronique, ni empreinte de piece d'identite, ni pour un tiers (un
 *   co-membre) quoi que ce soit au-dela de la liste blanche stricte tenue par
 *   {@link ContexteAssistant#construirePour} : voir la javadoc de cette
 *   classe pour le detail.</li>
 *   <li>Chaque echange reussi (question + reponse) est persiste par
 *   {@link HistoriqueConversationService#enregistrerEchange}, toujours
 *   rattache a l'utilisateur du jeton — jamais a un autre. La lecture de cet
 *   historique ({@link #historique()}) est soumise a la meme regle : voir
 *   {@code AssistantController}.</li>
 * </ul>
 *
 * <p><strong>Pouvoir d'ecriture : aucun.</strong> Ce service lit et explique.
 * Il n'initie aucun paiement, ne modifie rien, ne suspend personne — le
 * prompt systeme le rappelle egalement au modele, en defense en profondeur.</p>
 */
@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    /**
     * Cadre le modele : reponses fondees uniquement sur le contexte fourni,
     * aucun chiffre invente, aucun pouvoir d'ecriture, francais simple.
     */
    private static final String PROMPT_SYSTEME = """
            Tu es l'assistant de Tontyn, une application de gestion de tontines communautaires au Senegal.

            Regles strictes, sans exception :
            1. Tu reponds UNIQUEMENT a partir des informations du contexte fourni, qui decrit la situation du membre qui te parle et, pour ses tontines, un resume tres limite des autres membres (nom, statut, ordre de passage, retard eventuel sur le tour en cours). Tu ne connais rien d'autre sur qui que ce soit : ni telephone, ni PIN, ni piece d'identite, ni historique de paiement detaille d'un autre membre. Si on te demande une information absente du contexte, dis que tu ne l'as pas plutot que de l'inventer.
            2. Tu ne cites JAMAIS un montant, une date ou un chiffre absent de ce contexte. Si l'information manque, dis-le clairement plutot que de l'inventer.
            3. Tu ne donnes aucun conseil financier, juridique ou fiscal : tu decris la situation, tu ne juges pas si cotiser, emprunter ou epargner est une bonne idee.
            4. Tu ne peux rien modifier : ni confirmer un paiement, ni suspendre un membre, ni changer un ordre de passage. Si on te le demande, explique que cela se fait depuis l'application.
            5. Si la question sort du sujet de sa tontine, rappelle poliment que tu es limite a ca.
            6. Reponds en francais simple, en 2 a 4 phrases, sans jargon technique.
            """;

    private final SecuriteTontine securite;
    private final ContexteAssistant contexteAssistant;
    private final ClientDeepSeek clientDeepSeek;
    private final HistoriqueConversationService historiqueConversationService;

    @Value("${app.assistant.active:true}")
    private boolean actif;

    @Value("${app.assistant.cle-api:}")
    private String cleApi;

    @Value("${app.assistant.max-longueur-question:300}")
    private int maxLongueurQuestion;

    @Value("${app.assistant.limite-appels:10}")
    private int limiteAppels;

    @Value("${app.assistant.limite-fenetre-minutes:10}")
    private int limiteFenetreMinutes;

    /**
     * Horodatage des derniers appels par utilisateur, pour la limitation de
     * debit. En memoire uniquement : se reinitialise a chaque redemarrage et
     * ne se partage pas entre plusieurs instances — sans consequence sur le
     * plan gratuit Render (une seule instance), a revoir si le projet passe a
     * l'echelle.
     */
    private final Map<Long, Deque<Instant>> appelsParUtilisateur = new ConcurrentHashMap<>();

    public AssistantService(SecuriteTontine securite, ContexteAssistant contexteAssistant,
                            ClientDeepSeek clientDeepSeek,
                            HistoriqueConversationService historiqueConversationService) {
        this.securite = securite;
        this.contexteAssistant = contexteAssistant;
        this.clientDeepSeek = clientDeepSeek;
        this.historiqueConversationService = historiqueConversationService;
    }

    /**
     * Historique de conversation du seul utilisateur courant (voir
     * {@code AssistantController}) : aucun identifiant n'est accepte en
     * parametre, pour la meme raison que {@link #repondre}.
     */
    public List<MessageConversationResponse> historique() {
        return securite.utilisateurCourant()
                .map(historiqueConversationService::listerPour)
                .orElse(List.of());
    }

    public AssistantResponse repondre(AssistantQuestionRequest requete) {
        if (!actif || cleApi == null || cleApi.isBlank()) {
            return indisponible("L'assistant n'est pas disponible pour le moment.");
        }

        Utilisateur utilisateur = securite.utilisateurCourant().orElse(null);
        if (utilisateur == null) {
            // Ne devrait pas arriver : l'endpoint exige une authentification.
            // Cas limite (compte supprime apres emission du jeton) traite
            // sans exposer de detail technique.
            log.warn("Question a l'assistant sans utilisateur resolu malgre l'authentification.");
            return indisponible("L'assistant n'est pas disponible pour le moment.");
        }

        String question = requete.question() == null ? "" : requete.question().trim();
        if (question.isEmpty()) {
            return indisponible("Posez-moi une question sur votre tontine.");
        }
        if (question.length() > maxLongueurQuestion) {
            return indisponible(
                    "Votre question est trop longue ; reformulez-la en une phrase ou deux.");
        }
        if (limiteAtteinte(utilisateur.getId())) {
            return indisponible(
                    "Vous avez pose beaucoup de questions recemment ; reessayez dans quelques minutes.");
        }

        String contexte = contexteAssistant.construirePour(utilisateur);
        String messageUtilisateur = contexte + "\nQuestion : " + question;

        try {
            String reponse = clientDeepSeek.repondre(PROMPT_SYSTEME, messageUtilisateur);
            historiqueConversationService.enregistrerEchange(utilisateur, question, reponse);
            return new AssistantResponse(reponse, true);
        } catch (AssistantIndisponibleException e) {
            log.warn("Assistant indisponible pour l'utilisateur {} : {}",
                    utilisateur.getId(), e.getMessage());
            return indisponible(
                    "Je n'ai pas pu obtenir de reponse pour l'instant ; reessayez dans un instant.");
        }
    }

    /** Fenetre glissante simple : purge les appels trop anciens, puis compte. */
    private boolean limiteAtteinte(Long utilisateurId) {
        Deque<Instant> appels = appelsParUtilisateur.computeIfAbsent(
                utilisateurId, id -> new ArrayDeque<>());
        Instant seuil = Instant.now().minus(Duration.ofMinutes(limiteFenetreMinutes));
        synchronized (appels) {
            while (!appels.isEmpty() && appels.peekFirst().isBefore(seuil)) {
                appels.pollFirst();
            }
            if (appels.size() >= limiteAppels) {
                return true;
            }
            appels.addLast(Instant.now());
            return false;
        }
    }

    private AssistantResponse indisponible(String message) {
        return new AssistantResponse(message, false);
    }
}
