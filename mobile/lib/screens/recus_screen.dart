import 'package:flutter/material.dart';

import '../models/modeles.dart' as m;
import '../services/api_service.dart';
import '../services/ressources.dart';
import '../theme/app_theme.dart';
import '../utils/format.dart';
import '../widgets/communs.dart';
import 'recu_vue.dart';

/// Justificatifs de cotisation.
class RecusScreen extends StatefulWidget {
  const RecusScreen({super.key});
  @override
  State<RecusScreen> createState() => _RecusScreenState();
}

class _RecusScreenState extends State<RecusScreen> {
  bool _chargement = true;
  String? _erreur;
  List<m.Recu> _recus = [];

  @override
  void initState() { super.initState(); _charger(); }

  Future<void> _charger() async {
    setState(() { _chargement = true; _erreur = null; });
    try {
      final l = await Ressources.recus();
      if (mounted) setState(() { _recus = l; _chargement = false; });
    } on ErreurApi catch (e) {
      if (mounted) setState(() { _erreur = e.message; _chargement = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    final total = _recus.fold<double>(0, (s, r) => s + r.montant);
    return Scaffold(
      appBar: AppBar(title: const Text('Reçus')),
      body: _chargement
          ? const Chargement()
          : _erreur != null
              ? ErreurVue(_erreur!, onReessayer: _charger)
              : _recus.isEmpty
                  ? const EtatVide(
                      icone: Icons.receipt_long_outlined,
                      titre: 'Aucun reçu',
                      texte: 'Les reçus sont émis à la confirmation des paiements.')
                  : RefreshIndicator(
                      onRefresh: _charger, color: Jetons.bleu,
                      child: ListView(children: [
                        Padding(
                          padding: const EdgeInsets.all(Jetons.e4),
                          child: Text(
                              '${_recus.length} reçu(s) émis, pour un total de ${fcfa(total)}.',
                              style: Theme.of(context).textTheme.bodySmall),
                        ),
                        ..._recus.map((r) => LigneListe(
                              titre: r.numero ?? '—',
                              sousTitre:
                                  '${r.membreNom ?? ''}\n${r.tontineNom ?? ''} — cycle n°${r.cycleNumero}',
                              valeur: fcfa(r.montant),
                              valeurSecondaire: libelleMethode(r.methode),
                              onTap: () => showDialog(
                                  context: context, builder: (_) => RecuVue(recu: r)),
                            )),
                      ]),
                    ),
    );
  }
}
