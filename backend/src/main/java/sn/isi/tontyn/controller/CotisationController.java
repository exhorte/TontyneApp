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
    @PreAuthorize("(#cycleId != null and @secu.appartientTontineDuCycle(#cycleId)) or "
            + "(#membreId != null and @secu.peutConsulterMembre(#membreId)) or "
            + "(#tontineId != null and @secu.appartientTontine(#tontineId)) or "
            + "(#cycleId == null and #membreId == null and #tontineId == null)")
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
    @PreAuthorize("@secu.appartientTontineDuCotisation(#id)")
    public CotisationResponse obtenir(@PathVariable Long id) {
        return cotisationService.obtenir(id);
    }

    @GetMapping("/{id}/paiement")
    @PreAuthorize("@secu.appartientTontineDuCotisation(#id)")
    public PaiementResponse obtenirPaiement(@PathVariable Long id) {
        return paiementService.obtenirParCotisation(id);
    }

    /**
     * Endpoint metier : enregistrer une cotisation. Reserve au gestionnaire de
     * la tontine visee, ou au membre qui cotise pour son propre compte : ni
     * l'un ni l'autre ne peut enregistrer une cotisation au nom d'un tiers.
     */
    @PostMapping
    @PreAuthorize("@secu.peutCotiser(#req.membreId(), #req.cycleId())")
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
