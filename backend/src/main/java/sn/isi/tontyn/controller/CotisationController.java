package sn.isi.tontyn.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.isi.tontyn.dto.CotisationRequest;
import sn.isi.tontyn.dto.CotisationResponse;
import sn.isi.tontyn.dto.PaiementResponse;
import sn.isi.tontyn.service.CotisationService;
import sn.isi.tontyn.service.PaiementService;

import java.util.List;

@RestController
@RequestMapping("/api/cotisations")
public class CotisationController {

    private final CotisationService cotisationService;
    private final PaiementService paiementService;

    public CotisationController(CotisationService cotisationService,
                                PaiementService paiementService) {
        this.cotisationService = cotisationService;
        this.paiementService = paiementService;
    }

    @GetMapping
    public List<CotisationResponse> lister(@RequestParam(required = false) Long cycleId,
                                           @RequestParam(required = false) Long membreId,
                                           @RequestParam(required = false) Long tontineId) {
        if (cycleId != null)   return cotisationService.listerParCycle(cycleId);
        if (membreId != null)  return cotisationService.listerParMembre(membreId);
        if (tontineId != null) return cotisationService.listerParTontine(tontineId);
        return cotisationService.lister();
    }

    @GetMapping("/{id}")
    public CotisationResponse obtenir(@PathVariable Long id) {
        return cotisationService.obtenir(id);
    }

    @GetMapping("/{id}/paiement")
    public PaiementResponse obtenirPaiement(@PathVariable Long id) {
        return paiementService.obtenirParCotisation(id);
    }

    /** Endpoint metier : enregistrer une cotisation. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CotisationResponse enregistrer(@Valid @RequestBody CotisationRequest req) {
        return cotisationService.enregistrer(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@secu.gereCotisation(#id)")
    public CotisationResponse modifier(@PathVariable Long id,
                                       @Valid @RequestBody CotisationRequest req) {
        return cotisationService.modifier(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@secu.gereCotisation(#id)")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        cotisationService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
