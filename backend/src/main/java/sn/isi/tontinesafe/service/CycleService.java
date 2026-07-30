package sn.isi.tontinesafe.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.isi.tontinesafe.dto.CycleRequest;
import sn.isi.tontinesafe.dto.CycleResponse;
import sn.isi.tontinesafe.dto.GenerationCyclesRequest;
import sn.isi.tontinesafe.exception.ConflitMetierException;
import sn.isi.tontinesafe.exception.RessourceIntrouvableException;
import sn.isi.tontinesafe.model.Cycle;
import sn.isi.tontinesafe.model.Membre;
import sn.isi.tontinesafe.model.Tontine;
import sn.isi.tontinesafe.repository.CotisationRepository;
import sn.isi.tontinesafe.repository.CycleRepository;
import sn.isi.tontinesafe.repository.MembreRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class CycleService {

    private final CycleRepository cycleRepository;
    private final MembreRepository membreRepository;
    private final CotisationRepository cotisationRepository;
    private final TontineService tontineService;

    public CycleService(CycleRepository cycleRepository,
                        MembreRepository membreRepository,
                        CotisationRepository cotisationRepository,
                        TontineService tontineService) {
        this.cycleRepository = cycleRepository;
        this.membreRepository = membreRepository;
        this.cotisationRepository = cotisationRepository;
        this.tontineService = tontineService;
    }

    @Transactional(readOnly = true)
    public List<CycleResponse> lister() {
        return cycleRepository.findAll().stream().map(this::versReponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CycleResponse> listerParTontine(Long tontineId) {
        tontineService.chargerTontine(tontineId);
        return cycleRepository.findByTontineIdOrderByNumeroAsc(tontineId).stream()
                .map(this::versReponse).toList();
    }

    @Transactional(readOnly = true)
    public CycleResponse obtenir(Long id) {
        return versReponse(chargerCycle(id));
    }

    public CycleResponse creer(CycleRequest req) {
        Tontine tontine = tontineService.chargerTontine(req.tontineId());
        Cycle cycle = new Cycle();
        cycle.setTontine(tontine);
        appliquer(cycle, req);
        cycle.setStatut(req.statut() != null ? req.statut() : "EN_COURS");
        return versReponse(cycleRepository.save(cycle));
    }

    public CycleResponse modifier(Long id, CycleRequest req) {
        Cycle cycle = chargerCycle(id);
        appliquer(cycle, req);
        if (req.statut() != null) {
            cycle.setStatut(req.statut());
        }
        return versReponse(cycleRepository.save(cycle));
    }

    public void supprimer(Long id) {
        Cycle cycle = chargerCycle(id);
        if (!cotisationRepository.findByCycleId(id).isEmpty()) {
            throw new ConflitMetierException(
                    "Suppression impossible : des cotisations sont rattachees a ce cycle.");
        }
        cycleRepository.delete(cycle);
    }

    /**
     * Endpoint metier : genere un cycle par membre actif de la tontine.
     * Chaque membre devient beneficiaire du cycle correspondant a son ordre de tour,
     * et les dates s'enchainent selon la periodicite de la tontine.
     */
    public List<CycleResponse> genererCycles(Long tontineId, GenerationCyclesRequest req) {
        Tontine tontine = tontineService.chargerTontine(tontineId);

        if (cycleRepository.existsByTontineId(tontineId)) {
            throw new ConflitMetierException("Des cycles existent deja pour cette tontine. "
                    + "Supprimez-les avant de relancer la generation.");
        }
        List<Membre> membres =
                membreRepository.findByTontineIdAndStatutOrderByOrdreTourAsc(tontineId, "ACTIF");
        if (membres.isEmpty()) {
            throw new ConflitMetierException(
                    "Aucun membre actif : impossible de generer les cycles.");
        }

        List<Cycle> cycles = new ArrayList<>();
        LocalDate debut = req.dateDebut();
        for (int i = 0; i < membres.size(); i++) {
            LocalDate fin = ajouterPeriode(debut, tontine.getPeriodicite()).minusDays(1);
            Cycle cycle = new Cycle();
            cycle.setTontine(tontine);
            cycle.setNumero(i + 1);
            cycle.setDateDebut(debut);
            cycle.setDateFin(fin);
            cycle.setBeneficiaire(membres.get(i));
            cycle.setStatut(i == 0 ? "EN_COURS" : "PLANIFIE");
            cycles.add(cycle);
            debut = fin.plusDays(1);
        }
        return cycleRepository.saveAll(cycles).stream().map(this::versReponse).toList();
    }

    /**
     * Cloture un cycle : chaque membre actif de la tontine doit avoir une cotisation
     * payee. Les cotisations non encore enregistrees comptent donc comme manquantes.
     */
    public CycleResponse cloturer(Long id) {
        Cycle cycle = chargerCycle(id);
        if ("CLOTURE".equals(cycle.getStatut())) {
            throw new ConflitMetierException("Ce cycle est deja cloture.");
        }
        long membresAttendus = membreRepository
                .findByTontineIdAndStatutOrderByOrdreTourAsc(cycle.getTontine().getId(), "ACTIF")
                .size();
        long payees = cotisationRepository.findByCycleIdAndStatut(id, "PAYEE").size();

        if (payees < membresAttendus) {
            throw new ConflitMetierException("Cloture impossible : " + payees + " cotisation(s) "
                    + "payee(s) sur " + membresAttendus + " membre(s) actif(s). "
                    + (membresAttendus - payees) + " cotisation(s) restent dues.");
        }
        cycle.setStatut("CLOTURE");
        return versReponse(cycleRepository.save(cycle));
    }

    @Transactional(readOnly = true)
    public Cycle chargerCycle(Long id) {
        return cycleRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Cycle", id));
    }

    private void appliquer(Cycle cycle, CycleRequest req) {
        if (req.dateFin() != null && req.dateFin().isBefore(req.dateDebut())) {
            throw new ConflitMetierException(
                    "La date de fin ne peut pas preceder la date de debut.");
        }
        cycle.setNumero(req.numero());
        cycle.setDateDebut(req.dateDebut());
        cycle.setDateFin(req.dateFin() != null ? req.dateFin()
                : ajouterPeriode(req.dateDebut(), cycle.getTontine().getPeriodicite()).minusDays(1));

        if (req.beneficiaireId() != null) {
            Membre beneficiaire = membreRepository.findById(req.beneficiaireId())
                    .orElseThrow(() -> new RessourceIntrouvableException("Membre",
                            req.beneficiaireId()));
            if (!beneficiaire.getTontine().getId().equals(cycle.getTontine().getId())) {
                throw new ConflitMetierException(
                        "Le beneficiaire choisi n'appartient pas a cette tontine.");
            }
            cycle.setBeneficiaire(beneficiaire);
        }
    }

    private LocalDate ajouterPeriode(LocalDate date, String periodicite) {
        return switch (periodicite == null ? "MENSUELLE" : periodicite) {
            case "QUOTIDIENNE"  -> date.plusDays(1);
            case "HEBDOMADAIRE" -> date.plusWeeks(1);
            case "BIMENSUELLE"  -> date.plusWeeks(2);
            case "TRIMESTRIELLE" -> date.plusMonths(3);
            default              -> date.plusMonths(1);
        };
    }

    private CycleResponse versReponse(Cycle cycle) {
        return CycleResponse.from(cycle, cotisationRepository.totalPayePourCycle(cycle.getId()));
    }
}
