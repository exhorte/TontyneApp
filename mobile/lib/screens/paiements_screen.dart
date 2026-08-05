import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/modeles.dart' as m;
import '../services/api_service.dart';
import '../services/auth_service.dart';
import '../services/ressources.dart';
import '../theme/app_theme.dart';
import '../utils/format.dart';
import '../widgets/communs.dart';
import 'recu_vue.dart';

/// Paiements Mobile Money : suivi, confirmation et acces au recu.
class PaiementsScreen extends StatefulWidget {
  const PaiementsScreen({super.key});
  @override
  State<PaiementsScreen> createState() => _PaiementsScreenState();
}

class _PaiementsScreenState extends State<PaiementsScreen> {
  bool _chargement = true;
  String? _erreur;
  List<m.Paiement> _paiements = [];

  @override
  void initState() { super.initState(); _charger(); }

  Future<void> _charger() async {
    setState(() { _chargement = true; _erreur = null; });
    try {
      final l = await Ressources.paiements();
      if (mounted) setState(() { _paiements = l; _chargement = false; });
    } on ErreurApi catch (e) {
      if (mounted) setState(() { _erreur = e.message; _chargement = false; });
    }
  }

  Future<void> _confirmer(m.Paiement p) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Confirmer le paiement'),
        content: Text(
            'Confirmer le paiement de ${fcfa(p.montant)} de ${p.membreNom ?? ''} ?\n\n'
            'La cotisation passera à « payée », un reçu sera émis et le membre '
            'sera notifié par e-mail.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false),
              child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(context, true),
              child: const Text('Confirmer')),
        ],
      ),
    );
    if (ok != true) return;
    try {
      await Ressources.confirmerPaiement(p.id);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
          content: Text('Paiement confirmé : le reçu est émis et le membre notifié.')));
      _charger();
    } on ErreurApi catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
      }
    }
  }

  Future<void> _voirRecu(m.Paiement p) async {
    try {
      final r = await Ressources.recuDuPaiement(p.id);
      if (!mounted || r == null) return;
      showDialog(context: context, builder: (_) => RecuVue(recu: r));
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
      backgroundColor: Jetons.blanc,
      body: _chargement
          ? const Chargement()
          : _erreur != null
              ? ErreurVue(_erreur!, onReessayer: _charger)
              : RefreshIndicator(
                  onRefresh: _charger, color: Jetons.bleu,
                  child: _paiements.isEmpty
                      ? ListView(children: const [
                          SizedBox(height: 60),
                          EtatVide(
                              icone: Icons.credit_card_outlined,
                              titre: 'Aucun paiement',
                              texte: 'Les règlements par Orange Money ou Wave apparaîtront ici.'),
                        ])
                      : ListView(children: [
                          const Padding(
                            padding: EdgeInsets.all(Jetons.e4),
                            child: Bandeau(
                                'Une cotisation est d\'abord enregistrée, puis un paiement est '
                                'initié. La confirmation par un gestionnaire solde la cotisation, '
                                'génère le reçu et notifie le membre.'),
                          ),
                          ..._paiements.map((p) => LigneListe(
                                titre: p.reference ?? 'Paiement',
                                sousTitre:
                                    '${p.membreNom ?? ''} · ${libelleMethode(p.methode)}\n${formaterDateHeure(p.date)}',
                                valeur: fcfa(p.montant),
                                fin: BadgeStatut(p.statut),
                                onTap: () {
                                  if (p.statut == 'CONFIRME') {
                                    _voirRecu(p);
                                  } else if (p.statut == 'INITIE' && peutGerer) {
                                    _confirmer(p);
                                  }
                                },
                              )),
                          const SizedBox(height: Jetons.e8),
                        ]),
                ),
    );
  }
}
