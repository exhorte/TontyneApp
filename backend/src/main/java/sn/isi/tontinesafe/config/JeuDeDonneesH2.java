package sn.isi.tontinesafe.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import sn.isi.tontinesafe.model.Role;
import sn.isi.tontinesafe.model.Utilisateur;
import sn.isi.tontinesafe.repository.UtilisateurRepository;

/**
 * Comptes de demonstration crees uniquement sur le profil h2 (base en memoire).
 * Permet de tester les endpoints proteges sans PostgreSQL ni jeu de donnees externe.
 */
@Configuration
@Profile("h2")
public class JeuDeDonneesH2 {

    private static final Logger log = LoggerFactory.getLogger(JeuDeDonneesH2.class);

    @Bean
    CommandLineRunner initialiserComptes(UtilisateurRepository repository,
                                         PasswordEncoder encoder) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            creer(repository, encoder, "Diop", "Awa",
                    "admin@tontinesafe.sn", "Admin@1234", Role.ADMINISTRATEUR);
            creer(repository, encoder, "Ndiaye", "Moussa",
                    "gestionnaire@tontinesafe.sn", "Gestion@1234", Role.GESTIONNAIRE);
            creer(repository, encoder, "Fall", "Fatou",
                    "membre1@tontinesafe.sn", "Membre@1234", Role.MEMBRE);
            creer(repository, encoder, "Sow", "Ibrahima",
                    "membre2@tontinesafe.sn", "Membre@1234", Role.MEMBRE);
            creer(repository, encoder, "Ba", "Aminata",
                    "membre3@tontinesafe.sn", "Membre@1234", Role.MEMBRE);

            log.info("[PROFIL H2] {} comptes de demonstration crees "
                    + "(admin@tontinesafe.sn / Admin@1234)", repository.count());
        };
    }

    private void creer(UtilisateurRepository repository, PasswordEncoder encoder,
                       String nom, String prenom, String email, String motDePasse, Role role) {
        Utilisateur u = new Utilisateur();
        u.setNom(nom);
        u.setPrenom(prenom);
        u.setEmail(email);
        u.setTelephone("+221770000000");
        u.setMotDePasse(encoder.encode(motDePasse));
        u.setRole(role);
        repository.save(u);
    }
}
