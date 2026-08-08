package sn.isi.tontyn.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import sn.isi.tontyn.model.Role;
import sn.isi.tontyn.model.Utilisateur;
import sn.isi.tontyn.repository.UtilisateurRepository;

/**
 * Comptes crees au demarrage sur le profil h2 (base en memoire).
 *
 * <p>Un seul compte porte le role global ADMINISTRATEUR, reserve a l'exploitant
 * de la plateforme. Les autres sont de simples membres : chacun peut creer une
 * tontine et en devient alors administrateur, sans droit sur les tontines des
 * autres.</p>
 *
 * <p>Les adresses proviennent de proprietes externalisees, alimentees par les
 * variables d'environnement TONTYN_EMAIL_ADMIN, TONTYN_EMAIL_MEMBRE1 et
 * TONTYN_EMAIL_MEMBRE2, ou par le fichier application-local.properties. Aucune
 * adresse reelle ne figure donc dans le depot.</p>
 */
@Configuration
@Profile("h2")
public class JeuDeDonneesH2 {

    private static final Logger log = LoggerFactory.getLogger(JeuDeDonneesH2.class);

    @Value("${app.comptes.admin.email}")
    private String emailAdmin;
    @Value("${app.comptes.admin.nom}")
    private String nomAdmin;
    @Value("${app.comptes.admin.prenom}")
    private String prenomAdmin;

    @Value("${app.comptes.membre1.email}")
    private String emailMembre1;
    @Value("${app.comptes.membre1.nom}")
    private String nomMembre1;
    @Value("${app.comptes.membre1.prenom}")
    private String prenomMembre1;

    @Value("${app.comptes.membre2.email}")
    private String emailMembre2;
    @Value("${app.comptes.membre2.nom}")
    private String nomMembre2;
    @Value("${app.comptes.membre2.prenom}")
    private String prenomMembre2;

    @Bean
    CommandLineRunner initialiserComptes(UtilisateurRepository repository,
                                         PasswordEncoder encodeur) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            creer(repository, encodeur, nomAdmin, prenomAdmin,
                    emailAdmin, "Admin@1234", Role.ADMINISTRATEUR);
            creer(repository, encodeur, nomMembre1, prenomMembre1,
                    emailMembre1, "Membre@1234", Role.MEMBRE);
            creer(repository, encodeur, nomMembre2, prenomMembre2,
                    emailMembre2, "Membre@1234", Role.MEMBRE);

            log.info("[PROFIL H2] {} comptes crees.", repository.count());
            log.info("[PROFIL H2] Administrateur plateforme : {} / Admin@1234", emailAdmin);
            log.info("[PROFIL H2] Membre 1                  : {} / Membre@1234", emailMembre1);
            log.info("[PROFIL H2] Membre 2                  : {} / Membre@1234", emailMembre2);
            log.info("[PROFIL H2] Rappel : tout membre peut creer une tontine "
                    + "et en devient administrateur.");
        };
    }

    private void creer(UtilisateurRepository repository, PasswordEncoder encodeur,
                       String nom, String prenom, String email, String motDePasse, Role role) {
        Utilisateur u = new Utilisateur();
        u.setNom(nom);
        u.setPrenom(prenom);
        u.setEmail(email);
        u.setTelephone("+221770000000");
        u.setMotDePasse(encodeur.encode(motDePasse));
        u.setRole(role);
        repository.save(u);
    }
}
