package sn.isi.tontinesafe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.isi.tontinesafe.model.MethodePaiement;
import sn.isi.tontinesafe.model.Paiement;

import java.util.List;
import java.util.Optional;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    Optional<Paiement> findByCotisationId(Long cotisationId);

    Optional<Paiement> findByReference(String reference);

    List<Paiement> findByStatut(String statut);

    List<Paiement> findByMethode(MethodePaiement methode);

    boolean existsByCotisationId(Long cotisationId);
}
