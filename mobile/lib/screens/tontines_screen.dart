import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/modeles.dart' as m;
import '../services/api_service.dart';
import '../services/auth_service.dart';
import '../services/ressources.dart';
import '../theme/app_theme.dart';
import '../utils/format.dart';
import '../widgets/communs.dart';
import 'tontine_detail_screen.dart';

class TontinesScreen extends StatefulWidget {
  const TontinesScreen({super.key});
  @override
  State<TontinesScreen> createState() => _TontinesScreenState();
}

class _TontinesScreenState extends State<TontinesScreen> {
  bool _chargement = true;
  String? _erreur, _filtre;
  List<m.Tontine> _tontines = [];

  @override
  void initState() { super.initState(); _charger(); }

  Future<void> _charger() async {
    setState(() { _chargement = true; _erreur = null; });
    try {
      final auth = context.read<AuthService>();
      final l = auth.peutGerer
          ? await Ressources.tontines(statut: _filtre)
          : await Ressources.tontinesDe(auth.utilisateur!.id);
      if (mounted) setState(() { _tontines = l; _chargement = false; });
    } on ErreurApi catch (e) {
      if (mounted) setState(() { _erreur = e.message; _chargement = false; });
    }
  }

  Future<void> _nouvelleTontine() async {
    final cree = await showModalBottomSheet<bool>(
      context: context, isScrollControlled: true,
      backgroundColor: Jetons.blanc,
      shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(Jetons.rModale))),
      builder: (_) => const _FormulaireTontine(),
    );
    if (cree == true) _charger();
  }

  @override
  Widget build(BuildContext context) {
    final peutGerer = context.read<AuthService>().peutGerer;
    return Scaffold(
      backgroundColor: Jetons.blanc,
      floatingActionButton: peutGerer
          ? FloatingActionButton.extended(
              onPressed: _nouvelleTontine,
              backgroundColor: Jetons.bleu,
              foregroundColor: Jetons.blanc,
              elevation: 0,
              icon: const Icon(Icons.add, size: 20),
              label: const Text('Nouvelle'))
          : null,
      body: _chargement
          ? const Chargement()
          : _erreur != null
              ? ErreurVue(_erreur!, onReessayer: _charger)
              : RefreshIndicator(
                  onRefresh: _charger,
                  color: Jetons.bleu,
                  child: _tontines.isEmpty
                      ? ListView(children: [
                          const SizedBox(height: 60),
                          EtatVide(
                            icone: Icons.groups_outlined,
                            titre: 'Aucune tontine',
                            texte: peutGerer
                                ? 'Créez une première tontine pour commencer.'
                                : 'Vous ne participez à aucune tontine.',
                            action: peutGerer
                                ? FilledButton.icon(
                                    onPressed: _nouvelleTontine,
                                    icon: const Icon(Icons.add, size: 18),
                                    label: const Text('Nouvelle tontine'))
                                : null,
                          ),
                        ])
                      : ListView(children: [
                          ..._tontines.map((x) => LigneListe(
                                titre: x.nom,
                                sousTitre:
                                    '${libellePeriodicite(x.periodicite)} · ${x.nombreMembresInscrits}/${x.nombreMembres} membres · ${x.nombreCycles} cycle(s)',
                                valeur: fcfa(x.montantCotisation),
                                fin: BadgeStatut(x.statut),
                                onTap: () => Navigator.push(context,
                                    MaterialPageRoute(
                                        builder: (_) => TontineDetailScreen(tontineId: x.id))
                                  ).then((_) => _charger()),
                              )),
                          const SizedBox(height: 80),
                        ]),
                ),
    );
  }
}

/// Formulaire de creation d'une tontine (feuille modale).
class _FormulaireTontine extends StatefulWidget {
  const _FormulaireTontine();
  @override
  State<_FormulaireTontine> createState() => _FormulaireTontineState();
}

class _FormulaireTontineState extends State<_FormulaireTontine> {
  final _nom = TextEditingController();
  final _description = TextEditingController();
  final _montant = TextEditingController(text: '25000');
  final _nombre = TextEditingController(text: '10');
  String _periodicite = 'MENSUELLE';
  bool _envoi = false;
  String? _erreur;

  @override
  void dispose() {
    _nom.dispose(); _description.dispose(); _montant.dispose(); _nombre.dispose();
    super.dispose();
  }

  Future<void> _creer() async {
    setState(() { _envoi = true; _erreur = null; });
    try {
      await Ressources.creerTontine({
        'nom': _nom.text.trim(),
        'description': _description.text.trim(),
        'montantCotisation': double.tryParse(_montant.text) ?? 0,
        'periodicite': _periodicite,
        'nombreMembres': int.tryParse(_nombre.text) ?? 0,
        'statut': 'ACTIVE',
      });
      if (mounted) Navigator.pop(context, true);
    } on ErreurApi catch (e) {
      if (mounted) setState(() { _erreur = e.message; _envoi = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
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
          Text('Nouvelle tontine', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: Jetons.e5),
          if (_erreur != null) ...[Bandeau(_erreur!, erreur: true), const SizedBox(height: Jetons.e4)],
          TextField(controller: _nom,
              decoration: const InputDecoration(labelText: 'Nom de la tontine')),
          const SizedBox(height: Jetons.e3),
          TextField(controller: _description, maxLines: 2,
              decoration: const InputDecoration(labelText: 'Description')),
          const SizedBox(height: Jetons.e3),
          Row(children: [
            Expanded(child: TextField(controller: _montant,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: 'Cotisation (FCFA)'))),
            const SizedBox(width: Jetons.e3),
            Expanded(child: TextField(controller: _nombre,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: 'Membres'))),
          ]),
          const SizedBox(height: Jetons.e3),
          DropdownButtonFormField<String>(
            initialValue: _periodicite,
            decoration: const InputDecoration(labelText: 'Périodicité'),
            items: const ['QUOTIDIENNE','HEBDOMADAIRE','BIMENSUELLE','MENSUELLE','TRIMESTRIELLE']
                .map((p) => DropdownMenuItem(value: p, child: Text(libellePeriodicite(p))))
                .toList(),
            onChanged: (v) => setState(() => _periodicite = v ?? 'MENSUELLE'),
          ),
          const SizedBox(height: Jetons.e5),
          FilledButton(
            onPressed: _envoi ? null : _creer,
            style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(48)),
            child: _envoi
                ? const SizedBox(width: 18, height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Jetons.blanc))
                : const Text('Créer la tontine'),
          ),
          const SizedBox(height: Jetons.e5),
        ]),
      ),
    );
  }
}
