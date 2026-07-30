package sn.isi.tontinesafe.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.isi.tontinesafe.dto.UtilisateurResponse;
import sn.isi.tontinesafe.repository.UtilisateurRepository;

import java.util.List;

/**
 * Annuaire des comptes de la plateforme.
 * Reserve aux roles ADMINISTRATEUR / GESTIONNAIRE (utilise pour l'ajout de membres).
 */
@RestController
@RequestMapping("/api/utilisateurs")
@PreAuthorize("hasAnyRole('ADMINISTRATEUR','GESTIONNAIRE')")
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
