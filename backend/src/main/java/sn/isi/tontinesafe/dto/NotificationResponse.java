package sn.isi.tontinesafe.dto;

import sn.isi.tontinesafe.model.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(Long id,
                                   String type,
                                   String message,
                                   LocalDateTime dateEnvoi,
                                   String canal,
                                   String statut,
                                   Long utilisateurId,
                                   String destinataire) {

    public static NotificationResponse from(Notification n) {
        var u = n.getUtilisateur();
        return new NotificationResponse(n.getId(), n.getType(), n.getMessage(),
                n.getDateEnvoi(), n.getCanal(), n.getStatut(), u.getId(), u.getEmail());
    }
}
