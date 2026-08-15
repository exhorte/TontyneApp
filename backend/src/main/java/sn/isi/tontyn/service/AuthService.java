package sn.isi.tontyn.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.isi.tontyn.dto.AuthResponse;
import sn.isi.tontyn.dto.EmailRequest;
import sn.isi.tontyn.dto.LoginRequest;
import sn.isi.tontyn.dto.OtpRequest;
import sn.isi.tontyn.dto.RegisterRequest;
import sn.isi.tontyn.dto.UtilisateurResponse;
import sn.isi.tontyn.model.Role;
import sn.isi.tontyn.model.Utilisateur;
import sn.isi.tontyn.repository.UtilisateurRepository;
import sn.isi.tontyn.security.JwtService;
import sn.isi.tontyn.util.Telephone;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Authentification a deux facteurs fondee sur le numero de telephone.
 *
 * <p>Le parcours comporte deux etapes. La premiere verifie le code PIN, que
 * l'utilisateur est seul a connaitre. La seconde verifie un code a usage unique
 * transmis par message court, que seul le detenteur du telephone peut lire. La
 * conjonction d'un facteur de connaissance et d'un facteur de possession
 * constitue une authentification a deux facteurs au sens propre.</p>
 *
 * <p>Le code PIN ne comptant que quatre chiffres, la protection contre la
 * recherche exhaustive ne peut reposer sur sa seule entropie : elle est assuree
 * par un verrouillage temporaire declenche apres un nombre limite d'echecs.</p>
 */
@Service
@Transactional
public class AuthService {

    /** Nombre d'echecs consecutifs tolere avant verrouillage. */
    private static final int ECHECS_TOLERES = 5;

    /** Duree du verrouillage une fois le seuil atteint. */
    private static final Duration DUREE_VERROU = Duration.ofMinutes(15);

    /** Duree de validite d'un code a usage unique. */
    private static final Duration DUREE_CODE = Duration.ofMinutes(5);

    private static final SecureRandom ALEA = new SecureRandom();

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder encodeur;
    private final JwtService jwtService;
    private final EnvoiSmsService smsService;
    private final EmailService emailService;

    @Value("${app.auth.exposer-code:false}")
    private boolean exposerCode;

    public AuthService(UtilisateurRepository utilisateurRepository,
                       PasswordEncoder encodeur,
                       JwtService jwtService,
                       EnvoiSmsService smsService,
                       EmailService emailService) {
        this.utilisateurRepository = utilisateurRepository;
        this.encodeur = encodeur;
        this.jwtService = jwtService;
        this.smsService = smsService;
        this.emailService = emailService;
    }

    // ------------------------------------------------------------------
    //  Inscription
    // ------------------------------------------------------------------

    public String inscrire(RegisterRequest req) {
        String numero = exigerNumeroValide(req.telephone());
        if (utilisateurRepository.existsByTelephone(numero)) {
            throw new IllegalArgumentException("Ce numero de telephone est deja utilise.");
        }
        Utilisateur u = new Utilisateur();
        u.setNom(req.nom());
        u.setPrenom(req.prenom());
        u.setTelephone(numero);
        u.setCodePin(encodeur.encode(req.codePin()));
        u.setRole(Role.MEMBRE);
        utilisateurRepository.save(u);
        return "Compte cree. Vous pouvez desormais vous connecter avec votre numero et votre code PIN.";
    }

    // ------------------------------------------------------------------
    //  Premier facteur : le code PIN
    // ------------------------------------------------------------------

    public String connexion(LoginRequest req) {
        String numero = exigerNumeroValide(req.telephone());
        Utilisateur u = utilisateurRepository.findByTelephone(numero)
                .orElseThrow(() -> new IllegalArgumentException("Identifiants invalides."));

        verifierNonVerrouille(u);

        if (!encodeur.matches(req.codePin(), u.getCodePin())) {
            enregistrerEchec(u);
            throw new IllegalArgumentException("Identifiants invalides.");
        }
        if (!u.isActif()) {
            throw new IllegalArgumentException("Ce compte est desactive.");
        }

        u.setTentativesEchouees(0);
        u.setVerrouilleJusqua(null);

        String code = genererCode();
        u.setOtpCode(code);
        u.setOtpExpiration(LocalDateTime.now().plus(DUREE_CODE));
        u.setOtpCanal("SMS");
        utilisateurRepository.save(u);

        smsService.envoyerCodeOtp(u.getTelephone(), code);
        // Confort supplementaire lorsqu'une adresse a ete confirmee.
        if (u.isEmailVerifie() && u.getEmail() != null) {
            emailService.envoyerCodeOtp(u.getEmail(), code);
        }

        String message = "Un code de verification a ete envoye au " + Telephone.masquer(numero) + ".";
        return exposerCode ? message + " [code de developpement : " + code + "]" : message;
    }

    // ------------------------------------------------------------------
    //  Second facteur : le code a usage unique
    // ------------------------------------------------------------------

