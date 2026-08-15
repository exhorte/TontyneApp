package sn.isi.tontyn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.isi.tontyn.model.Cotisation;

import java.time.LocalDate;
import java.util.List;

public interface CotisationRepository extends JpaRepository<Cotisation, Long> {

    List<Cotisation> findByCycleId(Long cycleId);

    List<Cotisation> findByMembreId(Long membreId);

    List<Cotisation> findByCycleIdAndStatut(Long cycleId, String statut);

    boolean existsByCycleIdAndMembreId(Long cycleId, Long membreId);

    /** Cotisations dont l'echeance (fin du cycle) est depassee sans etre payees. */
    List<Cotisation> findByStatutAndCycle_DateFinBefore(String statut, LocalDate date);

    /** Utilise par RelanceService (relances periodiques) et par le filtre ?statut=. */
    List<Cotisation> findByStatut(String statut);

    /** Nombre de retards accumules par un membre : sert au seuil d'alerte du gestionnaire. */
    long countByMembreIdAndStatut(Long membreId, String statut);

    @Query("select coalesce(sum(c.montant), 0) from Cotisation c "
            + "where c.cycle.id = :cycleId and c.statut = 'PAYEE'")
    double totalPayePourCycle(@Param("cycleId") Long cycleId);

    @Query("select c from Cotisation c where c.cycle.tontine.id = :tontineId")
    List<Cotisation> findByTontineId(@Param("tontineId") Long tontineId);
}
