package sn.isi.tontyn.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.isi.tontyn.security.SecuriteTontine;
import sn.isi.tontyn.service.DevJeuDeDonneesService;

/**
 * Endpoint temporaire : efface integralement les donnees applicatives et les
 * remplace par un jeu de demonstration coherent (tontines, membres, cycles,
 * cotisations, paiements). Sert a preparer les environnements de
 * demonstration (soutenance de memoire notamment). Destructeur et
 * irreversible : a retirer une fois la periode de demonstration terminee.
 *
 * <p>Accessible a l'administrateur de la plateforme, ou a quiconque presente
 * le jeton secret configure via {@code APP_RESET_TOKEN} (en-tete
 * {@code X-Reset-Token}) — utile juste apres un premier deploiement en
 * production, avant qu'aucun compte administrateur n'existe encore. Le jeton
 * est desactive (toute presentation refusee) tant qu'il n'est pas configure,
 * pour ne jamais laisser cet endpoint destructeur ouvert par defaut.</p>
 */
@RestController
@RequestMapping("/api/dev/jeu-de-donnees")
public class DevJeuDeDonneesController {

    private final DevJeuDeDonneesService service;
    private final SecuriteTontine securite;

    @Value("${app.reset.token:}")
    private String jetonAttendu;

    public DevJeuDeDonneesController(DevJeuDeDonneesService service, SecuriteTontine securite) {
        this.service = service;
        this.securite = securite;
    }

    @PostMapping("/reinitialiser")
    public ResponseEntity<String> reinitialiser(
            @RequestHeader(value = "X-Reset-Token", required = false) String jetonRecu) {
        boolean admin = securite.estAdministrateurPlateforme();
        boolean jetonValide = jetonAttendu != null && !jetonAttendu.isBlank()
                && jetonAttendu.equals(jetonRecu);
        if (!admin && !jetonValide) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Acces refuse : administrateur de la plateforme ou jeton "
                            + "X-Reset-Token valide requis.");
        }
        return ResponseEntity.ok(service.reinitialiser());
    }
}
