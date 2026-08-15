package sn.isi.tontyn.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TontineRequest(
        @NotBlank(message = "Le nom de la tontine est obligatoire.")
        @Size(max = 100, message = "Le nom ne doit pas depasser 100 caracteres.")
        String nom,

        @Size(max = 500, message = "La description ne doit pas depasser 500 caracteres.")
        String description,

        @Positive(message = "Le montant de cotisation doit etre strictement positif.")
        double montantCotisation,

        @NotBlank(message = "La periodicite est obligatoire.")
        @Pattern(regexp = "QUOTIDIENNE|HEBDOMADAIRE|BIMENSUELLE|MENSUELLE|TRIMESTRIELLE",
                 message = "Periodicite attendue : QUOTIDIENNE, HEBDOMADAIRE, BIMENSUELLE, "
                         + "MENSUELLE ou TRIMESTRIELLE.")
        String periodicite,

        @Min(value = 2, message = "Une tontine compte au minimum 2 membres.")
        int nombreMembres,

        @Pattern(regexp = "ACTIVE|SUSPENDUE|CLOTUREE",
                 message = "Statut attendu : ACTIVE, SUSPENDUE ou CLOTUREE.")
        String statut,

        /**
         * Date de demarrage effectif. Facultative : en son absence, la tontine
         * demarre le jour de sa creation.
         */
        LocalDate dateDebut,

        /**
         * Penalite de retard, en pourcentage du montant de la cotisation.
         * Facultative : absente ou nulle, la tontine ne sanctionne pas.
         * Le plafond de 50 % evite qu'une saisie erronee ne produise une
         * penalite superieure a la cotisation elle-meme.
         */
        @PositiveOrZero(message = "Le taux de penalite ne peut pas etre negatif.")
        @DecimalMax(value = "50.0", message = "Le taux de penalite ne peut pas depasser 50 %.")
        Double tauxPenalite
) {}
