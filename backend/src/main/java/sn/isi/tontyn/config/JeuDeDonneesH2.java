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
import sn.isi.tontyn.util.Telephone;

/**
 * Comptes crees au demarrage sur le profil h2 (base en memoire).
 *
 * <p>Les comptes sont identifies par leur numero de telephone et proteges par
 * un code PIN. Aucune adresse electronique n'est pre-renseignee : elle releve
 * desormais d'une demarche volontaire de l'utilisateur, depuis son profil.</p>
 */
@Configuration
@Profile("h2")
public class JeuDeDonneesH2 {

    private static final Logger log = LoggerFactory.getLogger(JeuDeDonneesH2.class);

    @Value("${app.comptes.admin.telephone}")
    private String telAdmin;
    @Value("${app.comptes.admin.nom}")
    private String nomAdmin;
    @Value("${app.comptes.admin.prenom}")
    private String prenomAdmin;

    @Value("${app.comptes.membre1.telephone}")
    private String telMembre1;
    @Value("${app.comptes.membre1.nom}")
    private String nomMembre1;
    @Value("${app.comptes.membre1.prenom}")
    private String prenomMembre1;

    @Value("${app.comptes.membre2.telephone}")
    private String telMembre2;
    @Value("${app.comptes.membre2.nom}")
    private String nomMembre2;
    @Value("${app.comptes.membre2.prenom}")
    private String prenomMembre2;

    @Value("${app.comptes.code-pin:1234}")
    private String codePinDemo;

    @Bean
    CommandLineRunner initialiserComptes(UtilisateurRepository repository,
                                         PasswordEncoder encodeur) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            creer(repository, encodeur, nomAdmin, prenomAdmin, telAdmin, Role.ADMINISTRATEUR);
            creer(repository, encodeur, nomMembre1, prenomMembre1, telMembre1, Role.MEMBRE);
            creer(repository, encodeur, nomMembre2, prenomMembre2, telMembre2, Role.MEMBRE);

            log.info("[PROFIL H2] {} comptes crees, code PIN commun : {}",
                    repository.count(), codePinDemo);
            log.info("[PROFIL H2] Administrateur plateforme : {}", Telephone.normaliser(telAdmin));
            log.info("[PROFIL H2] Membre 1                  : {}", Telephone.normaliser(telMembre1));
            log.info("[PROFIL H2] Membre 2                  : {}", Telephone.normaliser(telMembre2));
            log.info("[PROFIL H2] Le code de verification s'affiche sur la ligne [SMS SIMULE].");
        };
    }

    private void creer(UtilisateurRepository repository, PasswordEncoder encodeur,
                       String nom, String prenom, String telephone, Role role) {
        Utilisateur u = new Utilisateur();
        u.setNom(nom);
        u.setPrenom(prenom);
        u.setTelephone(Telephone.normaliser(telephone));
        u.setCodePin(encodeur.encode(codePinDemo));
        u.setRole(role);
        repository.save(u);
    }
}
