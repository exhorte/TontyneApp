import 'package:flutter/material.dart';
import '../theme/app_theme.dart';
import '../utils/format.dart';

/// Badge en pilule, coloré selon le statut — equivalent du composant web.
class BadgeStatut extends StatelessWidget {
  final String? statut;
  const BadgeStatut(this.statut, {super.key});

  @override
  Widget build(BuildContext context) {
    late Color fond, texte;
    switch (statut) {
      case 'ACTIVE': case 'ACTIF': case 'PAYEE': case 'CONFIRME': case 'LUE':
        fond = Jetons.vertFond; texte = Jetons.vert; break;
      case 'EN_ATTENTE': case 'PLANIFIE': case 'INITIE': case 'NON_LUE':
        fond = Jetons.ambreFond; texte = Jetons.ambre; break;
      case 'ANNULE': case 'SUSPENDU': case 'SUSPENDUE':
        fond = Jetons.rougeFond; texte = Jetons.rouge; break;
      case 'EN_COURS':
        fond = Jetons.bleuFond; texte = Jetons.bleu; break;
      default:
        fond = Jetons.surface; texte = Jetons.texteSecondaire;
    }
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
          color: fond, borderRadius: BorderRadius.circular(Jetons.rPastille)),
      child: Row(mainAxisSize: MainAxisSize.min, children: [
        Container(width: 6, height: 6,
            decoration: BoxDecoration(color: texte, shape: BoxShape.circle)),
        const SizedBox(width: 6),
        Text(libelleStatut(statut),
            style: TextStyle(fontSize: 12, fontWeight: FontWeight.w500, color: texte)),
      ]),
    );
  }
}

/// Carte de statistique (libelle en petites capitales + grand chiffre).
class CarteStat extends StatelessWidget {
  final String libelle, valeur;
  final String? detail;
  const CarteStat({super.key, required this.libelle, required this.valeur, this.detail});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(Jetons.e4),
      decoration: BoxDecoration(
        border: Border.all(color: Jetons.bordure),
        borderRadius: BorderRadius.circular(Jetons.rCarte),
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(libelle.toUpperCase(), style: Theme.of(context).textTheme.labelSmall),
        const SizedBox(height: Jetons.e2),
        Text(valeur,
            style: const TextStyle(
                fontSize: 24, height: 28 / 24, fontWeight: FontWeight.w400,
                color: Jetons.encre)),
        if (detail != null) ...[
          const SizedBox(height: 2),
          Text(detail!, style: Theme.of(context).textTheme.bodySmall),
        ],
      ]),
    );
  }
}

/// Ligne de liste : libelle + sous-libelle a gauche, valeur a droite, chevron.
class LigneListe extends StatelessWidget {
  final String titre;
  final String? sousTitre, valeur, valeurSecondaire;
  final Widget? fin;
  final VoidCallback? onTap;
  final Widget? tete;
  const LigneListe({
    super.key, required this.titre, this.sousTitre, this.valeur,
    this.valeurSecondaire, this.fin, this.onTap, this.tete,
  });

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: Jetons.e4, vertical: 14),
        decoration: const BoxDecoration(
            border: Border(bottom: BorderSide(color: Jetons.ligne))),
        child: Row(children: [
          if (tete != null) ...[tete!, const SizedBox(width: Jetons.e3)],
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(titre,
                  style: t.bodyLarge!.copyWith(fontWeight: FontWeight.w500)),
              if (sousTitre != null) ...[
                const SizedBox(height: 2),
                Text(sousTitre!, style: t.bodySmall),
              ],
            ]),
          ),
          if (valeur != null || valeurSecondaire != null) ...[
            const SizedBox(width: Jetons.e2),
            Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
              if (valeur != null)
                Text(valeur!,
                    style: t.bodyLarge!.copyWith(fontWeight: FontWeight.w500)),
              if (valeurSecondaire != null) ...[
                const SizedBox(height: 2),
                Text(valeurSecondaire!, style: t.bodySmall),
              ],
            ]),
          ],
          if (fin != null) ...[const SizedBox(width: Jetons.e3), fin!],
          if (onTap != null) ...[
            const SizedBox(width: Jetons.e2),
            const Icon(Icons.chevron_right, size: 20, color: Jetons.texteTertiaire),
          ],
        ]),
      ),
    );
  }
}

