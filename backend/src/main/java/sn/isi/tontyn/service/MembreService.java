package sn.isi.tontyn.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.isi.tontyn.dto.AjoutMembreRequest;
import sn.isi.tontyn.dto.MembreRequest;
import sn.isi.tontyn.dto.MembreResponse;
import sn.isi.tontyn.dto.ScoreFiabiliteResponse;
import sn.isi.tontyn.exception.ConflitMetierException;
import sn.isi.tontyn.exception.RessourceIntrouvableException;
import sn.isi.tontyn.model.Membre;
import sn.isi.tontyn.model.RoleGroupe;
import sn.isi.tontyn.model.Tontine;
import sn.isi.tontyn.model.Utilisateur;
import sn.isi.tontyn.repository.CotisationRepository;
import sn.isi.tontyn.repository.MembreRepository;
import sn.isi.tontyn.repository.UtilisateurRepository;
import sn.isi.tontyn.security.SecuriteTontine;

import java.util.List;

@Service
@Transactional
public class MembreService {

    private final MembreRepository membreRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CotisationRepository cotisationRepository;
    private final TontineService tontineService;
    private final DeplafonnementService deplafonnement;
    private final ScoreFiabiliteService scoreFiabiliteService;
    private final SecuriteTontine securiteTontine;

    public MembreService(MembreRepository membreRepository,
                         UtilisateurRepository utilisateurRepository,
                         CotisationRepository cotisationRepository,
                         TontineService tontineService,
                         DeplafonnementService deplafonnement,
                         ScoreFiabiliteService scoreFiabiliteService,
                         SecuriteTontine securiteTontine) {
        this.membreRepository = membreRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.cotisationRepository = cotisationRepository;
        this.tontineService = tontineService;
        this.deplafonnement = deplafonnement;
        this.scoreFiabiliteService = scoreFiabiliteService;
        this.securiteTontine = securiteTontine;
    }

    @Transactional(readOnly = true)
    public List<MembreResponse> lister() {
        return membreRepository.findAll().stream().map(this::versReponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MembreResponse> listerParTontine(Long tontineId) {
        tontineService.chargerTontine(tontineId);   // 404 si la tontine n'existe pas
        return membreRepository.findByTontineId(tontineId).stream()
                .map(this::versReponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MembreResponse> listerParUtilisateur(Long utilisateurId) {
        return membreRepository.findByUtilisateurId(utilisateurId).stream()
                .map(this::versReponse).toList();
    }

    @Transactional(readOnly = true)
    public MembreResponse obtenir(Long id) {
        return versReponse(chargerMembre(id));
    }

    /**
     * Construit la reponse d'un membre, score de fiabilite inclus si et
     * seulement si l'appelant courant est autorise a le consulter : le
     * gestionnaire de la tontine concernee, ou le membre pour son propre
     * score (voir PROMPT_SCORE_FIABILITE.md). Dans le cas contraire, le score
     * est simplement absent de la reponse plutot que de bloquer l'appel :
     * ces routes de liste restent aujourd'hui accessibles a tout utilisateur
     * authentifie (limite preexistante, documentee dans ETAT_DU_PROJET.md).
     */
    private MembreResponse versReponse(Membre membre) {
        boolean autorise = securiteTontine.gereMembre(membre.getId())
                || securiteTontine.estSonPropreMembre(membre.getId());
        if (!autorise) {
            return MembreResponse.from(membre);
        }
        ScoreFiabiliteResponse resume = scoreFiabiliteService.calculerResume(membre);
        return MembreResponse.from(membre, resume.score(), resume.niveauConfiance());
    }

    public MembreResponse creer(MembreRequest req) {
        return ajouterATontine(req.tontineId(),
                new AjoutMembreRequest(req.utilisateurId(), req.roleGroupe(), req.ordreTour()));
    }

    /** Endpoint metier : ajoute un utilisateur comme membre d'une tontine. */
    public MembreResponse ajouterATontine(Long tontineId, AjoutMembreRequest req) {
        Tontine tontine = tontineService.chargerTontine(tontineId);

        if ("CLOTUREE".equals(tontine.getStatut())) {
            throw new ConflitMetierException(
                    "Impossible d'ajouter un membre : la tontine est cloturee.");
        }
        Utilisateur utilisateur = utilisateurRepository.findById(req.utilisateurId())
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur",
                        req.utilisateurId()));

        // Une tontine au-dessus du plafond n'accepte que les comptes verifies.
        deplafonnement.exigerAjoutAutorise(tontine, utilisateur);

        if (membreRepository.existsByTontineIdAndUtilisateurId(tontineId, req.utilisateurId())) {
            throw new ConflitMetierException(
                    "Cet utilisateur est deja membre de la tontine \"" + tontine.getNom() + "\".");
        }
        long effectif = membreRepository.countByTontineId(tontineId);
        if (effectif >= tontine.getNombreMembres()) {
            throw new ConflitMetierException("La tontine a atteint son effectif maximal ("
                    + tontine.getNombreMembres() + " membres).");
        }

        int ordreTour = (req.ordreTour() != null) ? req.ordreTour() : (int) effectif + 1;
        if (membreRepository.existsByTontineIdAndOrdreTour(tontineId, ordreTour)) {
            throw new ConflitMetierException(
                    "L'ordre de tour " + ordreTour + " est deja attribue dans cette tontine.");
        }

        Membre membre = new Membre();
        membre.setTontine(tontine);
        membre.setUtilisateur(utilisateur);
        membre.setRoleGroupe(RoleGroupe.normaliser(req.roleGroupe()));
        membre.setOrdreTour(ordreTour);
        return versReponse(membreRepository.save(membre));
    }

    public MembreResponse modifier(Long id, AjoutMembreRequest req) {
        Membre membre = chargerMembre(id);
        if (req.roleGroupe() != null) {
            membre.setRoleGroupe(RoleGroupe.normaliser(req.roleGroupe()));
        }
        if (req.ordreTour() != null && req.ordreTour() != membre.getOrdreTour()) {
            if (membreRepository.existsByTontineIdAndOrdreTour(
                    membre.getTontine().getId(), req.ordreTour())) {
                throw new ConflitMetierException("L'ordre de tour " + req.ordreTour()
                        + " est deja attribue dans cette tontine.");
            }
            membre.setOrdreTour(req.ordreTour());
        }
        return versReponse(membreRepository.save(membre));
    }

    /** Retire un membre : refuse si des cotisations lui sont deja rattachees. */
    public void supprimer(Long id) {
        Membre membre = chargerMembre(id);
        if (!cotisationRepository.findByMembreId(id).isEmpty()) {
            throw new ConflitMetierException("Retrait impossible : ce membre possede des "
                    + "cotisations. Suspendez-le plutot que de le supprimer.");
        }
        membreRepository.delete(membre);
    }

    /** Suspend un membre sans rompre l'historique de ses cotisations. */
    public MembreResponse suspendre(Long id) {
        Membre membre = chargerMembre(id);
        if ("SUSPENDU".equals(membre.getStatut())) {
            throw new ConflitMetierException("Ce membre est deja suspendu.");
        }
        membre.setStatut("SUSPENDU");
        return versReponse(membreRepository.save(membre));
    }

    public MembreResponse reactiver(Long id) {
        Membre membre = chargerMembre(id);
        membre.setStatut("ACTIF");
        return versReponse(membreRepository.save(membre));
    }

    @Transactional(readOnly = true)
    public Membre chargerMembre(Long id) {
        return membreRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Membre", id));
    }
}
