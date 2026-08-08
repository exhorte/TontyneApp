package sn.isi.tontyn.model;

/**
 * Role global, valable au niveau de la plateforme.
 *
 * <p>La gestion d'une tontine n'est plus portee par un role global : elle
 * depend desormais du lien entre l'utilisateur et la tontine concernee,
 * exprime par l'attribut {@code roleGroupe} de l'entite {@code Membre}.
 * Tout utilisateur peut creer une tontine et en devient administrateur,
 * sans disposer pour autant du moindre droit sur les autres.</p>
 */
public enum Role {

    /** Exploitant de la plateforme : supervision et moderation de l'ensemble. */
    ADMINISTRATEUR,

    /** Utilisateur ordinaire : participe aux tontines et peut en creer. */
    MEMBRE
}
