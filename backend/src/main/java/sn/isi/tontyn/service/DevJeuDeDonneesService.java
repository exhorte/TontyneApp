package sn.isi.tontyn.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.isi.tontyn.model.*;
import sn.isi.tontyn.repository.*;
import sn.isi.tontyn.util.Telephone;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Reinitialise integralement les donnees applicatives et les remplace par un
 * jeu de demonstration coherent (tontines, membres a roles varies, cycles a
 * differents stades, cotisations payees/en retard/en attente, paiements et
 * recus).
 *
 * <p>Sert a preparer un environnement de demonstration (soutenance de
 * memoire notamment) avec des donnees credibles plutot que les quelques
 * comptes bruts crees par {@link sn.isi.tontyn.config.JeuDeDonneesH2}. Le
 * scenario met deliberement en scene des roles croises d'une tontine a
 * l'autre (un gestionnaire ici, simple membre la) afin de pouvoir demontrer
 * que les droits de gestion sont bien portes par tontine, pas par
 * l'utilisateur.</p>
 */
@Service
@Transactional
public class DevJeuDeDonneesService {

    private final UtilisateurRepository utilisateurRepository;
    private final TontineRepository tontineRepository;
    private final MembreRepository membreRepository;
    private final CycleRepository cycleRepository;
    private final CotisationRepository cotisationRepository;
    private final PaiementRepository paiementRepository;
    private final RecuRepository recuRepository;
    private final NotificationRepository notificationRepository;
    private final DemandeVerificationRepository demandeVerificationRepository;
    private final MessageConversationRepository messageConversationRepository;
    private final PasswordEncoder encodeur;

    @Value("${app.comptes.code-pin:1234}")
    private String codePinDemo;
    @Value("${app.comptes.admin.telephone:+221770000001}")
    private String telAdmin;
    @Value("${app.comptes.admin.nom:Diop}")
    private String nomAdmin;
    @Value("${app.comptes.admin.prenom:Awa}")
    private String prenomAdmin;

    public DevJeuDeDonneesService(UtilisateurRepository utilisateurRepository,
                                  TontineRepository tontineRepository,
                                  MembreRepository membreRepository,
                                  CycleRepository cycleRepository,
                                  CotisationRepository cotisationRepository,
                                  PaiementRepository paiementRepository,
                                  RecuRepository recuRepository,
                                  NotificationRepository notificationRepository,
                                  DemandeVerificationRepository demandeVerificationRepository,
                                  MessageConversationRepository messageConversationRepository,
                                  PasswordEncoder encodeur) {
        this.utilisateurRepository = utilisateurRepository;
        this.tontineRepository = tontineRepository;
        this.membreRepository = membreRepository;
        this.cycleRepository = cycleRepository;
        this.cotisationRepository = cotisationRepository;
        this.paiementRepository = paiementRepository;
        this.recuRepository = recuRepository;
        this.notificationRepository = notificationRepository;
        this.demandeVerificationRepository = demandeVerificationRepository;
        this.messageConversationRepository = messageConversationRepository;
        this.encodeur = encodeur;
    }

    public String reinitialiser() {
        supprimerTout();
        return construireJeuDeDemo();
    }

    /**
     * {@code deleteAllInBatch()} plutot que {@code deleteAll()} : ce dernier se
     * contente de placer les suppressions dans la file d'action Hibernate, qui
     * execute toujours les insertions avant les suppressions a l'ecriture —
     * les comptes recrees juste apres entreraient alors en collision (meme
     * numero de telephone) avec ceux qu'on croit deja partis. Le DELETE en
     * lot s'execute immediatement, dans l'ordre appele.
     */
    private void supprimerTout() {
        recuRepository.deleteAllInBatch();
        paiementRepository.deleteAllInBatch();
        cotisationRepository.deleteAllInBatch();
        cycleRepository.deleteAllInBatch();
        membreRepository.deleteAllInBatch();
        tontineRepository.deleteAllInBatch();
        notificationRepository.deleteAllInBatch();
        demandeVerificationRepository.deleteAllInBatch();
        messageConversationRepository.deleteAllInBatch();
        utilisateurRepository.deleteAllInBatch();
    }

