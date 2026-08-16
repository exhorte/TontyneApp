package sn.isi.tontyn.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.isi.tontyn.service.TontineService;

/**
 * Endpoint temporaire de test : supprime une tontine directement via le
 * service, sans passer par {@code @secu.gereTontine}, pour nettoyer des
 * tontines de test dont le membre gestionnaire a deja ete retire (le
 * controleur normal refuse alors l'acces). A retirer une fois le nettoyage
 * termine.
 */
@RestController
@RequestMapping("/api/dev/nettoyage")
public class DevNettoyageController {

    private final TontineService tontineService;

    public DevNettoyageController(TontineService tontineService) {
        this.tontineService = tontineService;
    }

    @DeleteMapping("/tontines/{id}")
    public String supprimerTontine(@PathVariable Long id) {
        tontineService.supprimer(id);
        return "Tontine " + id + " supprimee.";
    }
}
