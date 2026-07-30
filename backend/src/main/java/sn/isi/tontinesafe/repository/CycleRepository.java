package sn.isi.tontinesafe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.isi.tontinesafe.model.Cycle;

import java.util.List;

public interface CycleRepository extends JpaRepository<Cycle, Long> {

    List<Cycle> findByTontineIdOrderByNumeroAsc(Long tontineId);

    List<Cycle> findByTontineIdAndStatut(Long tontineId, String statut);

    boolean existsByTontineId(Long tontineId);

    long countByTontineId(Long tontineId);
}
