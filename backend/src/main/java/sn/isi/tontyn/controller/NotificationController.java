package sn.isi.tontyn.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.isi.tontyn.dto.NotificationRequest;
import sn.isi.tontyn.dto.NotificationResponse;
import sn.isi.tontyn.service.NotificationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public List<NotificationResponse> lister() {
        return notificationService.lister();
    }

    @GetMapping("/{id}")
    public NotificationResponse obtenir(@PathVariable Long id) {
        return notificationService.obtenir(id);
    }

    @GetMapping("/utilisateur/{utilisateurId}")
    public List<NotificationResponse> listerParUtilisateur(@PathVariable Long utilisateurId) {
        return notificationService.listerParUtilisateur(utilisateurId);
    }

    @GetMapping("/utilisateur/{utilisateurId}/non-lues")
    public Map<String, Long> compterNonLues(@PathVariable Long utilisateurId) {
        return Map.of("nonLues", notificationService.compterNonLues(utilisateurId));
    }

    /** Endpoint metier : envoyer une notification. */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse envoyer(@Valid @RequestBody NotificationRequest req) {
        return notificationService.envoyer(req);
    }

    @PatchMapping("/{id}/lue")
    public NotificationResponse marquerLue(@PathVariable Long id) {
        return notificationService.marquerLue(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        notificationService.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
