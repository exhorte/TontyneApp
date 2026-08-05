import 'package:flutter/material.dart';

import '../models/modeles.dart' as m;
import '../theme/app_theme.dart';
import '../utils/format.dart';

/// Reçu numerique, reprenant la mise en page de la version web.
class RecuVue extends StatelessWidget {
  final m.Recu recu;
  const RecuVue({super.key, required this.recu});

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    return Dialog(
      insetPadding: const EdgeInsets.all(Jetons.e5),
      child: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(Jetons.e5),
          child: Column(mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start, children: [
            Row(children: [
              Text('Reçu de paiement', style: t.titleLarge),
              const Spacer(),
              IconButton(
                  onPressed: () => Navigator.pop(context),
                  icon: const Icon(Icons.close, size: 20),
                  style: IconButton.styleFrom(backgroundColor: Jetons.surface)),
            ]),
            const SizedBox(height: Jetons.e4),
            Container(
              padding: const EdgeInsets.all(Jetons.e4),
              decoration: BoxDecoration(
                border: Border.all(color: Jetons.bordure),
                borderRadius: BorderRadius.circular(Jetons.rCarte),
              ),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Container(
                    width: 28, height: 28,
                    decoration: BoxDecoration(
                        color: Jetons.bleu, borderRadius: BorderRadius.circular(7)),
                    alignment: Alignment.center,
                    child: const Text('T',
                        style: TextStyle(color: Jetons.blanc,
                            fontSize: 13, fontWeight: FontWeight.w700)),
                  ),
                  const SizedBox(width: Jetons.e2),
                  Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    const Text('Tontyn',
                        style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600)),
                    Text('Reçu de cotisation', style: t.bodySmall),
                  ]),
                  const Spacer(),
                  Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
                    Text(recu.numero ?? '',
                        style: const TextStyle(
                            fontSize: 13, fontWeight: FontWeight.w700,
                            color: Jetons.bleu, fontFamily: 'monospace')),
                    Text('Émis le ${formaterDateHeure(recu.dateEmission)}',
                        style: t.bodySmall!.copyWith(fontSize: 11)),
                  ]),
                ]),
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: Jetons.e3),
                  child: Divider(color: Jetons.bleu, thickness: 1.2),
                ),
                _ligne(context, 'Membre', recu.membreNom ?? '—'),
                _ligne(context, 'Tontine', recu.tontineNom ?? '—'),
                _ligne(context, 'Cycle', 'n°${recu.cycleNumero}'),
                _ligne(context, 'Méthode de paiement', libelleMethode(recu.methode)),
                _ligne(context, "Référence de l'opérateur", recu.referencePaiement ?? '—'),
                const SizedBox(height: Jetons.e2),
                const Divider(),
                const SizedBox(height: Jetons.e2),
                Row(children: [
                  const Text('Montant réglé',
                      style: TextStyle(
                          fontSize: 16, fontWeight: FontWeight.w600, color: Jetons.bleu)),
                  const Spacer(),
                  Text(fcfa(recu.montant),
                      style: const TextStyle(
                          fontSize: 16, fontWeight: FontWeight.w600, color: Jetons.bleu)),
                ]),
              ]),
            ),
            const SizedBox(height: Jetons.e4),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text('Fermer')),
            ),
          ]),
        ),
      ),
    );
  }

  Widget _ligne(BuildContext context, String libelle, String valeur) => Padding(
        padding: const EdgeInsets.only(bottom: Jetons.e3),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(libelle.toUpperCase(), style: Theme.of(context).textTheme.labelSmall),
          const SizedBox(height: 2),
          Text(valeur, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500)),
        ]),
      );
}
