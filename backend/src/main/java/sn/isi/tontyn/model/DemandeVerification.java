package sn.isi.tontyn.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Demande de verification deposee par un utilisateur, quel qu'en soit le niveau.
 *
 * <p>Une seule entite couvre les deux paliers, car ils ne different que par les
 * pieces jointes : la verification simple se contente de l'etat civil declare
 * et d'un portrait ; la verification complete y ajoute une piece d'identite
 * photographiee.</p>
 *
 * <p><strong>Donnees sensibles.</strong> Le traitement releve, au Senegal, de
 * l'autorisation prealable de la Commission de protection des donnees
 * personnelles, et non de la simple declaration (loi n° 2008-12 du 25 janvier
 * 2008). Trois precautions en decoulent, appliquees ici : les images ne sont
 * pas conservees en base, mais dans un repertoire hors arborescence publique ;
 * le numero de la piece n'est jamais enregistre en clair ; les images sont
 * detruites peu apres la decision, les donnees extraites suffisant a justifier
 * le statut acquis.</p>
 */
@Entity
@Table(name = "demande_verification")
@Getter @Setter @NoArgsConstructor
public class DemandeVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    /** Palier sollicite : SIMPLE ou COMPLETE. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NiveauVerification niveauDemande;

    // --- Etat civil declare -------------------------------------------------

    private String nom;
    private String prenom;
    private LocalDate dateNaissance;

    /** Portrait, requis aux deux paliers. */
    @Column(length = 120)
    private String fichierSelfie;

    // --- Piece d'identite, propre au palier complet -------------------------

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TypePiece typePiece;

    /**
     * Empreinte du numero de la piece.
     *
     * <p>Elle permet de detecter qu'une meme piece a servi a deux comptes sans
     * jamais rendre le numero lisible : la comparaison reste possible, la
     * lecture ne l'est pas.</p>
     */
    @Column(length = 64)
    private String empreinteNumero;

    /** Une piece expiree est refusee. */
    private LocalDate dateExpiration;

    @Column(length = 120)
    private String fichierRecto;

    /** Absent pour le passeport, qui n'exige pas de verso. */
    @Column(length = 120)
    private String fichierVerso;

    /** Vraie une fois les images detruites, la decision etant deja prise. */
    private boolean imagesPurgees = false;

    // --- Instruction --------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutVerification statut = StatutVerification.EN_ATTENTE;

    @Column(length = 500)
    private String motifRejet;

    private LocalDateTime dateSoumission = LocalDateTime.now();
    private LocalDateTime dateDecision;

    /** Administrateur ayant tranche : la decision doit rester imputable. */
    @ManyToOne
    @JoinColumn(name = "decideur_id")
    private Utilisateur decideur;

    /** La piece est-elle encore valide a la date du jour ? */
    public boolean pieceExpiree() {
        return dateExpiration != null && dateExpiration.isBefore(LocalDate.now());
    }

    /** Le palier complet exige une piece d'identite. */
    public boolean exigePiece() {
        return niveauDemande == NiveauVerification.COMPLETE;
    }
}
