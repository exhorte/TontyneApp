package sn.isi.tontyn.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.isi.tontyn.dto.TontineRequest;
import sn.isi.tontyn.dto.TontineResponse;
import sn.isi.tontyn.exception.ConflitMetierException;
import sn.isi.tontyn.exception.RessourceIntrouvableException;
import sn.isi.tontyn.model.Membre;
import sn.isi.tontyn.model.RoleGroupe;
import sn.isi.tontyn.model.Tontine;
import sn.isi.tontyn.model.Utilisateur;
import sn.isi.tontyn.repository.CycleRepository;
import sn.isi.tontyn.repository.MembreRepository;
import sn.isi.tontyn.repository.TontineRepository;
import sn.isi.tontyn.security.SecuriteTontine;

import java.util.List;

@Service
@Transactional
public class TontineService {

    private final TontineRepository tontineRepository;
    private final MembreRepository membreRepository;
    private final CycleRepository cycleRepository;
    private final SecuriteTontine securite;

    public TontineService(TontineRepository tontineRepository,
                          MembreRepository membreRepository,
                          CycleRepository cycleRepository,
                          SecuriteTontine securite) {
        this.tontineRepository = tontineRepository;
        this.membreRepository = membreRepository;
        this.cycleRepository = cycleRepository;
        this.securite = securite;
    }

    @Transactional(readOnly = true)
    public List<TontineResponse> lister() {
        return tontineRepository.findAll().stream().map(this::versReponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TontineResponse> listerParStatut(String statut) {
        return tontineRepository.findByStatut(statut).stream().map(this::versReponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TontineResponse> listerParUtilisateur(Long utilisateurId) {
        return tontineRepository.findByUtilisateurId(utilisateurId).stream()
                .map(this::versReponse).toList();
    }

    @Transactional(readOnly = true)
    public TontineResponse obtenir(Long id) {
        return versReponse(chargerTontine(id));
    }

    /**
     * Cree une tontine et rattache immediatement son auteur en qualite
     * d'administrateur de celle-ci.
     *
     * <p>C'est ce rattachement qui fonde tout le controle d'acces ulterieur :
     * les droits de gestion ne decoulent plus d'un role global, mais de la
     * qualite d'administrateur au sein d'une tontine determinee. Le createur
     * recoit le premier tour de beneficiaire, qu'il pourra reordonner par la
     * suite.</p>
     */
    public TontineResponse creer(TontineRequest req) {
        Tontine t = new Tontine();
        appliquer(t, req);
        t.setStatut(req.statut() != null ? req.statut() : "ACTIVE");
        if (t.getDateDebut() == null) {
            t.setDateDebut(java.time.LocalDate.now());
        }
        Tontine enregistree = tontineRepository.save(t);

        securite.utilisateurCourant().ifPresent(auteur -> rattacherAdministrateur(enregistree, auteur));

        return versReponse(enregistree);
    }

    /** Inscrit l'auteur de la tontine comme premier membre, avec les droits d'administration. */
    private void rattacherAdministrateur(Tontine tontine, Utilisateur auteur) {
        if (membreRepository.existsByTontineIdAndUtilisateurId(tontine.getId(), auteur.getId())) {
            return;
        }
        Membre membre = new Membre();
        membre.setTontine(tontine);
        membre.setUtilisateur(auteur);
        membre.setRoleGroupe(RoleGroupe.ADMINISTRATEUR);
        membre.setOrdreTour(1);
        membre.setStatut("ACTIF");
        membreRepository.save(membre);
    }

    public TontineResponse modifier(Long id, TontineRequest req) {
        Tontine t = chargerTontine(id);
        long inscrits = membreRepository.countByTontineId(id);
        if (req.nombreMembres() < inscrits) {
            throw new ConflitMetierException("La tontine compte deja " + inscrits
                    + " membre(s) : la capacite ne peut pas etre reduite a "
                    + req.nombreMembres() + ".");
        }
        appliquer(t, req);
        if (req.statut() != null) {
            t.setStatut(req.statut());
        }
        return versReponse(tontineRepository.save(t));
    }

    public void supprimer(Long id) {
        Tontine t = chargerTontine(id);
        if (membreRepository.countByTontineId(id) > 0) {
            throw new ConflitMetierException(
                    "Suppression impossible : des membres sont rattaches a cette tontine.");
        }
        if (cycleRepository.existsByTontineId(id)) {
            throw new ConflitMetierException(
                    "Suppression impossible : des cycles existent pour cette tontine.");
        }
        tontineRepository.delete(t);
    }

    /** Cloture la tontine : plus aucun cycle ni cotisation ne peut y etre ajoute. */
    public TontineResponse cloturer(Long id) {
        Tontine t = chargerTontine(id);
        if ("CLOTUREE".equals(t.getStatut())) {
            throw new ConflitMetierException("Cette tontine est deja cloturee.");
        }
        t.setStatut("CLOTUREE");
        return versReponse(tontineRepository.save(t));
    }

    /** Chargement partage avec les autres services. */
    @Transactional(readOnly = true)
    public Tontine chargerTontine(Long id) {
        return tontineRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Tontine", id));
    }

    private void appliquer(Tontine t, TontineRequest req) {
        t.setNom(req.nom());
        t.setDescription(req.description());
        t.setMontantCotisation(req.montantCotisation());
        t.setPeriodicite(req.periodicite());
        t.setNombreMembres(req.nombreMembres());
        if (req.dateDebut() != null) {
            t.setDateDebut(req.dateDebut());
        }
    }

    private TontineResponse versReponse(Tontine t) {
        return TontineResponse.from(t,
                membreRepository.countByTontineId(t.getId()),
                cycleRepository.countByTontineId(t.getId()),
                securite.gereTontine(t.getId()),
                securite.appartientTontine(t.getId()));
    }
}
