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
                                           @RequestParam(required = false) Long tontineId,
                                           @RequestParam(required = false) String statut) {
        List<CotisationResponse> resultat;
        if (cycleId != null)        resultat = cotisationService.listerParCycle(cycleId);
        else if (membreId != null)  resultat = cotisationService.listerParMembre(membreId);
        else if (tontineId != null) resultat = cotisationService.listerParTontine(tontineId);
        else                        resultat = cotisationService.lister();

        // Filtre optionnel, utile au tableau de bord du gestionnaire :
        // GET /api/cotisations?tontineId=..&statut=EN_RETARD
        if (statut != null) {
            resultat = resultat.stream().filter(c -> statut.equals(c.statut())).toList();
        }
        return resultat;
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

    /**
     * Endpoint metier : leve la penalite de retard, au cas par cas.
     * Reserve au gestionnaire de la tontine concernee.
     */
    @PostMapping("/{id}/penalite/lever")
    @PreAuthorize("@secu.gereCotisation(#id)")
    public CotisationResponse leverPenalite(@PathVariable Long id) {
        return cotisationService.leverPenalite(id);
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
