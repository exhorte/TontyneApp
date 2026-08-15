package sn.isi.tontyn.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Compte utilisateur de la plateforme.
 *
 * <p>Depuis la refonte de l'authentification, l'identifiant principal est le
 * <strong>numero de telephone</strong>, conserve au format international. Ce
 * choix repond aux usages observes au Senegal, ou le numero constitue
 * l'identite numerique la plus universellement detenue, bien avant l'adresse
 * electronique.</p>
 *
 * <p>L'adresse electronique demeure facultative : l'utilisateur peut l'ajouter
 * depuis son profil pour disposer d'un second canal de recuperation.</p>
 */
@Entity
@Table(name = "utilisateur")
@Getter @Setter @NoArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String prenom;

    /** Identifiant principal, au format international (exemple : +221771234567). */
    @Column(unique = true, nullable = false, length = 20)
    private String telephone;

    /** Facultative, ajoutee depuis le profil. Unique lorsqu'elle est renseignee. */
    @Column(unique = true)
    private String email;

    /** Vraie une fois l'adresse confirmee par le code envoye a celle-ci. */
    private boolean emailVerifie = false;

    /** Adresse en cours de confirmation, non encore promue dans le champ email. */
    private String emailEnAttente;

    /** Code PIN a quatre chiffres, conserve sous forme d'empreinte BCrypt. */
    @Column(nullable = false)
    private String codePin;

    @Enumerated(EnumType.STRING)
    private Role role = Role.MEMBRE;

    private boolean actif = true;

    // ------------------------------------------------------------------
    //  Second facteur : code a usage unique
    // ------------------------------------------------------------------

    private String otpCode;
    private LocalDateTime otpExpiration;

    /** Canal ayant servi a transmettre le code : SMS ou EMAIL. */
    private String otpCanal;

    /** Code distinct, dedie a la confirmation d'une adresse electronique. */
    private String codeEmail;
    private LocalDateTime codeEmailExpiration;

    // ------------------------------------------------------------------
    //  Protection contre la recherche exhaustive du code PIN
    // ------------------------------------------------------------------

    /**
     * Un PIN a quatre chiffres n'offre que dix mille combinaisons : sans
     * limitation, il serait epuise en quelques minutes. Ce compteur, associe au
     * verrouillage temporaire ci-dessous, rend l'attaque impraticable.
     */
    private int tentativesEchouees = 0;

    /** Instant jusqu'auquel le compte refuse toute tentative de connexion. */
    private LocalDateTime verrouilleJusqua;

    // ------------------------------------------------------------------
    //  Verification d'identite et deplafonnement
    // ------------------------------------------------------------------

    /**
     * Niveau de verification du compte, dont depend le plafond applicable.
     *
     * <p>Trois paliers se succedent : le numero seul, l'etat civil declare avec
     * portrait, puis la piece d'identite validee. A chacun correspond un
     * plafond de tour, defini par la configuration.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NiveauVerification niveauVerification = NiveauVerification.NON_VERIFIE;

    /** Date a laquelle l'identite a ete etablie. */
    private LocalDateTime dateVerification;

    /** Niveau effectif, jamais nul : un compte sans valeur est reput non verifie. */
    public NiveauVerification niveauEffectif() {
        return niveauVerification != null ? niveauVerification : NiveauVerification.NON_VERIFIE;
    }
}
