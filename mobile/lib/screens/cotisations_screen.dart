import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/modeles.dart' as m;
import '../services/api_service.dart';
import '../services/auth_service.dart';
import '../services/ressources.dart';
import '../theme/app_theme.dart';
import '../utils/format.dart';
import '../widgets/communs.dart';
import 'paiement_form.dart';

/// Cotisations : consultation, enregistrement et acces au reglement.
class CotisationsScreen extends StatefulWidget {
  const CotisationsScreen({super.key});
  @override
  State<CotisationsScreen> createState() => _CotisationsScreenState();
}

class _CotisationsScreenState extends State<CotisationsScreen> {
  bool _chargement = true;
  String? _erreur;
  List<m.Cotisation> _cotisations = [];

  @override
  void initState() { super.initState(); _charger(); }

  Future<void> _charger() async {
    setState(() { _chargement = true; _erreur = null; });
    try {
      final auth = context.read<AuthService>();
      List<m.Cotisation> l;
      if (auth.peutGerer) {
        l = await Ressources.cotisations();
      } else {
        l = [];
        for (final mb in await Ressources.membres(utilisateurId: auth.utilisateur!.id)) {
          l.addAll(await Ressources.cotisationsDuMembre(mb.id));
        }
      }
      if (mounted) setState(() { _cotisations = l; _chargement = false; });
    } on ErreurApi catch (e) {
      if (mounted) setState(() { _erreur = e.message; _chargement = false; });
    }
  }

  Future<void> _nouvelle() async {
    final ok = await showModalBottomSheet<bool>(
      context: context, isScrollControlled: true, backgroundColor: Jetons.blanc,
      shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(Jetons.rModale))),
      builder: (_) => const _FormulaireCotisation(),
    );
    if (ok == true) _charger();
  }

  Future<void> _payer(m.Cotisation c) async {
    final ok = await showModalBottomSheet<bool>(
      context: context, isScrollControlled: true, backgroundColor: Jetons.blanc,
      shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(Jetons.rModale))),
      builder: (_) => PaiementForm(cotisation: c),
    );
    if (ok == true) _charger();
  }

  @override
  Widget build(BuildContext context) {
    final enAttente = _cotisations.where((c) => c.statut != 'PAYEE').length;
    return Scaffold(
      backgroundColor: Jetons.blanc,
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _nouvelle,
        backgroundColor: Jetons.bleu, foregroundColor: Jetons.blanc, elevation: 0,
        icon: const Icon(Icons.add, size: 20), label: const Text('Cotiser'),
      ),
      body: _chargement
          ? const Chargement()
          : _erreur != null
              ? ErreurVue(_erreur!, onReessayer: _charger)
              : RefreshIndicator(
                  onRefresh: _charger, color: Jetons.bleu,
                  child: _cotisations.isEmpty
                      ? ListView(children: const [
                          SizedBox(height: 60),
                          EtatVide(
                              icone: Icons.savings_outlined,
                              titre: 'Aucune cotisation',
                              texte: 'Enregistrez une cotisation pour un cycle en cours.'),
                        ])
                      : ListView(children: [
                          Padding(
                            padding: const EdgeInsets.fromLTRB(
                                Jetons.e4, Jetons.e4, Jetons.e4, Jetons.e2),
                            child: Text(
                                '${_cotisations.length} cotisation(s), dont $enAttente en attente de règlement.',
                                style: Theme.of(context).textTheme.bodySmall),
                          ),
                          ..._cotisations.map((c) => LigneListe(
                                titre: c.membreNom ?? 'Cotisation',
                                sousTitre:
                                    '${c.tontineNom ?? ''} — cycle n°${c.cycleNumero}\n${formaterDateHeure(c.date)}',
                                valeur: fcfa(c.montant),
                                fin: BadgeStatut(c.statut),
                                onTap: c.statut == 'PAYEE' ? null : () => _payer(c),
                              )),
                          const SizedBox(height: 80),
                        ]),
                ),
    );
  }
}

/// Enregistrement d'une cotisation : tontine, cycle puis membre.
class _FormulaireCotisation extends StatefulWidget {
  const _FormulaireCotisation();
  @override
  State<_FormulaireCotisation> createState() => _FormulaireCotisationState();
}

