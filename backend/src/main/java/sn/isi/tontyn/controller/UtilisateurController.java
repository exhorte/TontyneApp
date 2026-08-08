package sn.isi.tontyn.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.isi.tontyn.dto.UtilisateurResponse;
import sn.isi.tontyn.repository.UtilisateurRepository;

import java.util.List;

/**
 * Annuaire des comptes de la plateforme.
 * Accessible a tout compte authentifie : sert a designer les personnes
 * a inviter dans une tontine.
 */
@RestController
@RequestMapping("/api/utilisateurs")
// Consultation ouverte a tout compte authentifie : necessaire pour designer
// les personnes a inviter dans une tontine. Une recherche ciblee par adresse
// electronique remplacera avantageusement cette liste complete.
@PreAuthorize("isAuthenticated()")
public class UtilisateurController {

    private final UtilisateurRepository utilisateurRepository;

    public UtilisateurController(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    @GetMapping
    public List<UtilisateurResponse> lister() {
        return utilisateurRepository.findAll().stream()
                .map(UtilisateurResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public UtilisateurResponse obtenir(@PathVariable Long id) {
        return utilisateurRepository.findById(id)
                .map(UtilisateurResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
    }
}