    public AuthResponse verifierOtp(OtpRequest req) {
        String numero = exigerNumeroValide(req.telephone());
        Utilisateur u = utilisateurRepository.findByTelephone(numero)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));

        verifierNonVerrouille(u);

        boolean codeAbsent = u.getOtpCode() == null || u.getOtpExpiration() == null;
        if (codeAbsent || !u.getOtpCode().equals(req.code())) {
            enregistrerEchec(u);
            throw new IllegalArgumentException("Code invalide ou expire.");
        }
        if (u.getOtpExpiration().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Code expire. Veuillez en demander un nouveau.");
        }

        u.setOtpCode(null);
        u.setOtpExpiration(null);
        u.setOtpCanal(null);
        u.setTentativesEchouees(0);
        u.setVerrouilleJusqua(null);
        utilisateurRepository.save(u);

        String jeton = jwtService.generateToken(u.getId(), u.getTelephone(), u.getRole().name());
        return new AuthResponse(jeton, "Authentification reussie.");
    }

    // ------------------------------------------------------------------
    //  Profil
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public UtilisateurResponse moi(String telephone) {
        return UtilisateurResponse.from(charger(telephone));
    }

    /**
     * Associe une adresse electronique au compte.
     *
     * <p>L'adresse reste en attente jusqu'a confirmation : sans cette etape, un
     * utilisateur pourrait declarer l'adresse d'un tiers et lui faire parvenir
     * des codes de verification.</p>
     */
    public String demanderAjoutEmail(String telephone, EmailRequest.Ajout req) {
        Utilisateur u = charger(telephone);
        String adresse = req.email().trim().toLowerCase();

        utilisateurRepository.findByEmail(adresse).ifPresent(autre -> {
            if (!autre.getId().equals(u.getId())) {
                throw new IllegalArgumentException("Cette adresse est deja associee a un autre compte.");
            }
        });

        String code = genererCode();
        u.setEmailEnAttente(adresse);
        u.setCodeEmail(code);
        u.setCodeEmailExpiration(LocalDateTime.now().plus(DUREE_CODE));
        utilisateurRepository.save(u);

        emailService.envoyerCodeOtp(adresse, code);
        return "Un code de confirmation a ete envoye a " + adresse + ".";
    }

    public UtilisateurResponse confirmerEmail(String telephone, EmailRequest.Confirmation req) {
        Utilisateur u = charger(telephone);
        if (u.getEmailEnAttente() == null || u.getCodeEmail() == null
                || u.getCodeEmailExpiration() == null) {
            throw new IllegalArgumentException("Aucune adresse en attente de confirmation.");
        }
        if (u.getCodeEmailExpiration().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Code expire. Veuillez recommencer.");
        }
        if (!u.getCodeEmail().equals(req.code())) {
            throw new IllegalArgumentException("Code invalide.");
        }

        u.setEmail(u.getEmailEnAttente());
        u.setEmailVerifie(true);
        u.setEmailEnAttente(null);
        u.setCodeEmail(null);
        u.setCodeEmailExpiration(null);
        utilisateurRepository.save(u);
        return UtilisateurResponse.from(u);
    }

    /** Retire l'adresse electronique associee au compte. */
    public UtilisateurResponse retirerEmail(String telephone) {
        Utilisateur u = charger(telephone);
        u.setEmail(null);
        u.setEmailVerifie(false);
        u.setEmailEnAttente(null);
        u.setCodeEmail(null);
        u.setCodeEmailExpiration(null);
        utilisateurRepository.save(u);
        return UtilisateurResponse.from(u);
    }

    // ------------------------------------------------------------------
    //  Utilitaires internes
    // ------------------------------------------------------------------

    private Utilisateur charger(String telephone) {
        return utilisateurRepository.findByTelephone(telephone)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
    }

    private String exigerNumeroValide(String saisie) {
        String numero = Telephone.normaliser(saisie);
        if (!Telephone.estValide(numero)) {
            throw new IllegalArgumentException("Numero de telephone invalide.");
        }
        return numero;
    }

    private String genererCode() {
        return String.format("%06d", ALEA.nextInt(1_000_000));
    }

    private void verifierNonVerrouille(Utilisateur u) {
        LocalDateTime verrou = u.getVerrouilleJusqua();
        if (verrou != null && verrou.isAfter(LocalDateTime.now())) {
            long minutes = Math.max(1, Duration.between(LocalDateTime.now(), verrou).toMinutes());
            throw new IllegalArgumentException(
                    "Trop de tentatives infructueuses. Reessayez dans " + minutes + " minute(s).");
        }
    }

    /** Incremente le compteur d'echecs et verrouille le compte au seuil atteint. */
    private void enregistrerEchec(Utilisateur u) {
        int echecs = u.getTentativesEchouees() + 1;
        u.setTentativesEchouees(echecs);
        if (echecs >= ECHECS_TOLERES) {
            u.setVerrouilleJusqua(LocalDateTime.now().plus(DUREE_VERROU));
            u.setTentativesEchouees(0);
        }
        utilisateurRepository.save(u);
    }
}
