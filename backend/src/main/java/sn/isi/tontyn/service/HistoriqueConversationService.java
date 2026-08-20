package sn.isi.tontyn.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.isi.tontyn.dto.MessageConversationResponse;
import sn.isi.tontyn.model.AuteurMessage;
import sn.isi.tontyn.model.MessageConversation;
import sn.isi.tontyn.model.Utilisateur;
import sn.isi.tontyn.repository.MessageConversationRepository;

import java.util.List;

/**
 * Historique de conversation avec l'assistant (CU-06) : persistance et
 * lecture.
 *
 * <p>Aucune methode de ce service n'accepte d'identifiant d'utilisateur
 * fourni par un appelant : {@link #enregistrerEchange} recoit l'{@link
 * Utilisateur} deja resolu depuis le jeton JWT par {@code AssistantService},
 * et {@link #listerPour} ne lit que celui qu'on lui passe. C'est
 * {@code AssistantController}/{@code AssistantService} qui garantit que ce
 * parametre est toujours l'utilisateur courant, jamais une valeur choisie par
 * le client — le meme principe que pour la construction du contexte (voir
 * {@code ContexteAssistant}). Resultat : un utilisateur, gestionnaire ou non,
 * ne peut jamais obtenir l'historique d'un autre.</p>
 */
@Service
@Transactional
public class HistoriqueConversationService {

    private final MessageConversationRepository repository;

    public HistoriqueConversationService(MessageConversationRepository repository) {
        this.repository = repository;
    }

    /** Persiste la question puis la reponse d'un echange reussi, dans cet ordre. */
    public void enregistrerEchange(Utilisateur utilisateur, String question, String reponse) {
        repository.save(message(utilisateur, AuteurMessage.UTILISATEUR, question));
        repository.save(message(utilisateur, AuteurMessage.ASSISTANT, reponse));
    }

    @Transactional(readOnly = true)
    public List<MessageConversationResponse> listerPour(Utilisateur utilisateur) {
        return repository.findByUtilisateurIdOrderByHorodatageAsc(utilisateur.getId()).stream()
                .map(MessageConversationResponse::from).toList();
    }

    private MessageConversation message(Utilisateur utilisateur, AuteurMessage auteur,
                                        String contenu) {
        MessageConversation m = new MessageConversation();
        m.setUtilisateur(utilisateur);
        m.setAuteur(auteur);
        m.setContenu(contenu);
        return m;
    }
}
