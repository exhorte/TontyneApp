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
 * <p>Les adresses proviennent de proprietes externalisees
 * (app.comptes.*.email), elles-memes alimentees par les variables
 * d'environnement TONTYN_EMAIL_ADMIN, TONTYN_EMAIL_GESTIONNAIRE et
 * TONTYN_EMAIL_MEMBRE, ou par le fichier application-local.properties.
 * Aucune adresse reelle ne figure donc dans le depot.</p>
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

    @Value("${app.comptes.gestionnaire.email}")
    private String emailGestionnaire;
    @Value("${app.comptes.gestionnaire.nom}")
    private String nomGestionnaire;
    @Value("${app.comptes.gestionnaire.prenom}")
    private String prenomGestionnaire;

    @Value("${app.comptes.membre.email}")
    private String emailMembre;
    @Value("${app.comptes.membre.nom}")
    private String nomMembre;
    @Value("${app.comptes.membre.prenom}")
    private String prenomMembre;

    @Bean
    CommandLineRunner initialiserComptes(UtilisateurRepository repository,
                                         PasswordEncoder encoder) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            creer(repository, encoder, nomAdmin, prenomAdmin,
                    emailAdmin, "Admin@1234", Role.ADMINISTRATEUR);
            creer(repository, encoder, nomGestionnaire, prenomGestionnaire,
                    emailGestionnaire, "Gestion@1234", Role.GESTIONNAIRE);
            creer(repository, encoder, nomMembre, prenomMembre,
                    emailMembre, "Membre@1234", Role.MEMBRE);

            log.info("[PROFIL H2] {} comptes crees.", repository.count());
            log.info("[PROFIL H2] Administrateur : {}", emailAdmin);
            log.info("[PROFIL H2] Gestionnaire   : {}", emailGestionnaire);
            log.info("[PROFIL H2] Membre         : {}", emailMembre);
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
