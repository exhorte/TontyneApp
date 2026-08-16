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

/**
 * Construit le texte de contexte transmis a DeepSeek : la situation d'un
 * membre sur Tontyn, et uniquement la sienne.
 *
 * <p><strong>Cloisonnement des donnees (regle absolue, voir aussi
 * {@code AssistantService}).</strong> Cette classe ne prend en parametre
 * qu'un {@link Utilisateur} deja resolu par
 * {@code SecuriteTontine.utilisateurCourant()} — jamais un identifiant fourni
 * par le client. Elle ne s'appuie que sur les services metier existants
 * ({@link MembreService}, {@link CotisationService}, {@link CycleService}),
 * chacun filtre par cet utilisateur : aucune requete n'est reecrite ici, et
 * rien n'est jamais recupere pour un autre compte. Le texte produit ne
 * contient ni telephone, ni e-mail, ni empreinte de piece d'identite — le
 * prenom suffit, et il est deja visible des autres membres de chaque tontine
 * concernee.</p>
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

        CycleResponse tour = cycleService.listerParTontine(m.tontineId()).stream()
                .filter(c -> m.id().equals(c.beneficiaireId()))
                .findFirst().orElse(null);
        if (tour != null) {
            texte.append("Tour de beneficiaire : cycle n").append(tour.numero())
                    .append(", du ").append(tour.dateDebut().format(FORMAT_DATE))
                    .append(" au ").append(tour.dateFin().format(FORMAT_DATE)).append(".\n");
        } else {
            texte.append("Les cycles de cette tontine n'ont pas encore ete programmes.\n");
        }
        texte.append("--- fin tontine ---\n\n");
    }

    /** Meme motif que DeplafonnementService.formater : espace comme separateur de milliers. */
    private String formaterMontant(double montant) {
        return String.format("%,.0f", montant).replace(',', ' ');
    }
}
