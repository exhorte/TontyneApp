package sn.isi.tontinesafe.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.isi.tontinesafe.dto.CotisationRequest;
import sn.isi.tontinesafe.dto.CotisationResponse;
import sn.isi.tontinesafe.exception.ConflitMetierException;
import sn.isi.tontinesafe.exception.RessourceIntrouvableException;
import sn.isi.tontinesafe.model.Cotisation;
import sn.isi.tontinesafe.model.Cycle;
import sn.isi.tontinesafe.model.Membre;
import sn.isi.tontinesafe.repository.CotisationRepository;
import sn.isi.tontinesafe.repository.PaiementRepository;

import java.util.List;

@Service
@Transactional
public class CotisationService {

    private final CotisationRepository cotisationRepository;
    private final PaiementRepository paiementRepository;
    private final CycleService cycleService;
    private final MembreService membreService;

    public CotisationService(CotisationRepository cotisationRepository,
                             PaiementRepository paiementRepository,
                             CycleService cycleService,
                             MembreService membreService) {
        this.cotisationRepository = cotisationRepository;
        this.paiementRepository = paiementRepository;
        this.cycleService = cycleService;
        this.membreService = membreService;
    }

    @Transactional(readOnly = true)
    public List<CotisationResponse> lister() {
        return cotisationRepository.findAll().stream().map(CotisationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CotisationResponse> listerParCycle(Long cycleId) {
        cycleService.chargerCycle(cycleId);
        return cotisationRepository.findByCycleId(cycleId).stream()
                .map(CotisationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CotisationResponse> listerParMembre(Long membreId) {
        membreService.chargerMembre(membreId);
        return cotisationRepository.findByMembreId(membreId).stream()
                .map(CotisationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CotisationResponse> listerParTontine(Long tontineId) {
        return cotisationRepository.findByTontineId(tontineId).stream()
                .map(CotisationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CotisationResponse obtenir(Long id) {
        return CotisationResponse.from(chargerCotisation(id));
    }

    /** Endpoint metier : enregistre la cotisation d'un membre pour un cycle donne. */
    public CotisationResponse enregistrer(CotisationRequest req) {
        Cycle cycle = cycleService.chargerCycle(req.cycleId());
        Membre membre = membreService.chargerMembre(req.membreId());

        if (!membre.getTontine().getId().equals(cycle.getTontine().getId())) {
            throw new ConflitMetierException(
                    "Ce membre n'appartient pas a la tontine du cycle vise.");
        }
        if ("CLOTURE".equals(cycle.getStatut())) {
            throw new ConflitMetierException(
                    "Le cycle est cloture : aucune nouvelle cotisation n'est acceptee.");
        }
        if (!"ACTIF".equals(membre.getStatut())) {
            throw new ConflitMetierException(
                    "Ce membre est " + membre.getStatut() + " : il ne peut pas cotiser.");
        }
        if (cotisationRepository.existsByCycleIdAndMembreId(req.cycleId(), req.membreId())) {
            throw new ConflitMetierException(
                    "Une cotisation existe deja pour ce membre sur ce cycle.");
        }

        Cotisation cotisation = new Cotisation();
        cotisation.setCycle(cycle);
        cotisation.setMembre(membre);
        cotisation.setMontant(req.montant() != null
                ? req.montant() : cycle.getTontine().getMontantCotisation());
        return CotisationResponse.from(cotisationRepository.save(cotisation));
    }

    public CotisationResponse modifier(Long id, CotisationRequest req) {
        Cotisation cotisation = chargerCotisation(id);
        if ("PAYEE".equals(cotisation.getStatut())) {
            throw new ConflitMetierException(
                    "Une cotisation payee ne peut plus etre modifiee.");
        }
        if (req.montant() != null) {
            cotisation.setMontant(req.montant());
        }
        return CotisationResponse.from(cotisationRepository.save(cotisation));
    }

    public void supprimer(Long id) {
        Cotisation cotisation = chargerCotisation(id);
        if (paiementRepository.existsByCotisationId(id)) {
            throw new ConflitMetierException(
                    "Suppression impossible : un paiement est rattache a cette cotisation.");
        }
        cotisationRepository.delete(cotisation);
    }

    /** Bascule la cotisation en PAYEE (appele apres confirmation d'un paiement). */
    public void marquerPayee(Cotisation cotisation) {
        cotisation.setStatut("PAYEE");
        cotisationRepository.save(cotisation);
    }

    @Transactional(readOnly = true)
    public Cotisation chargerCotisation(Long id) {
        return cotisationRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Cotisation", id));
    }
}
