package sn.isi.tontyn.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.isi.tontyn.dto.CotisationResponse;
import sn.isi.tontyn.dto.CycleRequest;
import sn.isi.tontyn.dto.CycleResponse;
import sn.isi.tontyn.service.CotisationService;
import sn.isi.tontyn.service.CycleService;

import java.util.List;

@RestController
@RequestMapping("/api/cycles")
public class CycleController {

    private final CycleService cycleService;
    private final CotisationService cotisationService;

    public CycleController(CycleService cycleService, CotisationService cotisationService) {
        this.cycleService = cycleService;
        this.cotisationService = cotisationService;
    }

    @GetMapping
    public List<CycleResponse> lister(@RequestParam(required = false) Long tontineId) {
        return (tontineId != null) ? cycleService.listerParTontine(tontineId)
                                   : cycleService.lister();
    }

    @GetMapping("/{id}")
    public CycleResponse obtenir(@PathVariable Long id) {
        return cycleService.obtenir(id);
    }

    @GetMapping("/{id}/cotisations")
    public List<CotisationResponse> listerCotisations(@PathVariable Long id) {
        return cotisationService.listerParCycle(id);
    }

    @PostMapping
    @PreAuthorize("@secu.gereTontine(#req.tontineId())")
    @ResponseStatus(HttpStatus.CREATED)
    public CycleResponse creer(@Valid @RequestBody CycleRequest req) {
        return cycleService.creer(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@secu.gereCycle(#id)")
    public CycleResponse modifier(@PathVariable Long id, @Valid @RequestBody CycleRequest req) {
        return cycleService.modifier(id, req);
    }

    @PatchMapping("/{id}/cloturer")
    @PreAuthorize("@secu.gereCycle(#id)")
    public CycleResponse cloturer(@PathVariable Long id) {
        return cycleService.cloturer(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@secu.gereCycle(#id)")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        cycleService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
