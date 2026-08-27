package sn.isi.tontyn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.isi.tontyn.model.MethodePaiement;
import sn.isi.tontyn.model.Paiement;

import java.util.List;
import java.util.Optional;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    Optional<Paiement> findByCotisationId(Long cotisationId);

    Optional<Paiement> findByReference(String reference);

    List<Paiement> findByStatut(String statut);

    List<Paiement> findByMethode(MethodePaiement methode);

    boolean existsByCotisationId(Long cotisationId);

    @Query("select p from Paiement p where p.cotisation.cycle.tontine.id = :tontineId")
    List<Paiement> findByTontineId(@Param("tontineId") Long tontineId);
}
