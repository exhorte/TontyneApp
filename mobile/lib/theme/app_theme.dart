import 'package:flutter/material.dart';

/// Jetons de conception repris du frontend web (inspiration Ramp) :
/// bleu unique en accent, neutres chauds, titres en graisse 400,
/// angles doux, aucune ombre.
class Jetons {
  // Accent
  static const bleu = Color(0xFF0052FF);
  static const bleuSurvol = Color(0xFF0043D1);
  static const bleuFond = Color(0xFFF0F4FF);

  // Neutres chauds
  static const encre = Color(0xFF0C0A08);
  static const texteSecondaire = Color(0x9E0C0A08); // 62 %
  static const texteTertiaire = Color(0x730C0A08); // 45 %
  static const bordure = Color(0xFFD2CECB);
  static const ligne = Color(0xFFE9E5E2);
  static const surface = Color(0xFFF6F4F2);
  static const blanc = Color(0xFFFFFFFF);

  // Etats
  static const vert = Color(0xFF098551);
  static const vertFond = Color(0xFFE6F5EF);
  static const rouge = Color(0xFFCF202F);
  static const rougeFond = Color(0xFFFDECEE);
  static const ambre = Color(0xFFB85C00);
  static const ambreFond = Color(0xFFFDF3E6);

  // Rayons
  static const rBouton = 6.0;
  static const rCarte = 8.0;
  static const rChamp = 6.0;
  static const rModale = 12.0;
  static const rPastille = 999.0;

  // Espacements
  static const e1 = 4.0, e2 = 8.0, e3 = 12.0, e4 = 16.0, e5 = 20.0, e6 = 24.0, e8 = 32.0;
}

class AppTheme {
  static ThemeData construire() {
    const police = 'Roboto';

    final base = ThemeData.light(useMaterial3: true);
    return base.copyWith(
      scaffoldBackgroundColor: Jetons.blanc,
      colorScheme: base.colorScheme.copyWith(
        primary: Jetons.bleu,
        onPrimary: Jetons.blanc,
        surface: Jetons.blanc,
        onSurface: Jetons.encre,
        error: Jetons.rouge,
      ),
      textTheme: base.textTheme.apply(fontFamily: police).copyWith(
            // Titres en graisse 400, jamais en gras
            headlineMedium: const TextStyle(
                fontSize: 28, height: 32 / 28, fontWeight: FontWeight.w400,
                letterSpacing: -0.3, color: Jetons.encre),
            titleLarge: const TextStyle(
                fontSize: 20, height: 24 / 20, fontWeight: FontWeight.w400,
                color: Jetons.encre),
            titleMedium: const TextStyle(
                fontSize: 16, height: 20 / 16, fontWeight: FontWeight.w500,
                color: Jetons.encre),
            bodyLarge: const TextStyle(
                fontSize: 15, height: 20 / 15, fontWeight: FontWeight.w400,
                color: Jetons.encre),
            bodyMedium: const TextStyle(
                fontSize: 14, height: 20 / 14, fontWeight: FontWeight.w400,
                color: Jetons.encre),
            bodySmall: const TextStyle(
                fontSize: 13, height: 18 / 13, fontWeight: FontWeight.w400,
                color: Jetons.texteSecondaire),
            labelSmall: const TextStyle(
                fontSize: 12, fontWeight: FontWeight.w500, letterSpacing: 0.4,
                color: Jetons.texteTertiaire),
          ),
      appBarTheme: const AppBarTheme(
        backgroundColor: Jetons.blanc,
        foregroundColor: Jetons.encre,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        titleTextStyle: TextStyle(
            fontSize: 18, fontWeight: FontWeight.w500, color: Jetons.encre),
        shape: Border(bottom: BorderSide(color: Jetons.ligne)),
      ),
      dividerTheme: const DividerThemeData(
          color: Jetons.ligne, thickness: 1, space: 1),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: Jetons.blanc,
        contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 14),
        hintStyle: const TextStyle(color: Jetons.texteTertiaire, fontSize: 14),
        border: _bord(Jetons.bordure),
        enabledBorder: _bord(Jetons.bordure),
        focusedBorder: _bord(Jetons.bleu, epaisseur: 1.6),
        errorBorder: _bord(Jetons.rouge),
        focusedErrorBorder: _bord(Jetons.rouge, epaisseur: 1.6),
        errorStyle: const TextStyle(fontSize: 12.5, color: Jetons.rouge),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: Jetons.bleu,
          foregroundColor: Jetons.blanc,
          minimumSize: const Size(0, 44),
          padding: const EdgeInsets.symmetric(horizontal: 16),
          elevation: 0,
          textStyle: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500),
          shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(Jetons.rBouton)),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: Jetons.encre,
          minimumSize: const Size(0, 44),
          side: const BorderSide(color: Jetons.bordure),
          textStyle: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500),
          shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(Jetons.rBouton)),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: Jetons.bleu,
          textStyle: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500),
        ),
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: Jetons.blanc,
        indicatorColor: Jetons.bleuFond,
        elevation: 0,
        height: 66,
        labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
        labelTextStyle: WidgetStateProperty.resolveWith(
          (s) => TextStyle(
            fontSize: 11.5,
            fontWeight: s.contains(WidgetState.selected)
                ? FontWeight.w500 : FontWeight.w400,
            color: s.contains(WidgetState.selected)
                ? Jetons.bleu : Jetons.texteSecondaire,
          ),
        ),
        iconTheme: WidgetStateProperty.resolveWith(
          (s) => IconThemeData(
            size: 22,
            color: s.contains(WidgetState.selected)
                ? Jetons.bleu : Jetons.texteSecondaire,
          ),
        ),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: Jetons.blanc,
        elevation: 0,
        shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(Jetons.rModale)),
        titleTextStyle: const TextStyle(
            fontSize: 18, fontWeight: FontWeight.w500, color: Jetons.encre),
      ),
      snackBarTheme: const SnackBarThemeData(
        backgroundColor: Jetons.encre,
        contentTextStyle: TextStyle(color: Jetons.blanc, fontSize: 14),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  static OutlineInputBorder _bord(Color c, {double epaisseur = 1}) =>
      OutlineInputBorder(
        borderRadius: BorderRadius.circular(Jetons.rChamp),
        borderSide: BorderSide(color: c, width: epaisseur),
      );
}
