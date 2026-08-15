package sn.isi.tontyn.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.isi.tontyn.dto.NotificationRequest;
import sn.isi.tontyn.dto.NotificationResponse;
import sn.isi.tontyn.exception.ConflitMetierException;
import sn.isi.tontyn.exception.RessourceIntrouvableException;
import sn.isi.tontyn.model.Notification;
import sn.isi.tontyn.model.Utilisateur;
import sn.isi.tontyn.repository.NotificationRepository;
import sn.isi.tontyn.repository.UtilisateurRepository;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EmailService emailService;
    private final EnvoiSmsService smsService;

    public NotificationService(NotificationRepository notificationRepository,
                               UtilisateurRepository utilisateurRepository,
                               EmailService emailService,
                               EnvoiSmsService smsService) {
        this.notificationRepository = notificationRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.emailService = emailService;
        this.smsService = smsService;
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

        // L'adresse electronique etant desormais facultative, le canal demande
        // n'est pas toujours disponible : on retombe alors sur le message court,
        // le numero de telephone etant, lui, toujours renseigne.
        boolean emailUtilisable = destinataire.isEmailVerifie() && destinataire.getEmail() != null;

        if ("EMAIL".equals(canal) && emailUtilisable) {
            emailService.envoyer(destinataire.getEmail(), "Tontyn - " + type, message);
            notification.setStatut("ENVOYEE");
        } else if (destinataire.getTelephone() != null) {
            smsService.envoyer(destinataire.getTelephone(), "Tontyn : " + message);
            notification.setCanal("SMS");
            notification.setStatut("ENVOYEE");
        } else {
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
