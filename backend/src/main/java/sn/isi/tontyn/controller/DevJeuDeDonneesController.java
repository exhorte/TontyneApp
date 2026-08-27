package sn.isi.tontyn.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sn.isi.tontyn.service.DevJeuDeDonneesService;

/**
 * Endpoint temporaire, reserve a l'administrateur de la plateforme : efface
 * integralement les donnees applicatives et les remplace par un jeu de
 * demonstration coherent (tontines, membres, cycles, cotisations, paiements).
 * Sert a preparer les environnements de demonstration (soutenance de memoire
 * notamment). Destructeur et irreversible : a retirer une fois la periode de
 * demonstration terminee.
 */
@RestController
@RequestMapping("/api/dev/jeu-de-donnees")
public class DevJeuDeDonneesController {

    private final DevJeuDeDonneesService service;

    public DevJeuDeDonneesController(DevJeuDeDonneesService service) {
        this.service = service;
    }

    @PostMapping("/reinitialiser")
    @PreAuthorize("@secu.estAdministrateurPlateforme()")
    @ResponseStatus(HttpStatus.OK)
    public String reinitialiser() {
        return service.reinitialiser();
    }
}
