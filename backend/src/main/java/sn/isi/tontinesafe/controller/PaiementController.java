package sn.isi.tontinesafe.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.isi.tontinesafe.dto.PaiementRequest;
import sn.isi.tontinesafe.dto.PaiementResponse;
import sn.isi.tontinesafe.dto.RecuResponse;
import sn.isi.tontinesafe.service.PaiementService;
import sn.isi.tontinesafe.service.RecuService;

import java.util.List;

@RestController
@RequestMapping("/api/paiements")
public class PaiementController {

    private final PaiementService paiementService;
    private final RecuService recuService;

    public PaiementController(PaiementService paiementService, RecuService recuService) {
        this.paiementService = paiementService;
        this.recuService = recuService;
    }

    @GetMapping
    public List<PaiementResponse> lister(@RequestParam(required = false) String statut) {
        return (statut != null) ? paiementService.listerParStatut(statut)
                                : paiementService.lister();
    }

    @GetMapping("/{id}")
    public PaiementResponse obtenir(@PathVariable Long id) {
        return paiementService.obtenir(id);
    }

    @GetMapping("/{id}/recu")
    public RecuResponse obtenirRecu(@PathVariable Long id) {
        return recuService.obtenirParPaiement(id);
    }

    /** Endpoint metier : initier un paiement (Orange Money / Wave). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaiementResponse initier(@Valid @RequestBody PaiementRequest req) {
        return paiementService.initier(req);
    }

    /** Endpoint metier : confirmer le paiement, solder la cotisation et emettre le recu. */
    @PatchMapping("/{id}/confirmer")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','GESTIONNAIRE')")
    public PaiementResponse confirmer(@PathVariable Long id) {
        return paiementService.confirmer(id);
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','GESTIONNAIRE')")
    public PaiementResponse annuler(@PathVariable Long id) {
        return paiementService.annuler(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        paiementService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
