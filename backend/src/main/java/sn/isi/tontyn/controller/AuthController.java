package sn.isi.tontyn.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sn.isi.tontyn.dto.AuthResponse;
import sn.isi.tontyn.dto.EmailRequest;
import sn.isi.tontyn.dto.LoginRequest;
import sn.isi.tontyn.dto.OtpRequest;
import sn.isi.tontyn.dto.RegisterRequest;
import sn.isi.tontyn.dto.UtilisateurResponse;
import sn.isi.tontyn.service.AuthService;

/**
 * Points d'entree de l'authentification.
 *
 * <p>Le parcours nominal comporte deux appels : {@code /login} verifie le code
 * PIN et declenche l'envoi du code par message court, {@code /verify-otp}
 * controle ce code et delivre le jeton. Les points relatifs a l'adresse
 * electronique supposent un utilisateur deja authentifie.</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.inscrire(req));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.connexion(req));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody OtpRequest req) {
        return ResponseEntity.ok(authService.verifierOtp(req));
    }

    /** Profil de l'utilisateur authentifie. */
    @GetMapping("/me")
    public ResponseEntity<UtilisateurResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.moi(authentication.getName()));
    }

    // ------------------------------------------------------------------
    //  Adresse electronique facultative, geree depuis le profil
    // ------------------------------------------------------------------

    /** Demande d'association : un code de confirmation part vers l'adresse indiquee. */
    @PostMapping("/email")
    public ResponseEntity<String> ajouterEmail(Authentication authentication,
                                               @Valid @RequestBody EmailRequest.Ajout req) {
        return ResponseEntity.ok(authService.demanderAjoutEmail(authentication.getName(), req));
    }

    /** Confirmation de l'adresse au moyen du code recu. */
    @PostMapping("/email/confirmer")
    public ResponseEntity<UtilisateurResponse> confirmerEmail(
            Authentication authentication,
            @Valid @RequestBody EmailRequest.Confirmation req) {
        return ResponseEntity.ok(authService.confirmerEmail(authentication.getName(), req));
    }

    /** Dissociation de l'adresse electronique. */
    @DeleteMapping("/email")
    public ResponseEntity<UtilisateurResponse> retirerEmail(Authentication authentication) {
        return ResponseEntity.ok(authService.retirerEmail(authentication.getName()));
    }
}
