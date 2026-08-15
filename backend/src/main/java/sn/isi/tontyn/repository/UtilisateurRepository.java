package sn.isi.tontyn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.isi.tontyn.model.Utilisateur;

import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    /** Recherche par identifiant principal (numero au format international). */
    Optional<Utilisateur> findByTelephone(String telephone);

    boolean existsByTelephone(String telephone);

    /** L'adresse electronique reste facultative : elle peut etre absente. */
    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);
}
