package sn.isi.tontinesafe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.isi.tontinesafe.model.Tontine;

import java.util.List;

public interface TontineRepository extends JpaRepository<Tontine, Long> {

    List<Tontine> findByStatut(String statut);

    List<Tontine> findByNomContainingIgnoreCase(String nom);

    /** Tontines auxquelles un utilisateur participe. */
    @Query("select m.tontine from Membre m where m.utilisateur.id = :utilisateurId")
    List<Tontine> findByUtilisateurId(@Param("utilisateurId") Long utilisateurId);
}
