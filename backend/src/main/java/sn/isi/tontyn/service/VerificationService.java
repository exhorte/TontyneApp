package sn.isi.tontyn.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import sn.isi.tontyn.exception.ConflitMetierException;
import sn.isi.tontyn.exception.RessourceIntrouvableException;
import sn.isi.tontyn.model.*;
import sn.isi.tontyn.repository.DemandeVerificationRepository;
import sn.isi.tontyn.repository.UtilisateurRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Instruction des demandes de verification d'identite.
 *
 * <p>Deux paliers, deux traitements. La <strong>verification simple</strong>
 * repose sur l'etat civil declare et un portrait : elle est accordee
 * immediatement, a la maniere des premiers paliers pratiques par les emetteurs
 * de monnaie electronique, et demeure donc declarative — le controle
 * n'intervient qu'a posteriori, en cas de signalement. La <strong>verification
 * complete</strong> suppose la production d'une piece d'identite et passe
 * necessairement devant l'administrateur de la plateforme.</p>
 *
 * <p>Aucun service automatique de reconnaissance documentaire n'etant
 * accessible sans contrat commercial, l'instruction du palier complet est
 * humaine. La methode {@link #instruireAutomatiquement} materialise le point de
 * raccordement d'un fournisseur tel que Smile ID ou uqudo, le jour ou une cle
 * d'API serait disponible.</p>
 */
@Service
@Transactional
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);

    private final DemandeVerificationRepository demandeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DepotFichierService depot;
    private final NotificationService notificationService;

    /**
     * Sel applicatif ajoute avant hachage du numero de piece.
     *
     * <p>Un numero de piece a une entropie faible et un format previsible :
     * une empreinte nue se retrouverait par recherche exhaustive. Ce sel, garde
     * hors du depot, rend l'operation impraticable.</p>
     */
    @Value("${app.verification.sel:${JWT_SECRET:sel-de-developpement-a-remplacer}}")
    private String sel;

    @Value("${app.verification.instruction-automatique:false}")
    private boolean instructionAutomatique;

    public VerificationService(DemandeVerificationRepository demandeRepository,
                               UtilisateurRepository utilisateurRepository,
                               DepotFichierService depot,
                               NotificationService notificationService) {
        this.demandeRepository = demandeRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.depot = depot;
        this.notificationService = notificationService;
    }

    // ------------------------------------------------------------------
    //  Palier simple : etat civil declare et portrait, accorde d'emblee
    // ------------------------------------------------------------------

    public DemandeVerification soumettreSimple(Utilisateur utilisateur, String nom, String prenom,
                                               LocalDate dateNaissance, MultipartFile selfie) {
        interdireDemandeEnCours(utilisateur);
        exigerNiveauSuperieur(utilisateur, NiveauVerification.SIMPLE);

        DemandeVerification d = new DemandeVerification();
        d.setUtilisateur(utilisateur);
        d.setNiveauDemande(NiveauVerification.SIMPLE);
        d.setNom(nom);
        d.setPrenom(prenom);
        d.setDateNaissance(dateNaissance);
        d.setFichierSelfie(depot.deposer(selfie, "portrait"));

        // Accord immediat : le palier est declaratif.
        d.setStatut(StatutVerification.VALIDEE);
        d.setDateDecision(LocalDateTime.now());
        demandeRepository.save(d);

        promouvoir(utilisateur, NiveauVerification.SIMPLE);
        log.info("Verification simple accordee a l'utilisateur {}", utilisateur.getId());
        return d;
    }

    // ------------------------------------------------------------------
    //  Palier complet : piece d'identite, soumise a instruction
    // ------------------------------------------------------------------

    public DemandeVerification soumettreComplete(Utilisateur utilisateur, TypePiece type,
                                                 String numeroPiece, LocalDate dateExpiration,
                                                 String nom, String prenom, LocalDate dateNaissance,
                                                 MultipartFile recto, MultipartFile verso,
                                                 MultipartFile selfie) {
        interdireDemandeEnCours(utilisateur);
        exigerNiveauSuperieur(utilisateur, NiveauVerification.COMPLETE);

        if (type == null) {
            throw new ConflitMetierException("Le type de pièce est obligatoire.");
        }
        if (dateExpiration != null && dateExpiration.isBefore(LocalDate.now())) {
            throw new ConflitMetierException(
                    "Cette pièce a expiré le " + dateExpiration + ". Fournissez un document en cours de validité.");
        }
        if (type.exigeVerso() && (verso == null || verso.isEmpty())) {
            throw new ConflitMetierException(
                    "Le verso est obligatoire pour une " + type.getLibelle().toLowerCase() + ".");
        }

        String empreinte = empreinte(numeroPiece);
        refuserPieceDejaUtilisee(empreinte, utilisateur);

        DemandeVerification d = new DemandeVerification();
        d.setUtilisateur(utilisateur);
        d.setNiveauDemande(NiveauVerification.COMPLETE);
        d.setTypePiece(type);
        d.setEmpreinteNumero(empreinte);
        d.setDateExpiration(dateExpiration);
        d.setNom(nom);
        d.setPrenom(prenom);
        d.setDateNaissance(dateNaissance);
        d.setFichierRecto(depot.deposer(recto, "recto"));
        if (type.exigeVerso()) {
            d.setFichierVerso(depot.deposer(verso, "verso"));
        }
        d.setFichierSelfie(depot.deposer(selfie, "portrait"));
        d.setStatut(StatutVerification.EN_ATTENTE);
        demandeRepository.save(d);

        if (instructionAutomatique) {
            instruireAutomatiquement(d);
        }
        log.info("Demande de verification complete deposee par l'utilisateur {}", utilisateur.getId());
        return d;
    }

    // ------------------------------------------------------------------
    //  Instruction
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<DemandeVerification> fileAttente() {
        return demandeRepository.findByStatutOrderByDateSoumissionAsc(StatutVerification.EN_ATTENTE);
    }

    @Transactional(readOnly = true)
    public DemandeVerification derniereDemande(Long utilisateurId) {
        return demandeRepository.findFirstByUtilisateurIdOrderByDateSoumissionDesc(utilisateurId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public DemandeVerification charger(Long id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Demande de vérification", id));
    }

    public DemandeVerification valider(Long demandeId, Utilisateur decideur) {
        DemandeVerification d = charger(demandeId);
        exigerEnAttente(d);

        d.setStatut(StatutVerification.VALIDEE);
        d.setDateDecision(LocalDateTime.now());
        d.setDecideur(decideur);
        demandeRepository.save(d);

        promouvoir(d.getUtilisateur(), d.getNiveauDemande());
        notifier(d.getUtilisateur(), "VERIFICATION_VALIDEE",
                "Votre identité a été vérifiée. Votre compte est désormais déplafonné.");
        log.info("Demande {} validee par {}", demandeId, decideur != null ? decideur.getId() : "?");
        return d;
    }

    public DemandeVerification rejeter(Long demandeId, Utilisateur decideur, String motif) {
        if (motif == null || motif.isBlank()) {
            throw new ConflitMetierException(
                    "Un motif est obligatoire : l'utilisateur doit savoir quoi corriger.");
        }
        DemandeVerification d = charger(demandeId);
        exigerEnAttente(d);

        d.setStatut(StatutVerification.REJETEE);
        d.setMotifRejet(motif.trim());
        d.setDateDecision(LocalDateTime.now());
        d.setDecideur(decideur);
        demandeRepository.save(d);

        // Le niveau anterieur demeure : un refus ne degrade pas un acquis.
        notifier(d.getUtilisateur(), "VERIFICATION_REJETEE",
                "Votre demande de vérification a été refusée : " + d.getMotifRejet());
        log.info("Demande {} rejetee", demandeId);
        return d;
    }

    /**
     * Point de raccordement d'un fournisseur de verification automatique.
     *
     * <p>Une implementation de production transmettrait ici les images au
     * service retenu, puis appellerait {@link #valider} ou {@link #rejeter}
     * selon le verdict et le degre de confiance renvoye.</p>
     */
    private void instruireAutomatiquement(DemandeVerification demande) {
        log.info("Instruction automatique demandee pour {} : aucun fournisseur raccorde, "
                + "la demande reste en attente d'examen humain.", demande.getId());
    }

    // ------------------------------------------------------------------
    //  Purge des images
    // ------------------------------------------------------------------

    /**
     * Detruit les images des demandes tranchees depuis plus longtemps que la
     * duree de conservation. Les donnees extraites subsistent : elles fondent
     * le statut acquis, quand l'image n'a plus d'utilite une fois la decision
     * prise.
     */
    public int purger(int retentionJours) {
        LocalDateTime limite = LocalDateTime.now().minusDays(retentionJours);
        List<DemandeVerification> a = demandeRepository
                .findByImagesPurgeesFalseAndDateDecisionBefore(limite);

        for (DemandeVerification d : a) {
            depot.supprimer(d.getFichierRecto());
            depot.supprimer(d.getFichierVerso());
            depot.supprimer(d.getFichierSelfie());
            d.setFichierRecto(null);
            d.setFichierVerso(null);
            d.setFichierSelfie(null);
            d.setImagesPurgees(true);
        }
        if (!a.isEmpty()) {
            demandeRepository.saveAll(a);
            log.info("Purge des images : {} demande(s) traitee(s)", a.size());
        }
        return a.size();
    }

    // ------------------------------------------------------------------
    //  Utilitaires
    // ------------------------------------------------------------------

    private void promouvoir(Utilisateur u, NiveauVerification niveau) {
        if (u.niveauEffectif().auMoins(niveau)) {
            return;
        }
        u.setNiveauVerification(niveau);
        u.setDateVerification(LocalDateTime.now());
        utilisateurRepository.save(u);
    }

    private void interdireDemandeEnCours(Utilisateur u) {
        if (demandeRepository.existsByUtilisateurIdAndStatut(u.getId(), StatutVerification.EN_ATTENTE)) {
            throw new ConflitMetierException(
                    "Une demande est déjà en cours d'examen. Attendez la décision avant d'en déposer une autre.");
        }
    }

    private void exigerNiveauSuperieur(Utilisateur u, NiveauVerification vise) {
        if (u.niveauEffectif().auMoins(vise)) {
            throw new ConflitMetierException(
                    "Votre compte a déjà atteint la " + vise.getLibelle().toLowerCase() + ".");
        }
    }

    private void exigerEnAttente(DemandeVerification d) {
        if (d.getStatut() != StatutVerification.EN_ATTENTE) {
            throw new ConflitMetierException("Cette demande a déjà été tranchée.");
        }
    }

    private void refuserPieceDejaUtilisee(String empreinte, Utilisateur candidat) {
        if (empreinte == null) {
            return;
        }
        boolean deja = demandeRepository
                .findByEmpreinteNumeroAndStatut(empreinte, StatutVerification.VALIDEE).stream()
                .anyMatch(d -> !d.getUtilisateur().getId().equals(candidat.getId()));
        if (deja) {
            throw new ConflitMetierException(
                    "Cette pièce d'identité est déjà associée à un autre compte.");
        }
    }

    /**
     * Empreinte du numero de piece : elle autorise la comparaison sans jamais
     * rendre le numero lisible.
     */
    private String empreinte(String numero) {
        if (numero == null || numero.isBlank()) {
            return null;
        }
        String normalise = numero.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] condense = sha.digest((sel + "|" + normalise).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(condense);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    private void notifier(Utilisateur destinataire, String type, String message) {
        try {
            notificationService.envoyer(destinataire, type, message, "SMS");
        } catch (Exception e) {
            log.warn("Notification de verification non transmise : {}", e.getMessage());
        }
    }
}
