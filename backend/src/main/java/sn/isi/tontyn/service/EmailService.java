package sn.isi.tontyn.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Envoi des courriels applicatifs (code de verification a deux facteurs).
 *
 * <p>Le service fonctionne selon deux modes, choisis automatiquement :</p>
 * <ul>
 *   <li><b>Mode reel</b> : les variables d'environnement MAIL_USERNAME et
 *       MAIL_PASSWORD sont definies, le courriel part par SMTP ;</li>
 *   <li><b>Mode degrade</b> : elles sont absentes, le code est ecrit dans les
 *       journaux. Le parcours d'authentification reste identique, ce qui permet
 *       de developper et de demontrer l'application hors connexion.</li>
 * </ul>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:true}")
    private boolean mailActive;

    @Value("${spring.mail.username:}")
    private String identifiantSmtp;

    @Value("${app.mail.from:}")
    private String expediteur;

    @Value("${app.mail.expediteur:Tontyn}")
    private String libelleExpediteur;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** Vrai lorsque l'envoi SMTP est reellement possible. */
    private boolean envoiPossible() {
        return mailActive && identifiantSmtp != null && !identifiantSmtp.isBlank();
    }

    public void envoyerCodeOtp(String destinataire, String code) {
        String sujet = "Tontyn - Votre code de verification";
        if (!envoiPossible()) {
            log.info("[MAIL DESACTIVE] Destinataire={} | Sujet={} | Code={}",
                    destinataire, sujet, code);
            return;
        }
        envoyerHtml(destinataire, sujet, gabaritCodeOtp(code), "Votre code de verification est : "
                + code + " (valable 5 minutes).");
    }

    /** Envoi d'un message simple, conserve pour les notifications applicatives. */
    public void envoyer(String destinataire, String sujet, String contenu) {
        if (!envoiPossible()) {
            log.info("[MAIL DESACTIVE] Destinataire={} | Sujet={} | Contenu={}",
                    destinataire, sujet, contenu);
            return;
        }
        envoyerHtml(destinataire, sujet, gabaritSimple(sujet, contenu), contenu);
    }

    private void envoyerHtml(String destinataire, String sujet, String html, String repliTexte) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper aide = new MimeMessageHelper(
                    message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());
            aide.setTo(destinataire);
            aide.setSubject(sujet);
            aide.setText(repliTexte, html);
            if (expediteur != null && !expediteur.isBlank()) {
                try {
                    aide.setFrom(expediteur, libelleExpediteur);
                } catch (UnsupportedEncodingException e) {
                    aide.setFrom(expediteur);
                }
            }
            mailSender.send(message);
            log.info("Courriel envoye a {} (sujet : {})", destinataire, sujet);
        } catch (Exception e) {
            // Un echec d'envoi ne doit jamais interrompre le traitement metier :
            // le code reste journalise pour que l'utilisateur puisse poursuivre.
            log.warn("Echec de l'envoi du courriel a {} : {}", destinataire, e.getMessage());
            log.info("[REPLI JOURNAL] Destinataire={} | Contenu={}", destinataire, repliTexte);
        }
    }

    // --- Gabarits HTML -----------------------------------------------------

    private static final String BLEU = "#0052FF";
    private static final String ENCRE = "#0C0A08";
    private static final String BORDURE = "#D2CECB";
    private static final String SURFACE = "#F6F4F2";

    private String gabaritCodeOtp(String code) {
        return enveloppe(
                "<p style=\"margin:0 0 20px;font-size:14px;line-height:20px;color:" + ENCRE + ";\">"
                        + "Bonjour,</p>"
                        + "<p style=\"margin:0 0 24px;font-size:14px;line-height:20px;color:" + ENCRE + ";\">"
                        + "Voici le code a saisir pour finaliser votre connexion a Tontyn.</p>"
                        + "<div style=\"margin:0 0 24px;padding:20px;background:" + SURFACE + ";"
                        + "border:1px solid " + BORDURE + ";border-radius:8px;text-align:center;\">"
                        + "<span style=\"font-size:32px;letter-spacing:10px;font-weight:400;color:" + BLEU + ";\">"
                        + code + "</span></div>"
                        + "<p style=\"margin:0 0 8px;font-size:13px;line-height:20px;color:rgba(12,10,8,0.62);\">"
                        + "Ce code expire dans <strong style=\"font-weight:500;color:" + ENCRE + ";\">5 minutes</strong>.</p>"
                        + "<p style=\"margin:0;font-size:13px;line-height:20px;color:rgba(12,10,8,0.62);\">"
                        + "Si vous n'etes pas a l'origine de cette demande, ignorez ce message : "
                        + "aucune action ne sera effectuee sur votre compte.</p>");
    }

    private String gabaritSimple(String titre, String contenu) {
        return enveloppe(
                "<p style=\"margin:0 0 12px;font-size:16px;font-weight:400;color:" + ENCRE + ";\">"
                        + echapper(titre) + "</p>"
                        + "<p style=\"margin:0;font-size:14px;line-height:20px;color:" + ENCRE + ";\">"
                        + echapper(contenu) + "</p>");
    }

    private String enveloppe(String corps) {
        return "<!DOCTYPE html><html lang=\"fr\"><body style=\"margin:0;padding:32px 16px;"
                + "background:" + SURFACE + ";font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"100%\" style=\"max-width:520px;background:#ffffff;"
                + "border:1px solid " + BORDURE + ";border-radius:8px;\" cellpadding=\"0\" cellspacing=\"0\">"
                + "<tr><td style=\"padding:28px 32px 0;\">"
                + "<span style=\"font-size:20px;font-weight:400;color:" + BLEU + ";\">Tontyn</span>"
                + "</td></tr>"
                + "<tr><td style=\"padding:24px 32px 32px;\">" + corps + "</td></tr>"
                + "<tr><td style=\"padding:16px 32px 24px;border-top:1px solid #E9E5E2;\">"
                + "<p style=\"margin:0;font-size:12px;line-height:18px;color:rgba(12,10,8,0.62);\">"
                + "Tontyn &mdash; gestion numerique des tontines. Message automatique, merci de ne pas y repondre.</p>"
                + "</td></tr></table></td></tr></table></body></html>";
    }

    private String echapper(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
