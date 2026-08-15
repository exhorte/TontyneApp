package sn.isi.tontyn.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sn.isi.tontyn.util.Telephone;

/**
 * Transmission des codes de verification par message court.
 *
 * <p>L'acces aux passerelles SMS des operateurs suppose un agrement commercial
 * hors de portee d'un projet academique. Le service est donc concu pour que le
 * raccordement effectif se reduise a une seule methode : tant que la propriete
 * {@code app.sms.fournisseur} vaut {@code JOURNAL}, le code est ecrit dans les
 * journaux applicatifs, ce qui laisse le parcours d'authentification
 * rigoureusement identique. Le jour ou une cle d'API est disponible, seule
 * l'implementation de {@link #transmettre} change.</p>
 */
@Service
public class EnvoiSmsService {

    private static final Logger log = LoggerFactory.getLogger(EnvoiSmsService.class);

    /** JOURNAL (defaut) ou le nom d'un fournisseur reel. */
    @Value("${app.sms.fournisseur:JOURNAL}")
    private String fournisseur;

    @Value("${app.sms.expediteur:Tontyn}")
    private String expediteur;

    @Value("${app.sms.cle-api:}")
    private String cleApi;

    /** Envoie le code de verification au numero indique. */
    public void envoyerCodeOtp(String telephone, String code) {
        String message = "Tontyn : votre code de verification est " + code
                + ". Il expire dans 5 minutes. Ne le communiquez a personne.";
        transmettre(telephone, message);
    }

    /** Envoie un message libre (rappel d'echeance, alerte). */
    public void envoyer(String telephone, String message) {
        transmettre(telephone, message);
    }

    /**
     * Point de raccordement unique a une passerelle reelle.
     *
     * <p>Une implementation de production effectuerait ici un appel HTTP vers
     * l'interface du fournisseur, en transmettant {@code cleApi} et
     * {@code expediteur}, puis controlerait le code de retour.</p>
     */
    private void transmettre(String telephone, String message) {
        if (telephone == null) {
            log.warn("Envoi de SMS impossible : numero absent.");
            return;
        }
        if (!"JOURNAL".equalsIgnoreCase(fournisseur) && !cleApi.isBlank()) {
            // Emplacement prevu pour l'appel a la passerelle de l'operateur.
            log.info("Envoi SMS via {} vers {}", fournisseur, Telephone.masquer(telephone));
            return;
        }
        log.info("[SMS SIMULE] Destinataire={} | Message={}", telephone, message);
    }

    /** Vrai lorsque les envois sont uniquement journalises. */
    public boolean estSimule() {
        return "JOURNAL".equalsIgnoreCase(fournisseur) || cleApi.isBlank();
    }
}
