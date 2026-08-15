package sn.isi.tontyn.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.isi.tontyn.exception.ConflitMetierException;
import sn.isi.tontyn.model.Membre;
import sn.isi.tontyn.model.NiveauVerification;
import sn.isi.tontyn.model.Tontine;
import sn.isi.tontyn.model.Utilisateur;
import sn.isi.tontyn.repository.MembreRepository;

import java.util.List;

/**
 * Plafonds par paliers de verification.
 *
 * <p>Le dispositif transpose a l'echelle d'une tontine le principe retenu par
 * la BCEAO pour la monnaie electronique : les plafonds ne sont releves qu'au
 * benefice des personnes regulierement identifiees, et le sont progressivement.
 * L'utilisateur ne subit ainsi un controle qu'a la mesure de ce qu'il engage,
 * ce qui evite d'imposer la production de papiers d'identite a une tontine de
 * quartier.</p>
 *
 * <p><strong>Limite connue.</strong> Le plafond porte sur le tour d'une
 * tontine, et non sur l'engagement cumule d'un utilisateur : rien n'empeche un
 * membre de repartir son exposition sur plusieurs groupes pour depasser le
 * plafond qui lui est applicable. Le seuil bas du premier palier rend la
 * manoeuvre peu praticable, mais ne l'interdit pas. Le controle de l'exposition
 * agregee supposerait de sommer les tours de toutes les tontines actives d'un
 * utilisateur ; il constitue une piste d'evolution identifiee.</p>
 *
 * <p><strong>La grandeur soumise au plafond est le montant du tour</strong>,
 * soit la cotisation multipliee par le nombre de membres. Dans une tontine
 * rotative, ce montant mesure deux choses a la fois : la somme que percoit le
 * beneficiaire d'un cycle, et l'engagement total de chaque participant sur la
 * duree — puisqu'il y a autant de cycles que de membres. C'est donc la seule
 * grandeur qui exprime reellement le risque encouru.</p>
 *
 * <p><strong>Regle centrale :</strong> le plafond d'une tontine est celui de
 * son membre le moins verifie. Un groupe ne peut engager davantage que ce que
 * son maillon le plus faible autorise.</p>
 */
@Service
public class DeplafonnementService {

    private final MembreRepository membreRepository;

    /** Plafond applicable a un compte dont seul le numero a ete confirme. */
    @Value("${app.verification.plafond-non-verifie:200000}")
    private double plafondNonVerifie;

    /** Plafond applicable apres declaration de l'etat civil et portrait. */
    @Value("${app.verification.plafond-simple:1000000}")
    private double plafondSimple;

    public DeplafonnementService(MembreRepository membreRepository) {
        this.membreRepository = membreRepository;
    }

    // ------------------------------------------------------------------
    //  Bareme
    // ------------------------------------------------------------------

    /** Montant du tour autorise pour un niveau donne. */
    public double plafondPour(NiveauVerification niveau) {
        if (niveau == null) {
            return plafondNonVerifie;
        }
        return switch (niveau) {
            case NON_VERIFIE -> plafondNonVerifie;
            case SIMPLE -> plafondSimple;
            case COMPLETE -> Double.MAX_VALUE;
        };
    }

    /** Niveau minimal permettant d'engager le montant indique. */
    public NiveauVerification niveauRequisPour(double montantDuTour) {
        if (montantDuTour <= plafondNonVerifie) {
            return NiveauVerification.NON_VERIFIE;
        }
        if (montantDuTour <= plafondSimple) {
            return NiveauVerification.SIMPLE;
        }
        return NiveauVerification.COMPLETE;
    }

    /** Somme percue par le beneficiaire d'un cycle, et engagement de chaque membre. */
    public double montantDuTour(double montantCotisation, int nombreMembres) {
        return montantCotisation * Math.max(nombreMembres, 0);
    }

    public double montantDuTour(Tontine t) {
        return montantDuTour(t.getMontantCotisation(), t.getNombreMembres());
    }

