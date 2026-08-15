package sn.isi.tontyn.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sn.isi.tontyn.exception.ConflitMetierException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Depot des images de verification.
 *
 * <p>Les fichiers ne sont jamais conserves en base de donnees, ni exposes par
 * une route publique : ils sont ecrits dans un repertoire configure, sous un
 * nom aleatoire sans rapport avec l'identite de leur titulaire. La base ne
 * retient qu'un nom de fichier, et seul l'administrateur de la plateforme peut
 * en obtenir le contenu, par un point d'entree protege.</p>
 */
@Service
public class DepotFichierService {

    private static final Logger log = LoggerFactory.getLogger(DepotFichierService.class);

    /** Formats acceptes : ceux que produisent les appareils photographiques. */
    private static final Set<String> TYPES_ACCEPTES = Set.of("image/jpeg", "image/jpg", "image/png");

    /** Une photographie de piece depasse rarement ce poids. */
    private static final long TAILLE_MAX = 8L * 1024 * 1024;

    /** En deca, l'image est trop petite pour etre exploitable. */
    private static final long TAILLE_MIN = 20L * 1024;

    @Value("${app.verification.repertoire:./donnees/pieces}")
    private String repertoire;

    /**
     * Enregistre une image et renvoie son nom de stockage.
     *
     * @param fichier image transmise par le client
     * @param role    role fonctionnel, a seule fin de journalisation
     */
    public String deposer(MultipartFile fichier, String role) {
        controler(fichier, role);
        try {
            Path dossier = Paths.get(repertoire).toAbsolutePath().normalize();
            Files.createDirectories(dossier);

            String nom = UUID.randomUUID() + extension(fichier);
            Path cible = dossier.resolve(nom);
            fichier.transferTo(cible.toFile());

            log.info("Image de verification deposee ({}), {} octets", role, fichier.getSize());
            return nom;
        } catch (IOException e) {
            log.error("Echec du depot de l'image {} : {}", role, e.getMessage());
            throw new ConflitMetierException("L'enregistrement de l'image a échoué. Réessayez.");
        }
    }

    /** Lit une image deposee. Reserve a l'instruction. */
    public byte[] lire(String nomFichier) {
        if (nomFichier == null || nomFichier.isBlank()) {
            throw new ConflitMetierException("Aucune image associée à cette demande.");
        }
        try {
            Path chemin = cheminSur(nomFichier);
            if (!Files.exists(chemin)) {
                throw new ConflitMetierException("Cette image n'est plus disponible.");
            }
            return Files.readAllBytes(chemin);
        } catch (IOException e) {
            throw new ConflitMetierException("La lecture de l'image a échoué.");
        }
    }

    /** Supprime une image. Employe par la purge, et en cas d'echec de soumission. */
    public void supprimer(String nomFichier) {
        if (nomFichier == null || nomFichier.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(cheminSur(nomFichier));
        } catch (IOException e) {
            log.warn("Suppression impossible pour {} : {}", nomFichier, e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    //  Controles
    // ------------------------------------------------------------------

    private void controler(MultipartFile fichier, String role) {
        if (fichier == null || fichier.isEmpty()) {
            throw new ConflitMetierException("L'image « " + role + " » est manquante.");
        }
        String type = fichier.getContentType();
        if (type == null || !TYPES_ACCEPTES.contains(type.toLowerCase(Locale.ROOT))) {
            throw new ConflitMetierException(
                    "L'image « " + role + " » doit être au format JPEG ou PNG.");
        }
        if (fichier.getSize() > TAILLE_MAX) {
            throw new ConflitMetierException(
                    "L'image « " + role + " » dépasse 8 Mo. Réduisez sa définition.");
        }
        if (fichier.getSize() < TAILLE_MIN) {
            throw new ConflitMetierException(
                    "L'image « " + role + " » est trop petite pour être exploitable. "
                            + "Rapprochez-vous du document et évitez le flou.");
        }
    }

    /**
     * Resout un nom de fichier a l'interieur du repertoire de depot, en
     * refusant toute tentative d'en sortir.
     */
    private Path cheminSur(String nomFichier) {
        Path dossier = Paths.get(repertoire).toAbsolutePath().normalize();
        Path cible = dossier.resolve(nomFichier).normalize();
        if (!cible.startsWith(dossier)) {
            throw new ConflitMetierException("Nom de fichier invalide.");
        }
        return cible;
    }

    private String extension(MultipartFile fichier) {
        String type = fichier.getContentType();
        return (type != null && type.toLowerCase(Locale.ROOT).contains("png")) ? ".png" : ".jpg";
    }
}