    private String construireJeuDeDemo() {
        creerUtilisateur(nomAdmin, prenomAdmin, telAdmin, Role.ADMINISTRATEUR);

        Utilisateur moussa = creerUtilisateur("Ndiaye", "Moussa", "+221770000010", Role.MEMBRE);
        Utilisateur fatou = creerUtilisateur("Fall", "Fatou", "+221770000011", Role.MEMBRE);
        Utilisateur ibrahima = creerUtilisateur("Sow", "Ibrahima", "+221770000012", Role.MEMBRE);
        Utilisateur aminata = creerUtilisateur("Ba", "Aminata", "+221770000013", Role.MEMBRE);
        Utilisateur cheikh = creerUtilisateur("Diallo", "Cheikh", "+221770000014", Role.MEMBRE);
        Utilisateur mariama = creerUtilisateur("Gueye", "Mariama", "+221770000015", Role.MEMBRE);
        Utilisateur ousmane = creerUtilisateur("Diouf", "Ousmane", "+221770000016", Role.MEMBRE);
        Utilisateur khady = creerUtilisateur("Sarr", "Khady", "+221770000017", Role.MEMBRE);
        Utilisateur modou = creerUtilisateur("Ndoye", "Modou", "+221770000018", Role.MEMBRE);
        Utilisateur astou = creerUtilisateur("Kane", "Astou", "+221770000019", Role.MEMBRE);
        Utilisateur abdoulaye = creerUtilisateur("Cisse", "Abdoulaye", "+221770000020", Role.MEMBRE);

        LocalDate aujourdhui = LocalDate.now();

        // --- Tontine 1 : Tontine des Enseignants (mensuelle) --------------
        Tontine t1 = creerTontine("Tontine des Enseignants",
                "Cotisation mensuelle entre collegues d'un meme etablissement.",
                25000, "MENSUELLE", 5, 5,
                aujourdhui.minusMonths(2).withDayOfMonth(1));
        Membre t1m1 = ajouterMembre(t1, moussa, RoleGroupe.GESTIONNAIRE, 1);
        Membre t1m2 = ajouterMembre(t1, fatou, RoleGroupe.MEMBRE, 2);
        Membre t1m3 = ajouterMembre(t1, ibrahima, RoleGroupe.MEMBRE, 3);
        Membre t1m4 = ajouterMembre(t1, aminata, RoleGroupe.MEMBRE, 4);
        Membre t1m5 = ajouterMembre(t1, cheikh, RoleGroupe.MEMBRE, 5);

        Cycle t1c1 = creerCycle(t1, 1, "CLOTURE", t1m1,
                aujourdhui.minusMonths(2).withDayOfMonth(1),
                aujourdhui.minusMonths(1).withDayOfMonth(1).minusDays(1));
        for (Membre m : List.of(t1m1, t1m2, t1m3, t1m4, t1m5)) {
            confirmerCotisation(t1c1, m, 25000, MethodePaiement.ORANGE_MONEY);
        }

        Cycle t1c2 = creerCycle(t1, 2, "EN_COURS", t1m2,
                aujourdhui.minusMonths(1).withDayOfMonth(1),
                aujourdhui.withDayOfMonth(1).minusDays(1));
        confirmerCotisation(t1c2, t1m1, 25000, MethodePaiement.WAVE);
        confirmerCotisation(t1c2, t1m2, 25000, MethodePaiement.ORANGE_MONEY);
        confirmerCotisation(t1c2, t1m3, 25000, MethodePaiement.WAVE);
        cotisationEnRetard(t1c2, t1m4, 25000, 1250);
        cotisationAvecPaiementInitie(t1c2, t1m5, 25000, MethodePaiement.ORANGE_MONEY);

        creerCycle(t1, 3, "PLANIFIE", t1m3,
                aujourdhui.withDayOfMonth(1), aujourdhui.plusMonths(1).withDayOfMonth(1).minusDays(1));

        // --- Tontine 2 : Tontine du Marche Sandaga (hebdomadaire) ---------
        Tontine t2 = creerTontine("Tontine du Marche Sandaga",
                "Cotisation hebdomadaire entre commercantes du marche.",
                10000, "HEBDOMADAIRE", 6, 10, aujourdhui.minusWeeks(2));
        Membre t2m1 = ajouterMembre(t2, mariama, RoleGroupe.GESTIONNAIRE, 1);
        Membre t2m2 = ajouterMembre(t2, ousmane, RoleGroupe.MEMBRE, 2);
        Membre t2m3 = ajouterMembre(t2, khady, RoleGroupe.MEMBRE, 3);
        Membre t2m4 = ajouterMembre(t2, modou, RoleGroupe.MEMBRE, 4);
        Membre t2m5 = ajouterMembre(t2, astou, RoleGroupe.MEMBRE, 5);
        Membre t2m6 = ajouterMembre(t2, abdoulaye, RoleGroupe.MEMBRE, 6);

        Cycle t2c1 = creerCycle(t2, 1, "CLOTURE", t2m1,
                aujourdhui.minusWeeks(2), aujourdhui.minusWeeks(1).minusDays(1));
        for (Membre m : List.of(t2m1, t2m2, t2m3, t2m4, t2m5, t2m6)) {
            confirmerCotisation(t2c1, m, 10000, MethodePaiement.WAVE);
        }

        Cycle t2c2 = creerCycle(t2, 2, "EN_COURS", t2m2,
                aujourdhui.minusWeeks(1), aujourdhui.plusWeeks(1).minusDays(1));
        confirmerCotisation(t2c2, t2m1, 10000, MethodePaiement.ORANGE_MONEY);
        confirmerCotisation(t2c2, t2m2, 10000, MethodePaiement.WAVE);
        confirmerCotisation(t2c2, t2m3, 10000, MethodePaiement.ORANGE_MONEY);
        confirmerCotisation(t2c2, t2m4, 10000, MethodePaiement.WAVE);
        cotisationEnRetard(t2c2, t2m5, 10000, 1000);
        cotisationEnRetard(t2c2, t2m6, 10000, 1000);

        creerCycle(t2, 3, "PLANIFIE", t2m3,
                aujourdhui.plusWeeks(1), aujourdhui.plusWeeks(2).minusDays(1));

        // --- Tontine 3 : demonstration des roles croises d'une tontine a l'autre ---
        Tontine t3 = creerTontine("Tontine des Voisins",
                "Fatou, simple membre chez les Enseignants, gere ici sa propre tontine, "
                        + "a laquelle Moussa (gestionnaire chez les Enseignants) participe "
                        + "comme simple membre.",
                15000, "MENSUELLE", 4, 0, aujourdhui.withDayOfMonth(1));
        Membre t3m1 = ajouterMembre(t3, fatou, RoleGroupe.GESTIONNAIRE, 1);
        Membre t3m2 = ajouterMembre(t3, moussa, RoleGroupe.MEMBRE, 2);
        Membre t3m3 = ajouterMembre(t3, ibrahima, RoleGroupe.MEMBRE, 3);
        Membre t3m4 = ajouterMembre(t3, khady, RoleGroupe.MEMBRE, 4);

        Cycle t3c1 = creerCycle(t3, 1, "EN_COURS", t3m1,
                aujourdhui.withDayOfMonth(1), aujourdhui.plusMonths(1).withDayOfMonth(1).minusDays(1));
        confirmerCotisation(t3c1, t3m1, 15000, MethodePaiement.ORANGE_MONEY);
        confirmerCotisation(t3c1, t3m2, 15000, MethodePaiement.WAVE);
        cotisationSimple(t3c1, t3m3, 15000);
        cotisationEnRetard(t3c1, t3m4, 15000, 0);

        return "Jeu de donnees reinitialise : " + tontineRepository.count() + " tontines, "
                + utilisateurRepository.count() + " utilisateurs, "
                + membreRepository.count() + " membres, "
                + cycleRepository.count() + " cycles, "
                + cotisationRepository.count() + " cotisations, "
                + paiementRepository.count() + " paiements.";
    }

