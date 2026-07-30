package sn.isi.tontinesafe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "utilisateur")
@Getter @Setter @NoArgsConstructor
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private String prenom;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String motDePasse;      // hache avec BCrypt

    private String telephone;

    @Enumerated(EnumType.STRING)
    private Role role = Role.MEMBRE;

    private boolean actif = true;

    // Champs pour la 2FA (code OTP transmis par e-mail)
    private String otpCode;
    private LocalDateTime otpExpiration;
}
