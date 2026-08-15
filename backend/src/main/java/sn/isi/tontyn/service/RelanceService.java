package sn.isi.tontyn.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.isi.tontyn.model.Cotisation;
import sn.isi.tontyn.model.Membre;
import sn.isi.tontyn.model.RoleGroupe;
import sn.isi.tontyn.model.Tontine;
import sn.isi.tontyn.repository.CotisationRepository;
import sn.isi.tontyn.repository.MembreRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Detection, sanction et relance des cotisations en retard.
 *
 * <p>Une cotisation est en retard lorsque le cycle auquel elle appartient a
 * depasse sa date de fin sans que le membre ait paye. L'echeance n'est donc
 * pas un champ propre a {@link Cotisation} : elle se deduit de
 * {@code Cycle.dateFin}, deja porteuse de cette information et fixee au
 * moment de la generation des cycles (voir {@code CycleService}).</p>
 *
 * <p><strong>Sanction retenue.</strong> Le passage en retard declenche une
 * penalite financiere proportionnelle, au taux fixe sur la tontine
 * ({@code Tontine.tauxPenalite}, nul par defaut). Ce calcul est automatique
 * parce que la regle a ete arretee par les membres eux-memes a la
 * constitution du groupe : l'appliquer sans intervention garantit son
 * uniformite, quand une application discretionnaire ouvrirait la porte au
 * favoritisme — reproche classique adresse aux tontines traditionnelles. Le
 * gestionnaire garde neanmoins la faculte de la lever au cas par cas
 * ({@code CotisationService.leverPenalite}), pour les situations de force
 * majeure.</p>
 *
 * <p><strong>Ce que le systeme ne fait pas.</strong> Il ne suspend ni n'exclut
 * personne de sa propre initiative. Au-dela de
 * {@value #SEUIL_ALERTE_GESTIONNAIRE} retards, il se borne a alerter les
 * gestionnaires de la tontine, a qui revient la decision. Priver un membre de
 * son tour engage l'epargne d'une personne reelle : cela ne saurait resulter
 * d'un declenchement automatique.</p>
 */
@Service
@Transactional
public class RelanceService {

    /** Delai minimal entre deux rappels adresses au meme membre. */
    private static final int DELAI_RELANCE_JOURS = 3;

    /** Nombre de retards a partir duquel les gestionnaires sont alertes. */
    private static final int SEUIL_ALERTE_GESTIONNAIRE = 3;

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CotisationRepository cotisationRepository;
    private final MembreRepository membreRepository;
    private final NotificationService notificationService;

    public RelanceService(CotisationRepository cotisationRepository,
                          MembreRepository membreRepository,
                          NotificationService notificationService) {
        this.cotisationRepository = cotisationRepository;
        this.membreRepository = membreRepository;
        this.notificationService = notificationService;
    }

    /** Tourne chaque jour a 8h : detection des nouveaux retards, puis relances. */
    @Scheduled(cron = "0 0 8 * * *")
    public void traiterRetards() {
        detecterNouveauxRetards();
        relancerRetardsExistants();
    }

    /**
     * Bascule en retard les cotisations echues, applique la penalite prevue,
     * relance le membre et alerte les gestionnaires en cas de recidive.
     */
    private void detecterNouveauxRetards() {
        List<Cotisation> echues = cotisationRepository
                .findByStatutAndCycle_DateFinBefore("EN_ATTENTE", LocalDate.now());
        for (Cotisation c : echues) {
            c.setStatut("EN_RETARD");
            appliquerPenalite(c);
            envoyerRelance(c);
            alerterSiRecidive(c);
        }
    }

    private void relancerRetardsExistants() {
        LocalDateTime seuil = LocalDateTime.now().minusDays(DELAI_RELANCE_JOURS);
        for (Cotisation c : cotisationRepository.findByStatut("EN_RETARD")) {
            if (c.getDerniereRelance() == null || c.getDerniereRelance().isBefore(seuil)) {
                envoyerRelance(c);
            }
        }
    }

    /**
     * Calcule la penalite une fois pour toutes, a partir du taux de la tontine.
     * Un taux nul — le cas par defaut — laisse la cotisation inchangee.
     */
    private void appliquerPenalite(Cotisation c) {
        double taux = c.getCycle().getTontine().getTauxPenalite();
        if (taux > 0 && c.getPenalite() == 0) {
            c.setPenalite(Math.round(c.getMontant() * taux / 100.0));
        }
    }

    private void envoyerRelance(Cotisation c) {
        Tontine tontine = c.getCycle().getTontine();
        String message = String.format(
                "Votre cotisation de %.0f FCFA pour le cycle n°%d de la tontine %s "
                        + "est en retard depuis le %s.",
                c.getMontant(), c.getCycle().getNumero(), tontine.getNom(),
                c.getCycle().getDateFin().format(FORMAT_DATE));
        if (c.getPenalite() > 0) {
            message += String.format(" Une penalite de %.0f FCFA a ete appliquee : "
                    + "vous devez desormais %.0f FCFA.", c.getPenalite(), c.montantDu());
        }
        message += " Merci de regulariser rapidement.";

        notificationService.envoyer(c.getMembre().getUtilisateur(),
                "COTISATION_RETARD", message, "SMS");
        c.setDerniereRelance(LocalDateTime.now());
        cotisationRepository.save(c);
    }

    /**
     * Au-dela du seuil, previent les gestionnaires de la tontine. Le systeme
     * signale, il ne sanctionne pas : la suspension du membre demeure une
     * decision humaine, prise par
     * {@code MembreService.suspendre}.
     */
    private void alerterSiRecidive(Cotisation c) {
        Membre membre = c.getMembre();
        long retards = cotisationRepository.countByMembreIdAndStatut(membre.getId(), "EN_RETARD");
        if (retards < SEUIL_ALERTE_GESTIONNAIRE) {
            return;
        }
        var utilisateur = membre.getUtilisateur();
        String message = String.format(
                "%s %s accumule %d cotisations en retard dans la tontine %s. "
                        + "Vous pouvez le suspendre depuis la fiche du membre.",
                utilisateur.getPrenom(), utilisateur.getNom(), retards,
                c.getCycle().getTontine().getNom());

        for (Membre gestionnaire : membreRepository
                .findByTontineId(c.getCycle().getTontine().getId())) {
            if (RoleGroupe.estGestionnaire(gestionnaire.getRoleGroupe())) {
                notificationService.envoyer(gestionnaire.getUtilisateur(),
                        "MEMBRE_RETARDS_REPETES", message, "SMS");
            }
        }
    }
}
