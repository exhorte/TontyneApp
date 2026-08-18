package sn.isi.tontyn.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.isi.tontyn.dto.UtilisateurResponse;
import sn.isi.tontyn.exception.RessourceIntrouvableException;
import sn.isi.tontyn.repository.UtilisateurRepository;
import sn.isi.tontyn.util.Telephone;

import java.util.List;

/**
 * Annuaire des comptes de la plateforme.
 * Accessible a tout compte authentifie : sert a designer les personnes
 * a inviter dans une tontine.
 */
@RestController
@RequestMapping("/api/utilisateurs")
// Consultation ouverte a tout compte authentifie : necessaire pour designer
// les personnes a inviter dans une tontine.
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

    /**
     * Recherche ciblee par numero de telephone : remplace l'annuaire complet
     * pour designer la personne a ajouter a une tontine. Ne renvoie que le
     * compte correspondant exactement au numero fourni, jamais une liste.
     */
    @GetMapping("/recherche")
    public UtilisateurResponse rechercherParTelephone(@RequestParam String telephone) {
        String normalise = Telephone.normaliser(telephone);
        if (!Telephone.estValide(normalise)) {
            throw new IllegalArgumentException("Numero de telephone invalide.");
        }
        return utilisateurRepository.findByTelephone(normalise)
                .map(UtilisateurResponse::from)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "Aucun compte n'est inscrit sur Tontyn avec ce numero de telephone."));
    }
}