    public double getPlafondNonVerifie() {
        return plafondNonVerifie;
    }

    public double getPlafondSimple() {
        return plafondSimple;
    }

    // ------------------------------------------------------------------
    //  Controles
    // ------------------------------------------------------------------

    /**
     * Creation d'une tontine : l'auteur en devient le premier membre, son
     * niveau doit donc suffire au montant engage.
     */
    public void exigerCreationAutorisee(double montantCotisation, int nombreMembres,
                                        Utilisateur auteur) {
        double tour = montantDuTour(montantCotisation, nombreMembres);
        NiveauVerification requis = niveauRequisPour(tour);
        NiveauVerification acquis = niveau(auteur);

        if (!acquis.auMoins(requis)) {
            throw new ConflitMetierException(refus(
                    "Le tour de cette tontine atteint " + formater(tour) + ", ce qui exige une "
                            + requis.getLibelle().toLowerCase() + ". Votre compte est actuellement en "
                            + acquis.getLibelle().toLowerCase() + "."));
        }
    }

    /**
     * Modification d'une tontine : si la nouvelle configuration eleve le niveau
     * requis, tous les membres deja inscrits doivent l'atteindre.
     */
    @Transactional(readOnly = true)
    public void exigerAugmentationAutorisee(Tontine tontine, double nouveauMontant,
                                            int nouveauNombre) {
        double tour = montantDuTour(nouveauMontant, nouveauNombre);
        NiveauVerification requis = niveauRequisPour(tour);
        if (requis == NiveauVerification.NON_VERIFIE) {
            return;
        }

        List<String> insuffisants = membreRepository.findByTontineId(tontine.getId()).stream()
                .map(Membre::getUtilisateur)
                .filter(u -> !niveau(u).auMoins(requis))
                .map(u -> (u.getPrenom() + " " + u.getNom()).trim())
                .toList();

        if (!insuffisants.isEmpty()) {
            throw new ConflitMetierException(refus(
                    "Le tour atteindrait " + formater(tour) + ", ce qui exige une "
                            + requis.getLibelle().toLowerCase() + " pour chaque membre. "
                            + insuffisants.size() + " membre(s) ne l'ont pas atteinte : "
                            + String.join(", ", insuffisants) + "."));
        }
    }

    /** Ajout d'un membre : son niveau doit suffire au montant deja engage. */
    public void exigerAjoutAutorise(Tontine tontine, Utilisateur candidat) {
        double tour = montantDuTour(tontine);
        NiveauVerification requis = niveauRequisPour(tour);
        NiveauVerification acquis = niveau(candidat);

        if (!acquis.auMoins(requis)) {
            throw new ConflitMetierException(refus(
                    "Cette tontine engage " + formater(tour) + " par tour et n'accepte que les "
                            + "comptes ayant atteint la " + requis.getLibelle().toLowerCase()
                            + ". Ce compte est en " + acquis.getLibelle().toLowerCase() + "."));
        }
    }

    /**
     * Plafond effectif d'une tontine : celui de son membre le moins verifie.
     * Utile pour renseigner l'interface sans attendre un refus.
     */
    @Transactional(readOnly = true)
    public double plafondEffectif(Long tontineId) {
        return membreRepository.findByTontineId(tontineId).stream()
                .map(m -> plafondPour(niveau(m.getUtilisateur())))
                .min(Double::compare)
                .orElse(Double.MAX_VALUE);
    }

    // ------------------------------------------------------------------
    //  Utilitaires
    // ------------------------------------------------------------------

    private NiveauVerification niveau(Utilisateur u) {
        if (u == null || u.getNiveauVerification() == null) {
            return NiveauVerification.NON_VERIFIE;
        }
        return u.getNiveauVerification();
    }

    /** Prefixe commun a tous les refus, pour que le message soit reconnaissable. */
    private String refus(String detail) {
        return "Non autorisé. " + detail;
    }

    private String formater(double montant) {
        return String.format("%,.0f FCFA", montant).replace(',', ' ');
    }
}
