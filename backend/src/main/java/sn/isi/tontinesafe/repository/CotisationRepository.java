package sn.isi.tontinesafe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.isi.tontinesafe.model.Cotisation;

import java.util.List;

public interface CotisationRepository extends JpaRepository<Cotisation, Long> {

    List<Cotisation> findByCycleId(Long cycleId);

    List<Cotisation> findByMembreId(Long membreId);

    List<Cotisation> findByCycleIdAndStatut(Long cycleId, String statut);

    boolean existsByCycleIdAndMembreId(Long cycleId, Long membreId);

    @Query("select coalesce(sum(c.montant), 0) from Cotisation c "
            + "where c.cycle.id = :cycleId and c.statut = 'PAYEE'")
    double totalPayePourCycle(@Param("cycleId") Long cycleId);

    @Query("select c from Cotisation c where c.cycle.tontine.id = :tontineId")
    List<Cotisation> findByTontineId(@Param("tontineId") Long tontineId);
}
