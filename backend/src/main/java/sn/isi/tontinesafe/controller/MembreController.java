package sn.isi.tontinesafe.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.isi.tontinesafe.dto.AjoutMembreRequest;
import sn.isi.tontinesafe.dto.CotisationResponse;
import sn.isi.tontinesafe.dto.MembreRequest;
import sn.isi.tontinesafe.dto.MembreResponse;
import sn.isi.tontinesafe.service.CotisationService;
import sn.isi.tontinesafe.service.MembreService;

import java.util.List;

@RestController
@RequestMapping("/api/membres")
public class MembreController {

    private final MembreService membreService;
    private final CotisationService cotisationService;

    public MembreController(MembreService membreService, CotisationService cotisationService) {
        this.membreService = membreService;
        this.cotisationService = cotisationService;
    }

    @GetMapping
    public List<MembreResponse> lister(@RequestParam(required = false) Long tontineId,
                                       @RequestParam(required = false) Long utilisateurId) {
        if (tontineId != null) {
            return membreService.listerParTontine(tontineId);
        }
        if (utilisateurId != null) {
            return membreService.listerParUtilisateur(utilisateurId);
        }
        return membreService.lister();
    }

    @GetMapping("/{id}")
    public MembreResponse obtenir(@PathVariable Long id) {
        return membreService.obtenir(id);
    }

    @GetMapping("/{id}/cotisations")
    public List<CotisationResponse> listerCotisations(@PathVariable Long id) {
        return cotisationService.listerParMembre(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','GESTIONNAIRE')")
    @ResponseStatus(HttpStatus.CREATED)
    public MembreResponse creer(@Valid @RequestBody MembreRequest req) {
        return membreService.creer(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','GESTIONNAIRE')")
    public MembreResponse modifier(@PathVariable Long id,
                                   @Valid @RequestBody AjoutMembreRequest req) {
        return membreService.modifier(id, req);
    }

    @PatchMapping("/{id}/suspendre")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','GESTIONNAIRE')")
    public MembreResponse suspendre(@PathVariable Long id) {
        return membreService.suspendre(id);
    }

    @PatchMapping("/{id}/reactiver")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','GESTIONNAIRE')")
    public MembreResponse reactiver(@PathVariable Long id) {
        return membreService.reactiver(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATEUR','GESTIONNAIRE')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        membreService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
