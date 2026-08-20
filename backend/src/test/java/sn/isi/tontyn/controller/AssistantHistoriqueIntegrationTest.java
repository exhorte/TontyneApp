package sn.isi.tontyn.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import sn.isi.tontyn.model.Role;
import sn.isi.tontyn.model.Utilisateur;
import sn.isi.tontyn.repository.UtilisateurRepository;
import sn.isi.tontyn.security.JwtService;
import sn.isi.tontyn.service.ClientDeepSeek;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Bout en bout, avec de vrais jetons JWT et une vraie base : verifie qu'un
 * membre authentifie ne peut recuperer que son propre historique de
 * conversation avec l'assistant, jamais celui d'un autre (CU-06).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssistantHistoriqueIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private ClientDeepSeek clientDeepSeek;

    @Test
    void unUtilisateurNeRecupereQueSonPropreHistorique() throws Exception {
        when(clientDeepSeek.repondre(any(), any())).thenReturn("Reponse simulee de l'assistant.");

        Utilisateur alice = creerUtilisateur("+221701112222", "Alice", "Diallo");
        Utilisateur bob = creerUtilisateur("+221703334444", "Bob", "Sarr");

        String tokenAlice = jeton(alice);
        String tokenBob = jeton(bob);

        // Alice pose une question : un echange (question + reponse) doit etre
        // persiste dans SON historique a elle.
        mockMvc.perform(post("/api/assistant/question")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Bonjour, ou en est ma tontine ?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(true));

        // Bob n'a jamais rien demande : son historique est vide, et surtout
        // il ne recupere jamais celui d'Alice en interrogeant SON propre jeton.
        mockMvc.perform(get("/api/assistant/historique")
                        .header("Authorization", "Bearer " + tokenBob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Alice, elle, retrouve bien ses deux messages (question + reponse).
        mockMvc.perform(get("/api/assistant/historique")
                        .header("Authorization", "Bearer " + tokenAlice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].auteur").value("UTILISATEUR"))
                .andExpect(jsonPath("$[0].contenu").value("Bonjour, ou en est ma tontine ?"))
                .andExpect(jsonPath("$[1].auteur").value("ASSISTANT"))
                .andExpect(jsonPath("$[1].contenu").value("Reponse simulee de l'assistant."));

        // Sans jeton, l'acces est refuse (authentification requise, pas de
        // notion d'historique "public").
        mockMvc.perform(get("/api/assistant/historique"))
                .andExpect(status().isUnauthorized());
    }

    private Utilisateur creerUtilisateur(String telephone, String prenom, String nom) {
        Utilisateur u = new Utilisateur();
        u.setTelephone(telephone);
        u.setPrenom(prenom);
        u.setNom(nom);
        u.setCodePin(passwordEncoder.encode("1234"));
        u.setRole(Role.MEMBRE);
        return utilisateurRepository.save(u);
    }

    private String jeton(Utilisateur u) {
        return jwtService.generateToken(u.getId(), u.getTelephone(), u.getRole().name());
    }
}
