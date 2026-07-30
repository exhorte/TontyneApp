package sn.isi.tontinesafe.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.isi.tontinesafe.dto.NotificationRequest;
import sn.isi.tontinesafe.dto.NotificationResponse;
import sn.isi.tontinesafe.exception.ConflitMetierException;
import sn.isi.tontinesafe.exception.RessourceIntrouvableException;
import sn.isi.tontinesafe.model.Notification;
import sn.isi.tontinesafe.model.Utilisateur;
import sn.isi.tontinesafe.repository.NotificationRepository;
import sn.isi.tontinesafe.repository.UtilisateurRepository;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EmailService emailService;

    public NotificationService(NotificationRepository notificationRepository,
                               UtilisateurRepository utilisateurRepository,
                               EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> lister() {
        return notificationRepository.findAll().stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listerParUtilisateur(Long utilisateurId) {
        chargerUtilisateur(utilisateurId);
        return notificationRepository.findByUtilisateurIdOrderByDateEnvoiDesc(utilisateurId)
                .stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public NotificationResponse obtenir(Long id) {
        return NotificationResponse.from(chargerNotification(id));
    }

    /** Endpoint metier : envoie et historise une notification. */
    public NotificationResponse envoyer(NotificationRequest req) {
        Utilisateur destinataire = chargerUtilisateur(req.utilisateurId());
        String canal = req.canal() != null ? req.canal() : "EMAIL";
        return NotificationResponse.from(
                envoyer(destinataire, req.type(), req.message(), canal));
    }

    /** Variante interne utilisee par les autres services (paiement, cycle...). */
    public Notification envoyer(Utilisateur destinataire, String type,
                                String message, String canal) {
        Notification notification = new Notification();
        notification.setUtilisateur(destinataire);
        notification.setType(type);
        notification.setMessage(message);
        notification.setCanal(canal);

        if ("EMAIL".equals(canal)) {
            emailService.envoyer(destinataire.getEmail(), "TontineSafe - " + type, message);
            notification.setStatut("ENVOYEE");
        } else {
            // SMS / PUSH : passerelle non branchee a ce stade, la trace est conservee.
            notification.setStatut("EN_ATTENTE");
        }
        return notificationRepository.save(notification);
    }

    public NotificationResponse marquerLue(Long id) {
        Notification notification = chargerNotification(id);
        if ("LUE".equals(notification.getStatut())) {
            throw new ConflitMetierException("Cette notification est deja marquee comme lue.");
        }
        notification.setStatut("LUE");
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    public void supprimer(Long id) {
        notificationRepository.delete(chargerNotification(id));
    }

    @Transactional(readOnly = true)
    public long compterNonLues(Long utilisateurId) {
        chargerUtilisateur(utilisateurId);
        return notificationRepository.countByUtilisateurIdAndStatut(utilisateurId, "ENVOYEE");
    }

    private Notification chargerNotification(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Notification", id));
    }

    private Utilisateur chargerUtilisateur(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur", id));
    }
}
