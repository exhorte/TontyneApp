package sn.isi.tontyn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.isi.tontyn.model.MessageConversation;

import java.util.List;

public interface MessageConversationRepository extends JpaRepository<MessageConversation, Long> {

    List<MessageConversation> findByUtilisateurIdOrderByHorodatageAsc(Long utilisateurId);
}
