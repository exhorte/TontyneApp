import 'package:flutter/material.dart';

import '../models/modeles.dart' as m;
import '../services/api_service.dart';
import '../services/ressources.dart';
import '../theme/app_theme.dart';
import '../utils/format.dart';
import '../widgets/communs.dart';

/// Tours de cotisation, toutes tontines confondues.
class CyclesScreen extends StatefulWidget {
  const CyclesScreen({super.key});
  @override
  State<CyclesScreen> createState() => _CyclesScreenState();
}

class _CyclesScreenState extends State<CyclesScreen> {
  bool _chargement = true;
  String? _erreur;
  List<m.Cycle> _cycles = [];

  @override
  void initState() { super.initState(); _charger(); }

  Future<void> _charger() async {
    setState(() { _chargement = true; _erreur = null; });
    try {
      final l = await Ressources.cycles();
      if (mounted) setState(() { _cycles = l; _chargement = false; });
    } on ErreurApi catch (e) {
      if (mounted) setState(() { _erreur = e.message; _chargement = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Cycles')),
      body: _chargement
          ? const Chargement()
          : _erreur != null
              ? ErreurVue(_erreur!, onReessayer: _charger)
              : _cycles.isEmpty
                  ? const EtatVide(
                      icone: Icons.autorenew,
                      titre: 'Aucun cycle',
                      texte: 'Générez les cycles depuis la fiche d\'une tontine.')
                  : RefreshIndicator(
                      onRefresh: _charger, color: Jetons.bleu,
                      child: ListView(children: _cycles.map((c) => LigneListe(
                            titre: '${c.tontineNom ?? 'Tontine'} — cycle n°${c.numero}',
                            sousTitre:
                                '${formaterDate(c.dateDebut)} → ${formaterDate(c.dateFin)}\nBénéficiaire : ${c.beneficiaireNom ?? '—'}',
                            valeur: fcfa(c.montantCollecte),
                            fin: BadgeStatut(c.statut),
                          )).toList()),
                    ),
    );
  }
}
