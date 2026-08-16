package sn.isi.tontyn.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.isi.tontyn.service.RelanceService;

/**
 * Endpoint temporaire de test : declenche a la demande le job planifie de
 * detection des retards ({@code RelanceService.traiterRetards()}), qui ne
 * tourne normalement qu'une fois par jour a 8h. A retirer une fois la
 * verification manuelle terminee.
 */
@RestController
@RequestMapping("/api/dev/relance")
public class DevRelanceController {

    private final RelanceService relanceService;

    public DevRelanceController(RelanceService relanceService) {
        this.relanceService = relanceService;
    }

    @PostMapping("/declencher")
    public String declencher() {
        relanceService.traiterRetards();
        return "Traitement des retards execute.";
    }
}
