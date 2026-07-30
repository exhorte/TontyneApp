package sn.isi.tontinesafe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.isi.tontinesafe.model.Utilisateur;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    boolean existsByEmail(String email);
}