/// Pastille ronde bleue contenant une icone blanche.
class PastilleIcone extends StatelessWidget {
  final IconData icone;
  final double taille;
  const PastilleIcone(this.icone, {super.key, this.taille = 38});
  @override
  Widget build(BuildContext context) => Container(
        width: taille, height: taille,
        decoration: const BoxDecoration(color: Jetons.bleu, shape: BoxShape.circle),
        child: Icon(icone, size: taille * .5, color: Jetons.blanc),
      );
}

/// Etat vide : cercle gris, icone, titre, texte et action facultative.
class EtatVide extends StatelessWidget {
  final IconData icone;
  final String titre;
  final String? texte;
  final Widget? action;
  const EtatVide({super.key, required this.icone, required this.titre,
      this.texte, this.action});

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: Jetons.e6, vertical: 48),
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Container(
            width: 60, height: 60,
            decoration: const BoxDecoration(color: Jetons.surface, shape: BoxShape.circle),
            child: Icon(icone, size: 26, color: Jetons.texteTertiaire),
          ),
          const SizedBox(height: Jetons.e4),
          Text(titre, textAlign: TextAlign.center,
              style: t.titleMedium),
          if (texte != null) ...[
            const SizedBox(height: Jetons.e1),
            Text(texte!, textAlign: TextAlign.center, style: t.bodySmall),
          ],
          if (action != null) ...[const SizedBox(height: Jetons.e5), action!],
        ]),
      ),
    );
  }
}

/// Bandeau d'information ou d'erreur.
class Bandeau extends StatelessWidget {
  final String message;
  final bool erreur;
  const Bandeau(this.message, {super.key, this.erreur = false});
  @override
  Widget build(BuildContext context) => Container(
        width: double.infinity,
        padding: const EdgeInsets.all(Jetons.e3),
        decoration: BoxDecoration(
          color: erreur ? Jetons.rougeFond : Jetons.surface,
          borderRadius: BorderRadius.circular(Jetons.rCarte),
        ),
        child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Icon(erreur ? Icons.error_outline : Icons.info_outline,
              size: 18, color: erreur ? Jetons.rouge : Jetons.texteSecondaire),
          const SizedBox(width: Jetons.e2),
          Expanded(
            child: Text(message,
                style: TextStyle(
                    fontSize: 13.5,
                    color: erreur ? Jetons.rouge : Jetons.encre)),
          ),
        ]),
      );
}

/// En-tete de section : titre en graisse 400 + lien facultatif.
class TitreSection extends StatelessWidget {
  final String titre;
  final String? actionTexte;
  final VoidCallback? onAction;
  const TitreSection(this.titre, {super.key, this.actionTexte, this.onAction});
  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.fromLTRB(Jetons.e4, Jetons.e5, Jetons.e2, Jetons.e2),
        child: Row(children: [
          Expanded(child: Text(titre, style: Theme.of(context).textTheme.titleLarge)),
          if (actionTexte != null)
            TextButton(onPressed: onAction, child: Text(actionTexte!)),
        ]),
      );
}

/// Indicateur de chargement discret.
class Chargement extends StatelessWidget {
  const Chargement({super.key});
  @override
  Widget build(BuildContext context) => const Center(
        child: Padding(
          padding: EdgeInsets.all(48),
          child: SizedBox(
              width: 26, height: 26,
              child: CircularProgressIndicator(strokeWidth: 2.2, color: Jetons.bleu)),
        ),
      );
}

/// Affichage d'erreur avec possibilite de reessayer.
class ErreurVue extends StatelessWidget {
  final String message;
  final VoidCallback? onReessayer;
  const ErreurVue(this.message, {super.key, this.onReessayer});
  @override
  Widget build(BuildContext context) => EtatVide(
        icone: Icons.error_outline,
        titre: 'Une erreur est survenue',
        texte: message,
        action: onReessayer == null
            ? null
            : OutlinedButton.icon(
                onPressed: onReessayer,
                icon: const Icon(Icons.refresh, size: 18),
                label: const Text('Réessayer')),
      );
}
