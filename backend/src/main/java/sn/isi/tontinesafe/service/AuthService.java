package sn.isi.tontinesafe.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import sn.isi.tontinesafe.dto.*;
import sn.isi.tontinesafe.model.Role;
import sn.isi.tontinesafe.model.Utilisateur;
import sn.isi.tontinesafe.repository.UtilisateurRepository;
import sn.isi.tontinesafe.security.JwtService;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    public AuthService(UtilisateurRepository utilisateurRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       EmailService emailService) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    /** Inscription : hachage BCrypt du mot de passe. */
    public String inscrire(RegisterRequest req) {
        if (utilisateurRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Cet e-mail est deja utilise.");
        }
        Utilisateur u = new Utilisateur();
        u.setNom(req.nom());
        u.setPrenom(req.prenom());
        u.setEmail(req.email());
        u.setTelephone(req.telephone());
        u.setMotDePasse(passwordEncoder.encode(req.motDePasse()));
        u.setRole(Role.MEMBRE);
        utilisateurRepository.save(u);
        return "Compte cree avec succes.";
    }

    /** 1er facteur : verifie le mot de passe, genere et envoie un code OTP. */
    public String connexion(LoginRequest req) {
        Utilisateur u = utilisateurRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("Identifiants invalides."));
        if (!passwordEncoder.matches(req.motDePasse(), u.getMotDePasse())) {
            throw new IllegalArgumentException("Identifiants invalides.");
        }
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        u.setOtpCode(code);
        u.setOtpExpiration(LocalDateTime.now().plusMinutes(5));
        utilisateurRepository.save(u);
        emailService.envoyerCodeOtp(u.getEmail(), code);
        return "Un code de verification a ete envoye par e-mail.";
    }

    /** 2e facteur : verifie le code OTP et delivre le jeton JWT (id + email + role). */
    public AuthResponse verifierOtp(OtpRequest req) {
        Utilisateur u = utilisateurRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        if (u.getOtpCode() == null || !u.getOtpCode().equals(req.code())
                || u.getOtpExpiration().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Code invalide ou expire.");
        }
        u.setOtpCode(null);
        u.setOtpExpiration(null);
        utilisateurRepository.save(u);
        String token = jwtService.generateToken(u.getId(), u.getEmail(), u.getRole().name());
        return new AuthResponse(token, "Authentification reussie.");
    }

    /** Renvoie l'utilisateur actuellement authentifie (a partir de son e-mail). */
    public UtilisateurResponse moi(String email) {
        Utilisateur u = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        return UtilisateurResponse.from(u);
    }
}
