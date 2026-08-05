package sn.isi.tontyn.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.isi.tontyn.dto.AjoutMembreRequest;
import sn.isi.tontyn.dto.MembreRequest;
import sn.isi.tontyn.dto.MembreResponse;
import sn.isi.tontyn.exception.ConflitMetierException;
import sn.isi.tontyn.exception.RessourceIntrouvableException;
import sn.isi.tontyn.model.Membre;
import sn.isi.tontyn.model.Tontine;
import sn.isi.tontyn.model.Utilisateur;
import sn.isi.tontyn.repository.CotisationRepository;
import sn.isi.tontyn.repository.MembreRepository;
import sn.isi.tontyn.repository.UtilisateurRepository;

import java.util.List;

@Service
@Transactional
public class MembreService {

    private final MembreRepository membreRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CotisationRepository cotisationRepository;
    private final TontineService tontineService;

    public MembreService(MembreRepository membreRepository,
                         UtilisateurRepository utilisateurRepository,
                         CotisationRepository cotisationRepository,
                         TontineService tontineService) {
        this.membreRepository = membreRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.cotisationRepository = cotisationRepository;
        this.tontineService = tontineService;
    }

    @Transactional(readOnly = true)
    public List<MembreResponse> lister() {
        return membreRepository.findAll().stream().map(MembreResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MembreResponse> listerParTontine(Long tontineId) {
        tontineService.chargerTontine(tontineId);   // 404 si la tontine n'existe pas
        return membreRepository.findByTontineId(tontineId).stream()
                .map(MembreResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MembreResponse> listerParUtilisateur(Long utilisateurId) {
        return membreRepository.findByUtilisateurId(utilisateurId).stream()
                .map(MembreResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MembreResponse obtenir(Long id) {
        return MembreResponse.from(chargerMembre(id));
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
        membre.setRoleGroupe(req.roleGroupe() != null ? req.roleGroupe() : "MEMBRE");
        membre.setOrdreTour(ordreTour);
        return MembreResponse.from(membreRepository.save(membre));
    }

    public MembreResponse modifier(Long id, AjoutMembreRequest req) {
        Membre membre = chargerMembre(id);
        if (req.roleGroupe() != null) {
            membre.setRoleGroupe(req.roleGroupe());
        }
        if (req.ordreTour() != null && req.ordreTour() != membre.getOrdreTour()) {
            if (membreRepository.existsByTontineIdAndOrdreTour(
                    membre.getTontine().getId(), req.ordreTour())) {
                throw new ConflitMetierException("L'ordre de tour " + req.ordreTour()
                        + " est deja attribue dans cette tontine.");
            }
            membre.setOrdreTour(req.ordreTour());
        }
        return MembreResponse.from(membreRepository.save(membre));
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
        return MembreResponse.from(membreRepository.save(membre));
    }

    public MembreResponse reactiver(Long id) {
        Membre membre = chargerMembre(id);
        membre.setStatut("ACTIF");
        return MembreResponse.from(membreRepository.save(membre));
    }

    @Transactional(readOnly = true)
    public Membre chargerMembre(Long id) {
        return membreRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Membre", id));
    }
}
