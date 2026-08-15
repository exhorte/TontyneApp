package sn.isi.tontyn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.isi.tontyn.model.DemandeVerification;
import sn.isi.tontyn.model.StatutVerification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DemandeVerificationRepository extends JpaRepository<DemandeVerification, Long> {

    /** Demande la plus recente d'un utilisateur, quel qu'en soit l'etat. */
    Optional<DemandeVerification> findFirstByUtilisateurIdOrderByDateSoumissionDesc(Long utilisateurId);

    List<DemandeVerification> findByUtilisateurIdOrderByDateSoumissionDesc(Long utilisateurId);

    /** File d'instruction, du plus ancien au plus recent. */
    List<DemandeVerification> findByStatutOrderByDateSoumissionAsc(StatutVerification statut);

    boolean existsByUtilisateurIdAndStatut(Long utilisateurId, StatutVerification statut);

    /** Detecte qu'une meme piece a deja servi a un autre compte. */
    List<DemandeVerification> findByEmpreinteNumeroAndStatut(String empreinteNumero,
                                                             StatutVerification statut);

    /**
     * Demandes tranchees avant la date indiquee et dont les images subsistent :
     * ce sont celles que la purge doit traiter.
     */
    List<DemandeVerification> findByImagesPurgeesFalseAndDateDecisionBefore(LocalDateTime limite);

    long countByStatut(StatutVerification statut);
}
