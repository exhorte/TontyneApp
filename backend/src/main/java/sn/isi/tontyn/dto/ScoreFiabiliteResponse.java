package sn.isi.tontyn.dto;

import java.util.List;

/**
 * Score de fiabilite de paiement d'un membre, et sa decomposition.
 *
 * <p>{@code score} vaut {@code null} tant qu'aucune cotisation n'a ete
 * <em>jugee</em> (payee ou en retard) pour ce membre : voir
 * {@link sn.isi.tontyn.service.ScoreFiabiliteService}. Un membre recent n'est
 * pas un mauvais payeur, il est inconnu, et lui attribuer un chiffre serait
 * trompeur. {@code niveauConfiance} et {@code nombreCotisationsObservees}
 * indiquent, quand le score existe, sur quelle base il a ete etabli : une
 * note de 50 sur deux cotisations ne vaut pas une note de 50 sur trente.</p>
 *
 * <p>{@code decomposition} rend le score explicable : chaque critere affiche
 * la valeur observee et le nombre de points qu'il a retires, pour qu'un
 * membre puisse comprendre precisement ce qui a pese sur sa note.</p>
 */
public record ScoreFiabiliteResponse(
        Long membreId,
        Integer score,
        String niveauConfiance,
        int nombreCotisationsObservees,
        long ancienneteJours,
        String explicationGlobale,
        List<CritereScore> decomposition) {

    /** Un critere du score : sa valeur brute observee, et son cout en points. */
    public record CritereScore(
            String code,
            String libelle,
            String valeurObservee,
            double pointsRetires,
            String explication) {}
}
