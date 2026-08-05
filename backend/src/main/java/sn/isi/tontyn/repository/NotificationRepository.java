package sn.isi.tontyn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.isi.tontyn.model.Notification;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUtilisateurIdOrderByDateEnvoiDesc(Long utilisateurId);

    List<Notification> findByUtilisateurIdAndStatut(Long utilisateurId, String statut);

    long countByUtilisateurIdAndStatut(Long utilisateurId, String statut);
}
