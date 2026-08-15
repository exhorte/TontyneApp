import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/modeles.dart' as m;
import '../services/api_service.dart';
import '../services/auth_service.dart';
import '../services/ressources.dart';
import '../theme/app_theme.dart';
import '../widgets/communs.dart';

/// Adhesions des utilisateurs aux tontines.
class MembresScreen extends StatefulWidget {
  const MembresScreen({super.key});
  @override
  State<MembresScreen> createState() => _MembresScreenState();
}

class _MembresScreenState extends State<MembresScreen> {
  bool _chargement = true;
  String? _erreur;
  List<m.Membre> _membres = [];

  @override
  void initState() { super.initState(); _charger(); }

  Future<void> _charger() async {
    setState(() { _chargement = true; _erreur = null; });
    try {
      final auth = context.read<AuthService>();
      final l = auth.peutGerer
          ? await Ressources.membres()
          : await Ressources.membres(utilisateurId: auth.utilisateur!.id);
      if (mounted) setState(() { _membres = l; _chargement = false; });
    } on ErreurApi catch (e) {
      if (mounted) setState(() { _erreur = e.message; _chargement = false; });
    }
  }

  Future<void> _basculer(m.Membre mb) async {
    try {
      if (mb.statut == 'ACTIF') {
        await Ressources.suspendreMembre(mb.id);
      } else {
        await Ressources.reactiverMembre(mb.id);
      }
      _charger();
    } on ErreurApi catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final peutGerer = context.read<AuthService>().peutGerer;
    return Scaffold(
      appBar: AppBar(title: const Text('Membres')),
      body: _chargement
          ? const Chargement()
          : _erreur != null
              ? ErreurVue(_erreur!, onReessayer: _charger)
              : _membres.isEmpty
                  ? const EtatVide(
                      icone: Icons.person_outline,
                      titre: 'Aucun membre',
                      texte: "Aucune adhésion n'est enregistrée.")
                  : RefreshIndicator(
                      onRefresh: _charger, color: Jetons.bleu,
                      child: ListView(children: _membres.map((mb) => LigneListe(
                            tete: CircleAvatar(
                              radius: 16, backgroundColor: Jetons.bleuFond,
                              child: Text('${mb.ordreTour}',
                                  style: const TextStyle(
                                      fontSize: 12, color: Jetons.bleu,
                                      fontWeight: FontWeight.w600)),
                            ),
                            titre: mb.nomComplet ?? '—',
                            sousTitre: '${mb.tontineNom ?? ''}\n${mb.telephone ?? ''}',
                            fin: BadgeStatut(mb.statut),
                            onTap: peutGerer ? () => _basculer(mb) : null,
                          )).toList()),
                    ),
    );
  }
}