    // ------------------------------------------------------------------
    //  Constructeurs elementaires
    // ------------------------------------------------------------------

    private Utilisateur creerUtilisateur(String nom, String prenom, String telephone, Role role) {
        Utilisateur u = new Utilisateur();
        u.setNom(nom);
        u.setPrenom(prenom);
        u.setTelephone(Telephone.normaliser(telephone));
        u.setCodePin(encodeur.encode(codePinDemo));
        u.setRole(role);
        return utilisateurRepository.save(u);
    }

    private Tontine creerTontine(String nom, String description, double montant, String periodicite,
                                 int nombreMembres, double tauxPenalite, LocalDate dateDebut) {
        Tontine t = new Tontine();
        t.setNom(nom);
        t.setDescription(description);
        t.setMontantCotisation(montant);
        t.setPeriodicite(periodicite);
        t.setNombreMembres(nombreMembres);
        t.setDateDebut(dateDebut);
        t.setTauxPenalite(tauxPenalite);
        return tontineRepository.save(t);
    }

    private Membre ajouterMembre(Tontine tontine, Utilisateur utilisateur, String roleGroupe, int ordreTour) {
        Membre m = new Membre();
        m.setTontine(tontine);
        m.setUtilisateur(utilisateur);
        m.setRoleGroupe(roleGroupe);
        m.setOrdreTour(ordreTour);
        return membreRepository.save(m);
    }

