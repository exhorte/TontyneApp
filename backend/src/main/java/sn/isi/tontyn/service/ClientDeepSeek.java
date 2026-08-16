package sn.isi.tontyn.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import sn.isi.tontyn.exception.AssistantIndisponibleException;

import java.util.List;

/**
 * Bordure HTTP vers l'API DeepSeek (compatible du format de l'API OpenAI).
 *
 * <p>Seule cette classe connait le prestataire : le reste de l'application ne
 * manipule que {@link AssistantService}, qui l'appelle avec un prompt systeme
 * et un message deja construits. Meme esprit que {@code EnvoiSmsService} pour
 * les SMS : une operation, un point de raccordement unique, pour que changer
 * de fournisseur ne touche qu'un seul fichier.</p>
 *
 * <p><strong>Rien de sensible n'est journalise ici.</strong> Ni la cle d'API
 * (utilisee uniquement dans l'en-tete {@code Authorization}, jamais dans un
 * message de journal), ni le contexte, ni la question de l'utilisateur : en
 * cas d'echec, seuls le statut de la reponse et le type d'erreur sont
 * consignes.</p>
 */
@Service
public class ClientDeepSeek {

    private static final Logger log = LoggerFactory.getLogger(ClientDeepSeek.class);

    private final RestClient restClient;

    @Value("${app.assistant.cle-api:}")
    private String cleApi;

    @Value("${app.assistant.modele:deepseek-chat}")
    private String modele;

    @Value("${app.assistant.max-tokens-reponse:300}")
    private int maxTokensReponse;

    public ClientDeepSeek(RestClient.Builder restClientBuilder,
                          @Value("${app.assistant.url:https://api.deepseek.com/chat/completions}")
                          String url,
                          @Value("${app.assistant.timeout-ms:15000}") int timeoutMs) {
        this.restClient = restClientBuilder
                .baseUrl(url)
                .requestFactory(fabriqueRequetes(timeoutMs))
                .build();
    }

    private static ClientHttpRequestFactory fabriqueRequetes(int timeoutMs) {
        SimpleClientHttpRequestFactory fabrique = new SimpleClientHttpRequestFactory();
        fabrique.setConnectTimeout(timeoutMs);
        fabrique.setReadTimeout(timeoutMs);
        return fabrique;
    }

    /**
     * Interroge le modele avec un prompt systeme et un message utilisateur deja
     * construits (contexte filtre + question). Renvoie le texte de la reponse,
     * jamais {@code null} : toute anomalie (reseau, statut HTTP, reponse vide
     * ou mal formee) leve {@link AssistantIndisponibleException}, a charge de
     * l'appelant de la transformer en message degrade.
     */
    public String repondre(String promptSysteme, String messageUtilisateur) {
        try {
            RequeteDeepSeek corps = new RequeteDeepSeek(modele, List.of(
                    new Message("system", promptSysteme),
                    new Message("user", messageUtilisateur)),
                    maxTokensReponse, 0.3);

            ReponseDeepSeek reponse = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + cleApi)
                    .body(corps)
                    .retrieve()
                    .body(ReponseDeepSeek.class);

            if (reponse == null || reponse.choices() == null || reponse.choices().isEmpty()) {
                throw new AssistantIndisponibleException("Reponse vide de DeepSeek.");
            }
            Message message = reponse.choices().get(0).message();
            String contenu = message != null ? message.content() : null;
            if (contenu == null || contenu.isBlank()) {
                throw new AssistantIndisponibleException("Contenu vide dans la reponse DeepSeek.");
            }
            return contenu.trim();
        } catch (AssistantIndisponibleException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("Appel DeepSeek en echec ({}) : {}", e.getClass().getSimpleName(), e.getMessage());
            throw new AssistantIndisponibleException("Appel DeepSeek en echec.", e);
        } catch (Exception e) {
            log.warn("Erreur inattendue lors de l'appel a DeepSeek : {}", e.getClass().getSimpleName());
            throw new AssistantIndisponibleException("Erreur inattendue lors de l'appel a DeepSeek.", e);
        }
    }

    // ------------------------------------------------------------------
    //  Format d'echange (API DeepSeek / compatible OpenAI Chat Completions)
    // ------------------------------------------------------------------

    record RequeteDeepSeek(String model, List<Message> messages,
                            @JsonProperty("max_tokens") int maxTokens,
                            double temperature) {}

    record Message(String role, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ReponseDeepSeek(List<Choix> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choix(Message message) {}
}
