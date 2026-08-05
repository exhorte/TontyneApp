package sn.isi.tontyn.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.isi.tontyn.dto.*;
import sn.isi.tontyn.service.CycleService;
import sn.isi.tontyn.service.MembreService;
import sn.isi.tontyn.service.TontineService;

import java.util.List;

@RestController
@RequestMapping("/api/tontines")
public class TontineController {

    private final TontineService tontineService;
    private final MembreService membreService;
    private final CycleService cycleService;

    public TontineController(TontineService tontineService,
                             MembreService membreService,
                             CycleService cycleService) {
        this.tontineService = tontineService;
        this.membreService = membreService;
        this.cycleService = cycleService;
    }

    @GetMapping
    public List<TontineResponse> lister(@RequestParam(required = false) String statut) {
        return (statut != null) ? tontineService.listerParStatut(statut) : tontineService.lister();
    }

    @GetMapping("/{id}")
    public TontineResponse obtenir(@PathVariable Long id) {
        return tontineService.obtenir(id);
    }

    @GetMapping("/utilisateur/{utilisateurId}")
    public List<TontineResponse> listerParUtilisateur(@PathVariable Long utilisateurId) {
        return tontineService.listerParUtilisateur(utilisateurId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','GESTIONNAIRE')")
    @ResponseStatus(HttpStatus.CREATED)
    public TontineResponse creer(@Valid @RequestBody TontineRequest req) {
        return tontineService.creer(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','GESTIONNAIRE')")
    public TontineResponse modifier(@PathVariable Long id,
                                    @Valid @RequestBody TontineRequest req) {
        return tontineService.modifier(id, req);
    }

    @PatchMapping("/{id}/cloturer")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','GESTIONNAIRE')")
    public TontineResponse cloturer(@PathVariable Long id) {
        return tontineService.cloturer(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        tontineService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    // --- Sous-ressources ---

    @GetMapping("/{id}/membres")
    public List<MembreResponse> listerMembres(@PathVariable Long id) {
        return membreService.listerParTontine(id);
    }

    /** Endpoint metier : ajouter un membre a une tontine. */
    @PostMapping("/{id}/membres")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','GESTIONNAIRE')")
    @ResponseStatus(HttpStatus.CREATED)
    public MembreResponse ajouterMembre(@PathVariable Long id,
                                        @Valid @RequestBody AjoutMembreRequest req) {
        return membreService.ajouterATontine(id, req);
    }

    @GetMapping("/{id}/cycles")
    public List<CycleResponse> listerCycles(@PathVariable Long id) {
        return cycleService.listerParTontine(id);
    }

    /** Endpoint metier : generer automatiquement les cycles de la tontine. */
    @PostMapping("/{id}/cycles/generer")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','GESTIONNAIRE')")
    @ResponseStatus(HttpStatus.CREATED)
    public List<CycleResponse> genererCycles(@PathVariable Long id,
                                             @Valid @RequestBody GenerationCyclesRequest req) {
        return cycleService.genererCycles(id, req);
    }
}
