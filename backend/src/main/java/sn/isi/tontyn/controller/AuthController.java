package sn.isi.tontyn.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sn.isi.tontyn.dto.*;
import sn.isi.tontyn.service.AuthService;

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

    /** Profil de l'utilisateur authentifie (id, nom, e-mail, role...). */
    @GetMapping("/me")
    public ResponseEntity<UtilisateurResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.moi(authentication.getName()));
    }
}
