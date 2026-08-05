import 'package:flutter/material.dart';

import '../models/modeles.dart' as m;
import '../services/api_service.dart';
import '../services/ressources.dart';
import '../theme/app_theme.dart';
import '../utils/format.dart';
import '../widgets/communs.dart';

/// Initiation d'un paiement Mobile Money pour une cotisation donnee.
class PaiementForm extends StatefulWidget {
  final m.Cotisation cotisation;
  const PaiementForm({super.key, required this.cotisation});
  @override
  State<PaiementForm> createState() => _PaiementFormState();
}

class _PaiementFormState extends State<PaiementForm> {
  String _methode = 'WAVE';
  final _reference = TextEditingController();
  bool _envoi = false;
  String? _erreur;

  @override
  void dispose() { _reference.dispose(); super.dispose(); }

  Future<void> _initier() async {
    setState(() { _envoi = true; _erreur = null; });
    try {
      final corps = <String, dynamic>{
        'cotisationId': widget.cotisation.id,
        'methode': _methode,
      };
      if (_reference.text.trim().isNotEmpty) {
        corps['reference'] = _reference.text.trim();
      }
      await Ressources.initierPaiement(corps);
      if (mounted) Navigator.pop(context, true);
    } on ErreurApi catch (e) {
      if (mounted) setState(() { _erreur = e.message; _envoi = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = widget.cotisation;
    return Padding(
      padding: EdgeInsets.only(
          bottom: MediaQuery.of(context).viewInsets.bottom,
          left: Jetons.e5, right: Jetons.e5, top: Jetons.e5),
      child: SingleChildScrollView(
        child: Column(mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Center(child: Container(width: 36, height: 4,
              decoration: BoxDecoration(color: Jetons.bordure,
                  borderRadius: BorderRadius.circular(2)))),
          const SizedBox(height: Jetons.e5),
          Text('Initier un paiement', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: Jetons.e2),
          Text('Étape 2 du parcours : cotisation → paiement → reçu.',
              style: Theme.of(context).textTheme.bodySmall),
          const SizedBox(height: Jetons.e4),
          if (_erreur != null) ...[Bandeau(_erreur!, erreur: true), const SizedBox(height: Jetons.e4)],
          Bandeau('Montant à régler : ${fcfa(c.montant)} — ${c.membreNom ?? ''}, '
              '${c.tontineNom ?? ''}, cycle n°${c.cycleNumero}.'),
          const SizedBox(height: Jetons.e4),
          Text('Méthode de paiement',
              style: Theme.of(context).textTheme.bodySmall),
          const SizedBox(height: Jetons.e2),
          Row(children: [
            Expanded(child: _choixMethode('WAVE', 'Wave')),
            const SizedBox(width: Jetons.e3),
            Expanded(child: _choixMethode('ORANGE_MONEY', 'Orange Money')),
          ]),
          const SizedBox(height: Jetons.e3),
          TextField(controller: _reference,
              decoration: const InputDecoration(
                  labelText: "Référence de l'opérateur",
                  helperText: 'Facultatif : générée automatiquement si vide')),
          const SizedBox(height: Jetons.e5),
          FilledButton(
            onPressed: _envoi ? null : _initier,
            style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(48)),
            child: _envoi
                ? const SizedBox(width: 18, height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Jetons.blanc))
                : const Text('Initier le paiement'),
          ),
          const SizedBox(height: Jetons.e5),
        ]),
      ),
    );
  }

  Widget _choixMethode(String valeur, String libelle) {
    final actif = _methode == valeur;
    return InkWell(
      onTap: () => setState(() => _methode = valeur),
      borderRadius: BorderRadius.circular(Jetons.rBouton),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 14),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: actif ? Jetons.bleuFond : Jetons.blanc,
          border: Border.all(color: actif ? Jetons.bleu : Jetons.bordure),
          borderRadius: BorderRadius.circular(Jetons.rBouton),
        ),
        child: Text(libelle,
            style: TextStyle(
                fontSize: 14,
                fontWeight: actif ? FontWeight.w500 : FontWeight.w400,
                color: actif ? Jetons.bleu : Jetons.encre)),
      ),
    );
  }
}