    private Cycle creerCycle(Tontine tontine, int numero, String statut, Membre beneficiaire,
                             LocalDate debut, LocalDate fin) {
        Cycle c = new Cycle();
        c.setTontine(tontine);
        c.setNumero(numero);
        c.setStatut(statut);
        c.setBeneficiaire(beneficiaire);
        c.setDateDebut(debut);
        c.setDateFin(fin);
        return cycleRepository.save(c);
    }

    /** Cotisation payee, avec son paiement confirme et le recu associe. */
    private void confirmerCotisation(Cycle cycle, Membre membre, double montant, MethodePaiement methode) {
        Cotisation c = cotisationSimple(cycle, membre, montant);
        c.setStatut("PAYEE");
        cotisationRepository.save(c);

        Paiement p = new Paiement();
        p.setCotisation(c);
        p.setMontant(montant);
        p.setMethode(methode);
        p.setReference(reference(methode));
        p.setStatut("CONFIRME");
        paiementRepository.save(p);

        Recu r = new Recu();
        r.setPaiement(p);
        r.setMontant(montant);
        r.setNumero("RC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        recuRepository.save(r);
    }

    /** Cotisation en retard, penalite comprise, sans paiement. */
    private void cotisationEnRetard(Cycle cycle, Membre membre, double montant, double penalite) {
        Cotisation c = cotisationSimple(cycle, membre, montant);
        c.setStatut("EN_RETARD");
        c.setPenalite(penalite);
        cotisationRepository.save(c);
    }

    /** Cotisation en attente, avec un paiement initie mais pas encore confirme (demo du workflow). */
    private void cotisationAvecPaiementInitie(Cycle cycle, Membre membre, double montant,
                                              MethodePaiement methode) {
        Cotisation c = cotisationSimple(cycle, membre, montant);
        Paiement p = new Paiement();
        p.setCotisation(c);
        p.setMontant(montant);
        p.setMethode(methode);
        p.setReference(reference(methode));
        p.setStatut("INITIE");
        paiementRepository.save(p);
    }

    /** Cotisation simple, en attente, sans paiement. */
    private Cotisation cotisationSimple(Cycle cycle, Membre membre, double montant) {
        Cotisation c = new Cotisation();
        c.setCycle(cycle);
        c.setMembre(membre);
        c.setMontant(montant);
        return cotisationRepository.save(c);
    }

    private String reference(MethodePaiement methode) {
        String prefixe = methode == MethodePaiement.WAVE ? "WV" : "OM";
        return prefixe + "-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
    }
}
