package sn.isi.tontyn.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Un message de l'historique de conversation avec l'assistant (CU-06) : soit
 * la question de l'utilisateur, soit la reponse du modele.
 *
 * <p>N'est jamais cree isolement : {@code AssistantService} enregistre
 * toujours les deux messages d'un echange ensemble, et uniquement apres un
 * appel reussi a DeepSeek — un echange en echec ne laisse aucune trace
 * partielle dans l'historique.</p>
 */
@Entity
@Table(name = "message_conversation")
@Getter @Setter @NoArgsConstructor
public class MessageConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuteurMessage auteur;

    @Column(nullable = false, length = 2000)
    private String contenu;

    @Column(nullable = false)
    private LocalDateTime horodatage = LocalDateTime.now();

    @ManyToOne(optional = false)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;
}
