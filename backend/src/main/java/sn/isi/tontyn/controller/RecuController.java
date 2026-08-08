package sn.isi.tontyn.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.isi.tontyn.dto.RecuResponse;
import sn.isi.tontyn.service.RecuService;

import java.util.List;

@RestController
@RequestMapping("/api/recus")
public class RecuController {

    private final RecuService recuService;

    public RecuController(RecuService recuService) {
        this.recuService = recuService;
    }

    @GetMapping
    public List<RecuResponse> lister(@RequestParam(required = false) Long membreId) {
        return (membreId != null) ? recuService.listerParMembre(membreId) : recuService.lister();
    }

    @GetMapping("/{id}")
    public RecuResponse obtenir(@PathVariable Long id) {
        return recuService.obtenir(id);
    }

    @GetMapping("/paiement/{paiementId}")
    public RecuResponse obtenirParPaiement(@PathVariable Long paiementId) {
        return recuService.obtenirParPaiement(paiementId);
    }

    /** Endpoint metier : emettre le recu d'un paiement confirme. */
    @PostMapping("/paiement/{paiementId}")
    @PreAuthorize("@secu.gerePaiement(#paiementId)")
    @ResponseStatus(HttpStatus.CREATED)
    public RecuResponse generer(@PathVariable Long paiementId) {
        return recuService.genererPourPaiement(paiementId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@secu.gereRecu(#id)")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        recuService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
