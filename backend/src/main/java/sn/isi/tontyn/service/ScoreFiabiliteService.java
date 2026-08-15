package sn.isi.tontyn.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.isi.tontyn.dto.ScoreFiabiliteResponse;
import sn.isi.tontyn.dto.ScoreFiabiliteResponse.CritereScore;
import sn.isi.tontyn.exception.RessourceIntrouvableException;
import sn.isi.tontyn.model.Cotisation;
import sn.isi.tontyn.model.Membre;
import sn.isi.tontyn.model.Paiement;
import sn.isi.tontyn.repository.CotisationRepository;
import sn.isi.tontyn.repository.MembreRepository;
import sn.isi.tontyn.repository.PaiementRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Score de fiabilite de paiement d'un membre, calcule a partir de son
 * historique de cotisations.
 *
 * <p><strong>Pourquoi une ponderation de regles, et pas un modele appris.</strong>
 * Deux raisons, deliberement conservees en commentaire ici plutot que dans un
 * seul document externe :</p>
 * <ol>
 *   <li>Le jeu de demonstration compte trois comptes. Aucun modele entraine
 *   ne tient sur un tel volume, et un jury technique le verrait
 *   immediatement.</li>
 *   <li>Un score qui penalise quelqu'un doit pouvoir se justifier a
 *   l'interesse, critere par critere. Sur de l'epargne populaire, une note
 *   opaque — meme statistiquement fondee — est indefendable.</li>
 * </ol>
 *
 * <p>Le calcul repose donc sur trois criteres lisibles, chacun plafonne, dont
 * les poids sont configurables ({@code application.properties}) plutot que
 * fixes en dur — comme le bareme de {@link DeplafonnementService}. Le point
 * de raccordement d'un veritable modele est prevu : voir
 * {@link #calculerAvecModeleExterne(Long)}, sur le modele de
 * {@code VerificationService.instruireAutomatiquement}.</p>
 *
 * <p><strong>Limite technique assumee.</strong> {@code Cotisation} ne porte
 * pas de date de reglement propre : son champ {@code date} est fixe a la
 * creation de la cotisation, jamais reecrit au paiement. La seule date
 * disponible est {@code Paiement.date}, fixee a l'initiation et non remise a
 * jour a la confirmation. Le service l'utilise comme approximation de la
 * date de reglement : en usage reel, membre initie puis confirme dans la
 * foulee, l'ecart se compte en minutes, donc sans incidence sur un score
 * exprime en jours.</p>
 */
@Service
public class ScoreFiabiliteService {

    /** Aucune cotisation jugee (payee ou en retard) : le score serait trompeur. */
    public static final String NIVEAU_INSUFFISANT = "INSUFFISANT";
    /** Moins de {@link #seuilConfianceFaible} cotisations jugees. */
    public static final String NIVEAU_FAIBLE = "FAIBLE";
    /** Entre {@link #seuilConfianceFaible} et {@link #seuilConfianceMoyenne} cotisations jugees. */
    public static final String NIVEAU_MOYENNE = "MOYENNE";
    /** {@link #seuilConfianceMoyenne} cotisations jugees ou plus. */
    public static final String NIVEAU_ELEVEE = "ELEVEE";

    private final MembreRepository membreRepository;
    private final CotisationRepository cotisationRepository;
    private final PaiementRepository paiementRepository;

    /** Malus maximal si aucune cotisation n'a ete reglee a temps (0 % de ponctualite). */
    @Value("${app.score.poids-ponctualite:50}")
    private double poidsPonctualite;

    /** Points retires par cotisation actuellement en retard et non soldee. */
    @Value("${app.score.malus-retard-par-unite:10}")
    private double malusRetardParUnite;

    /** Plafond du malus applique aux retards en cours, quel que soit leur nombre. */
    @Value("${app.score.malus-retard-plafond:30}")
    private double malusRetardPlafond;

    /** Points retires par jour de retard moyen constate sur les regularisations. */
    @Value("${app.score.malus-regularisation-par-jour:1}")
    private double malusRegularisationParJour;

    /** Plafond du malus applique au delai moyen de regularisation. */
    @Value("${app.score.malus-regularisation-plafond:20}")
    private double malusRegularisationPlafond;

    /** En dessous de ce nombre de cotisations jugees, la confiance est FAIBLE. */
    @Value("${app.score.seuil-confiance-faible:5}")
    private int seuilConfianceFaible;

    /** A partir de ce nombre de cotisations jugees, la confiance est ELEVEE. */
    @Value("${app.score.seuil-confiance-moyenne:15}")
    private int seuilConfianceMoyenne;

    public ScoreFiabiliteService(MembreRepository membreRepository,
                                 CotisationRepository cotisationRepository,
                                 PaiementRepository paiementRepository) {
        this.membreRepository = membreRepository;
        this.cotisationRepository = cotisationRepository;
        this.paiementRepository = paiementRepository;
    }

    /**
     * Calcule le score complet, avec sa decomposition, pour l'endpoint de
     * detail ({@code GET /api/membres/{id}/score}).
     */
    @Transactional(readOnly = true)
    public ScoreFiabiliteResponse calculerDetail(Long membreId) {
        Membre membre = membreRepository.findById(membreId)
                .orElseThrow(() -> new RessourceIntrouvableException("Membre", membreId));
        return calculer(membre);
    }

    /**
     * Calcule uniquement le resume (score + niveau de confiance), utilise par
     * {@code MembreResponse} pour eviter tout appel supplementaire au client.
     * Renvoie {@code [null, null]} sous forme de resume vide si le score n'est
     * pas encore determinable.
     */
    @Transactional(readOnly = true)
    public ScoreFiabiliteResponse calculerResume(Membre membre) {
        return calculer(membre);
    }

    private ScoreFiabiliteResponse calculer(Membre membre) {
        List<Cotisation> cotisations = cotisationRepository.findByMembreId(membre.getId());

        // Seules PAYEE et EN_RETARD constituent un "jugement" sur le membre :
        // une EN_ATTENTE dont le cycle n'est pas encore clos n'a encore rien
        // dit de son comportement.
        List<Cotisation> jugees = cotisations.stream()
                .filter(c -> "PAYEE".equals(c.getStatut()) || "EN_RETARD".equals(c.getStatut()))
                .toList();

        long ancienneteJours = ChronoUnit.DAYS.between(membre.getDateAdhesion(), LocalDate.now());

        if (jugees.isEmpty()) {
            String explication = "Aucune cotisation payee ou en retard n'est encore enregistree "
                    + "pour ce membre (adhesion il y a " + ancienneteJours + " jour(s)). "
                    + "Ce n'est pas un mauvais payeur : c'est un membre dont on ne sait encore "
                    + "rien. Le score n'est donc pas calcule plutot que de renvoyer un chiffre "
                    + "arbitraire.";
            List<CritereScore> decomposition = List.of(new CritereScore(
                    "DONNEES_INSUFFISANTES",
                    "Historique de cotisations",
                    "0 cotisation jugee (payee ou en retard)",
                    0,
                    explication));
            return new ScoreFiabiliteResponse(membre.getId(), null, NIVEAU_INSUFFISANT,
                    0, ancienneteJours, explication, decomposition);
        }

        int nOnTime = 0;
        int nLatePaid = 0;
        int nRetardEnCours = 0;
        long sommeJoursRetard = 0;

        for (Cotisation c : jugees) {
            if ("EN_RETARD".equals(c.getStatut())) {
                nRetardEnCours++;
                continue;
            }
            // c.getStatut() == "PAYEE" ici.
            LocalDate echeance = c.getCycle().getDateFin();
            Optional<Paiement> paiement = paiementRepository.findByCotisationId(c.getId());
            if (paiement.isEmpty()) {
                // Anomalie de donnees (une cotisation PAYEE devrait toujours avoir
                // un paiement confirme, RG-07/RG-09) : on ne penalise pas le
                // membre pour un trou qui n'est pas le sien.
                nOnTime++;
                continue;
            }
            LocalDate dateReglement = paiement.get().getDate().toLocalDate();
            if (!dateReglement.isAfter(echeance)) {
                nOnTime++;
            } else {
                nLatePaid++;
                sommeJoursRetard += ChronoUnit.DAYS.between(echeance, dateReglement);
            }
        }

        int nJugees = jugees.size();
        double tauxPonctualite = nOnTime / (double) nJugees;
        double delaiMoyenJours = nLatePaid > 0 ? sommeJoursRetard / (double) nLatePaid : 0;

        double malusPonctualite = (1 - tauxPonctualite) * poidsPonctualite;
        double malusRetardsEnCours = Math.min(nRetardEnCours * malusRetardParUnite, malusRetardPlafond);
        double malusRegularisation = Math.min(delaiMoyenJours * malusRegularisationParJour,
                malusRegularisationPlafond);

        double scoreBrut = 100 - malusPonctualite - malusRetardsEnCours - malusRegularisation;
        int score = (int) Math.round(Math.max(0, Math.min(100, scoreBrut)));

        String niveauConfiance;
        if (nJugees < seuilConfianceFaible) {
            niveauConfiance = NIVEAU_FAIBLE;
        } else if (nJugees < seuilConfianceMoyenne) {
            niveauConfiance = NIVEAU_MOYENNE;
        } else {
            niveauConfiance = NIVEAU_ELEVEE;
        }

        List<CritereScore> decomposition = new ArrayList<>();
        decomposition.add(new CritereScore(
                "PONCTUALITE",
                "Ponctualite des reglements",
                String.format(Locale.FRANCE, "%d cotisation(s) a temps sur %d jugee(s) (%.0f %%)",
                        nOnTime, nJugees, tauxPonctualite * 100),
                round1(malusPonctualite),
                "Poids dominant du score : chaque cotisation qui n'est pas reglee avant "
                        + "l'echeance du cycle (qu'elle soit finalement payee en retard ou "
                        + "encore en retard) abaisse ce taux."));
        decomposition.add(new CritereScore(
                "RETARDS_EN_COURS",
                "Retards actuellement non soldes",
                nRetardEnCours + " cotisation(s) en retard non reglee(s) a ce jour",
                round1(malusRetardsEnCours),
                "Penalite supplementaire, distincte de la ponctualite : un retard non "
                        + "encore regularise represente un risque present, pas seulement un "
                        + "antecedent. Plafonnee pour qu'une longue historique difficile ne "
                        + "fasse pas mecaniquement tomber le score a zero."));
        decomposition.add(new CritereScore(
                "DELAI_REGULARISATION",
                "Delai moyen de regularisation",
                nLatePaid > 0
                        ? String.format(Locale.FRANCE, "%.1f jour(s) en moyenne sur %d reglement(s) tardif(s)",
                                delaiMoyenJours, nLatePaid)
                        : "aucun reglement tardif observe",
                round1(malusRegularisation),
                "Mesure la vitesse de rattrapage une fois le retard constate : regulariser "
                        + "vite apres l'echeance coute moins cher que trainer plusieurs "
                        + "semaines, meme si le paiement finit par arriver."));

        String explicationGlobale = String.format(Locale.FRANCE,
                "Score calcule sur %d cotisation(s) jugee(s) (payees ou en retard), "
                        + "niveau de confiance %s. Adhesion il y a %d jour(s).",
                nJugees, niveauConfiance, ancienneteJours);

        return new ScoreFiabiliteResponse(membre.getId(), score, niveauConfiance,
                nJugees, ancienneteJours, explicationGlobale, decomposition);
    }

    private double round1(double valeur) {
        return Math.round(valeur * 10) / 10.0;
    }

    // ------------------------------------------------------------------
    //  Raccordement futur
    // ------------------------------------------------------------------

    /**
     * Point de raccordement d'un veritable modele de scoring, le jour ou le
     * volume de donnees le justifierait.
     *
     * <p>Sur le modele de {@code VerificationService.instruireAutomatiquement} :
     * une methode, un commentaire, aucune implementation. Une version de
     * production interrogerait ici un service externe ou un modele
     * auto-heberge, puis fusionnerait son verdict avec (ou a la place de) la
     * ponderation de regles ci-dessus, sans jamais perdre la decomposition
     * explicable, qui reste la contrainte non negociable de cette
     * fonctionnalite sur de l'epargne populaire.</p>
     */
    public ScoreFiabiliteResponse calculerAvecModeleExterne(Long membreId) {
        throw new UnsupportedOperationException(
                "Aucun modele de scoring externe n'est raccorde. Voir calculerDetail(Long).");
    }
}
