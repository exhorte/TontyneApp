package sn.isi.tontyn.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.isi.tontyn.dto.CotisationResponse;
import sn.isi.tontyn.dto.CycleResponse;
import sn.isi.tontyn.dto.MembreResponse;
import sn.isi.tontyn.model.Utilisateur;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Construit le texte de contexte transmis a DeepSeek : la situation d'un
 * membre sur Tontyn, sans restriction pour lui-meme, et un resume tres
 * limite des autres membres de ses tontines.
 *
 * <p><strong>Cloisonnement des donnees propres a l'utilisateur (regle
 * absolue, voir aussi {@code AssistantService}).</strong> Cette classe ne
 * prend en parametre qu'un {@link Utilisateur} deja resolu par
 * {@code SecuriteTontine.utilisateurCourant()} — jamais un identifiant fourni
 * par le client. Elle ne s'appuie que sur les services metier existants
 * ({@link MembreService}, {@link CotisationService}, {@link CycleService}),
 * chacun filtre par cet utilisateur : aucune requete n'est reecrite ici, et
 * rien n'est jamais recupere pour un autre compte que celui passe en
 * parametre ou les co-membres de ses propres tontines.</p>
 *
 * <p><strong>Donnees d'un tiers (co-membre d'une meme tontine) : liste
 * blanche stricte, point de passage unique.</strong> Un membre voit deja les
 * autres membres de ses tontines dans l'application ; l'assistant peut donc
 * en mobiliser un resume, mais exclusivement au travers de
 * {@link #versResumeTiers}, qui ne lit que quatre champs : nom, statut
 * d'adhesion, ordre de passage, et un indicateur generique de retard sur le
 * tour en cours. Aucune autre information n'atteint jamais le texte de
 * contexte pour un tiers — ni telephone, ni code PIN, ni piece d'identite ou
 * son contenu, ni niveau de verification detaille, ni historique de paiement
 * complet — meme si {@code MembreResponse} porte certains de ces champs par
 * ailleurs pour d'autres usages (l'annuaire de la tontine, par exemple) :
 * {@link ResumeMembreTiers} n'en reprend que ces quatre-la, et c'est ce
 * record, jamais {@code MembreResponse} directement, qui alimente le texte
 * ci-dessous pour un tiers. Pour toute evolution de ce qui peut etre partage
 * entre co-membres, modifier {@link #versResumeTiers} et rien d'autre : c'est
 * le composant unique a citer pour expliquer comment cette regle est
 * appliquee.</p>
 */
@Component
public class ContexteAssistant {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MembreService membreService;
    private final CotisationService cotisationService;
    private final CycleService cycleService;

    public ContexteAssistant(MembreService membreService, CotisationService cotisationService,
                             CycleService cycleService) {
        this.membreService = membreService;
        this.cotisationService = cotisationService;
        this.cycleService = cycleService;
    }

    /**
     * Liste blanche des donnees d'un tiers admissibles dans le contexte de
     * l'assistant. Voir la javadoc de la classe : ce record est le seul
     * chemin par lequel une information sur un co-membre peut atteindre le
     * modele de langage. N'y ajouter un champ qu'apres s'etre assure qu'il ne
     * figure pas parmi les donnees proscrites (telephone, PIN, piece
     * d'identite, niveau de verification detaille, historique de paiement).
     */
    private record ResumeMembreTiers(String nom, String statutAdhesion, int ordreTour,
                                     boolean enRetardTourEnCours) {}

    @Transactional(readOnly = true)
    public String construirePour(Utilisateur utilisateur) {
        List<MembreResponse> adhesions = membreService.listerParUtilisateur(utilisateur.getId());

        StringBuilder texte = new StringBuilder();
        texte.append("Vous repondez a ").append(utilisateur.getPrenom())
                .append(", membre sur Tontyn. Date du jour : ")
                .append(LocalDate.now().format(FORMAT_DATE)).append(".\n\n");

        if (adhesions.isEmpty()) {
            texte.append("Ce membre n'appartient a aucune tontine pour le moment.");
            return texte.toString();
        }

        for (MembreResponse m : adhesions) {
            ajouterTontine(texte, m);
        }

        return texte.toString();
    }

    private void ajouterTontine(StringBuilder texte, MembreResponse m) {
        texte.append("--- Tontine \"").append(m.tontineNom()).append("\" ---\n");
        texte.append("Statut dans le groupe : ").append(m.roleGroupe())
                .append(". Compte ").append(m.statut()).append(".\n");
        texte.append("Ordre de tour attribue : ").append(m.ordreTour()).append(".\n");

        texte.append("Score de fiabilite de paiement : ");
        if (m.score() != null) {
            texte.append(m.score()).append("/100 (confiance ").append(m.niveauConfiance())
                    .append(").\n");
        } else {
            texte.append("pas encore assez d'historique pour etre calcule.\n");
        }

        List<CotisationResponse> cotisations = cotisationService.listerParMembre(m.id());
        double totalDu = 0;
        if (cotisations.isEmpty()) {
            texte.append("Aucune cotisation enregistree pour l'instant.\n");
        } else {
            texte.append("Cotisations :\n");
            for (CotisationResponse c : cotisations) {
                texte.append("- Cycle n").append(c.cycleNumero())
                        .append(", echeance ").append(c.echeance().format(FORMAT_DATE))
                        .append(" : ").append(formaterMontant(c.montant())).append(" FCFA")
                        .append(", statut ").append(c.statut());
                if (c.penalite() > 0) {
                    texte.append(", penalite ").append(formaterMontant(c.penalite()))
                            .append(" FCFA, total du ").append(formaterMontant(c.montantDu()))
                            .append(" FCFA");
                }
                texte.append(".\n");
                if (!"PAYEE".equals(c.statut())) {
                    totalDu += c.montantDu();
                }
            }
        }
        texte.append("Total actuellement du sur cette tontine : ")
                .append(formaterMontant(totalDu)).append(" FCFA.\n");

        List<CycleResponse> cycles = cycleService.listerParTontine(m.tontineId());
        CycleResponse tour = cycles.stream()
                .filter(c -> m.id().equals(c.beneficiaireId()))
                .findFirst().orElse(null);
        if (tour != null) {
            texte.append("Tour de beneficiaire : cycle n").append(tour.numero())
                    .append(", du ").append(tour.dateDebut().format(FORMAT_DATE))
                    .append(" au ").append(tour.dateFin().format(FORMAT_DATE)).append(".\n");
        } else {
            texte.append("Les cycles de cette tontine n'ont pas encore ete programmes.\n");
        }

        ajouterAutresMembres(texte, m, cycles);

        texte.append("--- fin tontine ---\n\n");
    }

    /**
     * Resume des autres membres de cette meme tontine, filtre a la liste
     * blanche de {@link #versResumeTiers}. Le retard porte uniquement sur le
     * cycle EN_COURS de la tontine, s'il en existe un.
     */
    private void ajouterAutresMembres(StringBuilder texte, MembreResponse moi,
                                      List<CycleResponse> cycles) {
        List<MembreResponse> autres = membreService.listerParTontine(moi.tontineId()).stream()
                .filter(autre -> !autre.id().equals(moi.id()))
                .toList();
        if (autres.isEmpty()) {
            return;
        }

        Set<Long> membresEnRetard = membresEnRetardTourEnCours(cycles);

        texte.append("Autres membres de cette tontine (visibles par tout membre du groupe) :\n");
        for (MembreResponse autre : autres) {
            ResumeMembreTiers resume = versResumeTiers(autre, membresEnRetard.contains(autre.id()));
            texte.append("- ").append(resume.nom())
                    .append(" : statut ").append(resume.statutAdhesion())
                    .append(", ordre de passage ").append(resume.ordreTour())
                    .append(", ").append(resume.enRetardTourEnCours()
                            ? "en retard sur le tour en cours" : "a jour sur le tour en cours")
                    .append(".\n");
        }
    }

    /**
     * Point de passage unique pour les donnees d'un tiers : voir la javadoc
     * de la classe et de {@link ResumeMembreTiers}.
     */
    private ResumeMembreTiers versResumeTiers(MembreResponse autre, boolean enRetardTourEnCours) {
        return new ResumeMembreTiers(autre.nomComplet(), autre.statut(), autre.ordreTour(),
                enRetardTourEnCours);
    }

    /** Identifiants des membres dont la cotisation du cycle EN_COURS est EN_RETARD. */
    private Set<Long> membresEnRetardTourEnCours(List<CycleResponse> cycles) {
        CycleResponse enCours = cycles.stream()
                .filter(c -> "EN_COURS".equals(c.statut()))
                .findFirst().orElse(null);
        if (enCours == null) {
            return Set.of();
        }
        return cotisationService.listerParCycle(enCours.id()).stream()
                .filter(c -> "EN_RETARD".equals(c.statut()))
                .map(CotisationResponse::membreId)
                .collect(Collectors.toSet());
    }

    /** Meme motif que DeplafonnementService.formater : espace comme separateur de milliers. */
    private String formaterMontant(double montant) {
        return String.format("%,.0f", montant).replace(',', ' ');
    }
}
