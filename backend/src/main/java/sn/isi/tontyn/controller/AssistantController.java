package sn.isi.tontyn.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.isi.tontyn.dto.AssistantQuestionRequest;
import sn.isi.tontyn.dto.AssistantResponse;
import sn.isi.tontyn.dto.MessageConversationResponse;
import sn.isi.tontyn.service.AssistantService;

import java.util.List;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    /**
     * Question en langage naturel sur la situation du membre authentifie.
     *
     * <p>Aucun parametre d'identite n'est accepte ici : la portee de la
     * reponse est exclusivement celle de l'utilisateur du jeton JWT (voir
     * {@code AssistantService}). Pas de {@code @PreAuthorize} particulier :
     * la regle globale {@code anyRequest().authenticated()} de
     * {@code SecurityConfig} suffit, puisque l'identite ne peut venir que du
     * jeton.</p>
     */
    @PostMapping("/question")
    public AssistantResponse question(@RequestBody AssistantQuestionRequest requete) {
        return assistantService.repondre(requete);
    }

    /**
     * Historique de conversation avec l'assistant.
     *
     * <p>Meme principe que ci-dessus : aucun identifiant n'est accepte en
     * parametre ou en chemin. La reponse ne peut donc jamais porter que sur
     * l'utilisateur du jeton JWT — ni un autre membre, ni, pour un
     * gestionnaire, l'un de ses membres.</p>
     */
    @GetMapping("/historique")
    public List<MessageConversationResponse> historique() {
        return assistantService.historique();
    }
}
