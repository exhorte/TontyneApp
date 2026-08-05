package sn.isi.tontyn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.isi.tontyn.model.Recu;

import java.util.List;
import java.util.Optional;

public interface RecuRepository extends JpaRepository<Recu, Long> {

    Optional<Recu> findByPaiementId(Long paiementId);

    Optional<Recu> findByNumero(String numero);

    boolean existsByPaiementId(Long paiementId);

    @Query("select r from Recu r where r.paiement.cotisation.membre.id = :membreId")
    List<Recu> findByMembreId(@Param("membreId") Long membreId);
}
