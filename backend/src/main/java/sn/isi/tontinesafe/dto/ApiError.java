package sn.isi.tontinesafe.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/** Reponse d'erreur JSON uniforme renvoyee par le GlobalExceptionHandler. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(LocalDateTime horodatage,
                       int statut,
                       String erreur,
                       String message,
                       String chemin,
                       Map<String, String> champs) {

    public static ApiError of(int statut, String erreur, String message, String chemin) {
        return new ApiError(LocalDateTime.now(), statut, erreur, message, chemin, null);
    }

    public static ApiError validation(int statut, String erreur, String message,
                                      String chemin, Map<String, String> champs) {
        return new ApiError(LocalDateTime.now(), statut, erreur, message, chemin, champs);
    }
}
