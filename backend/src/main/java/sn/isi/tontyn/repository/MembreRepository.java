package sn.isi.tontyn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.isi.tontyn.model.Membre;

import java.util.List;
import java.util.Optional;

public interface MembreRepository extends JpaRepository<Membre, Long> {

    List<Membre> findByTontineId(Long tontineId);

    List<Membre> findByUtilisateurId(Long utilisateurId);

    List<Membre> findByTontineIdAndStatutOrderByOrdreTourAsc(Long tontineId, String statut);

    Optional<Membre> findByTontineIdAndUtilisateurId(Long tontineId, Long utilisateurId);

    boolean existsByTontineIdAndUtilisateurId(Long tontineId, Long utilisateurId);

    boolean existsByTontineIdAndOrdreTour(Long tontineId, int ordreTour);

    long countByTontineId(Long tontineId);
}
