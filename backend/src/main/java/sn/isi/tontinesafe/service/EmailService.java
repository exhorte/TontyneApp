package sn.isi.tontinesafe.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    /** Sur le profil h2 (app.mail.enabled=false), aucun SMTP n'est joignable :
     *  le message est alors ecrit dans les logs pour rester testable. */
    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void envoyerCodeOtp(String destinataire, String code) {
        envoyer(destinataire, "TontineSafe - Code de verification",
                "Votre code de verification est : " + code
                        + "\nIl expire dans 5 minutes.");
    }

    public void envoyer(String destinataire, String sujet, String contenu) {
        if (!mailEnabled) {
            log.info("[MAIL DESACTIVE] Destinataire={} | Sujet={} | Contenu={}",
                    destinataire, sujet, contenu);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(destinataire);
            message.setSubject(sujet);
            message.setText(contenu);
            mailSender.send(message);
        } catch (Exception e) {
            // L'echec d'envoi ne doit pas interrompre le traitement metier.
            log.warn("Echec de l'envoi de l'e-mail a {} : {}", destinataire, e.getMessage());
        }
    }
}