class _FormulaireCotisationState extends State<_FormulaireCotisation> {
  List<m.Tontine> _tontines = [];
  List<m.Cycle> _cycles = [];
  List<m.Membre> _membres = [];
  int? _tontineId, _cycleId, _membreId;
  final _montant = TextEditingController();
  bool _chargement = true, _envoi = false;
  String? _erreur;

  @override
  void initState() { super.initState(); _charger(); }

  @override
  void dispose() { _montant.dispose(); super.dispose(); }

  Future<void> _charger() async {
    try {
      final auth = context.read<AuthService>();
      final l = auth.peutGerer
          ? await Ressources.tontines()
          : await Ressources.tontinesDe(auth.utilisateur!.id);
      if (mounted) setState(() { _tontines = l; _chargement = false; });
    } on ErreurApi catch (e) {
      if (mounted) setState(() { _erreur = e.message; _chargement = false; });
    }
  }

  Future<void> _surTontine(int? id) async {
    setState(() { _tontineId = id; _cycleId = null; _membreId = null; });
    if (id == null) return;
    try {
      final cy = await Ressources.cyclesDeTontine(id);
      final mb = await Ressources.membresDeTontine(id);
      if (mounted) setState(() { _cycles = cy; _membres = mb; });
    } on ErreurApi catch (e) {
      if (mounted) setState(() => _erreur = e.message);
    }
  }

  Future<void> _enregistrer() async {
    setState(() { _envoi = true; _erreur = null; });
    try {
      final corps = <String, dynamic>{'cycleId': _cycleId, 'membreId': _membreId};
      final mt = double.tryParse(_montant.text);
      if (mt != null && mt > 0) corps['montant'] = mt;
      await Ressources.creerCotisation(corps);
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
          Text('Enregistrer une cotisation',
              style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: Jetons.e2),
          Text('Étape 1 du parcours : cotisation → paiement → reçu.',
              style: Theme.of(context).textTheme.bodySmall),
          const SizedBox(height: Jetons.e5),
          if (_erreur != null) ...[Bandeau(_erreur!, erreur: true), const SizedBox(height: Jetons.e4)],
          if (_chargement)
            const Chargement()
          else ...[
            DropdownButtonFormField<int>(
              initialValue: _tontineId, isExpanded: true,
              decoration: const InputDecoration(labelText: 'Tontine'),
              items: _tontines.map((t) => DropdownMenuItem(
                  value: t.id, child: Text(t.nom, overflow: TextOverflow.ellipsis))).toList(),
              onChanged: _surTontine,
            ),
            const SizedBox(height: Jetons.e3),
            DropdownButtonFormField<int>(
              initialValue: _cycleId, isExpanded: true,
              decoration: const InputDecoration(labelText: 'Cycle'),
              items: _cycles.map((c) => DropdownMenuItem(
                  value: c.id,
                  child: Text('Cycle n°${c.numero} (${libelleStatut(c.statut)})'))).toList(),
              onChanged: (v) => setState(() => _cycleId = v),
            ),
            const SizedBox(height: Jetons.e3),
            DropdownButtonFormField<int>(
              initialValue: _membreId, isExpanded: true,
              decoration: const InputDecoration(labelText: 'Membre'),
              items: _membres.map((mb) => DropdownMenuItem(
                  value: mb.id,
                  child: Text(mb.nomComplet ?? '—', overflow: TextOverflow.ellipsis))).toList(),
              onChanged: (v) => setState(() => _membreId = v),
            ),
            const SizedBox(height: Jetons.e3),
            TextField(controller: _montant, keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                    labelText: 'Montant (FCFA)',
                    helperText: 'Laissez vide pour le montant par défaut de la tontine')),
            const SizedBox(height: Jetons.e5),
            FilledButton(
              onPressed: _envoi || _cycleId == null || _membreId == null ? null : _enregistrer,
              style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(48)),
              child: _envoi
                  ? const SizedBox(width: 18, height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2, color: Jetons.blanc))
                  : const Text('Enregistrer'),
            ),
          ],
          const SizedBox(height: Jetons.e5),
        ]),
      ),
    );
  }
}
