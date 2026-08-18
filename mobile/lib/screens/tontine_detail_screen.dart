import 'package:flutter/material.dart';

import '../models/modeles.dart' as m;
import '../services/api_service.dart';
import '../services/ressources.dart';
import '../theme/app_theme.dart';
import '../utils/format.dart';
import '../widgets/communs.dart';

/// Fiche d'une tontine : indicateurs, onglets Membres et Cycles.
class TontineDetailScreen extends StatefulWidget {
  final int tontineId;
  const TontineDetailScreen({super.key, required this.tontineId});
  @override
  State<TontineDetailScreen> createState() => _TontineDetailScreenState();
}

class _TontineDetailScreenState extends State<TontineDetailScreen>
    with SingleTickerProviderStateMixin {
  late final TabController _onglets = TabController(length: 2, vsync: this);
  bool _chargement = true;
  String? _erreur;
  m.Tontine? _tontine;
  List<m.Membre> _membres = [];
  List<m.Cycle> _cycles = [];

  @override
  void initState() { super.initState(); _charger(); }

  @override
  void dispose() { _onglets.dispose(); super.dispose(); }

  Future<void> _charger() async {
    setState(() { _chargement = true; _erreur = null; });
    try {
      final t = await Ressources.tontine(widget.tontineId);
      final mb = await Ressources.membresDeTontine(widget.tontineId);
      final cy = await Ressources.cyclesDeTontine(widget.tontineId);
      if (mounted) {
        setState(() { _tontine = t; _membres = mb; _cycles = cy; _chargement = false; });
      }
    } on ErreurApi catch (e) {
      if (mounted) setState(() { _erreur = e.message; _chargement = false; });
    }
  }

  double get _collecte => _cycles.fold(0.0, (s, c) => s + c.montantCollecte);

  Future<void> _ajouterMembre() async {
    final ok = await showModalBottomSheet<bool>(
      context: context, isScrollControlled: true, backgroundColor: Jetons.blanc,
      shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(Jetons.rModale))),
      builder: (_) => _AjoutMembre(tontineId: widget.tontineId),
    );
    if (ok == true) _charger();
  }

  Future<void> _genererCycles() async {
    final date = DateTime.now();
    try {
      await Ressources.genererCycles(widget.tontineId,
          '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Cycles générés.')));
        _charger();
      }
    } on ErreurApi catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    // Droit reel sur CETTE tontine, tel que le serveur le declare.
    final peutGerer = _tontine?.administrateur ?? false;
    return Scaffold(
      appBar: AppBar(
        title: Text(_tontine?.nom ?? 'Tontine'),
        actions: peutGerer && _tontine != null
            ? [
                PopupMenuButton<String>(
                  icon: const Icon(Icons.more_vert),
                  onSelected: (v) {
                    if (v == 'membre') _ajouterMembre();
                    if (v == 'cycles') _genererCycles();
                  },
                  itemBuilder: (_) => const [
                    PopupMenuItem(value: 'membre', child: Text('Ajouter un membre')),
                    PopupMenuItem(value: 'cycles', child: Text('Générer les cycles')),
                  ],
                ),
              ]
            : null,
      ),
      body: _chargement
          ? const Chargement()
          : _erreur != null
              ? ErreurVue(_erreur!, onReessayer: _charger)
              : Column(children: [
                  _entete(),
                  TabBar(
                    controller: _onglets,
                    labelColor: Jetons.encre,
                    unselectedLabelColor: Jetons.texteSecondaire,
                    indicatorColor: Jetons.encre,
                    indicatorSize: TabBarIndicatorSize.label,
                    dividerColor: Jetons.ligne,
                    labelStyle: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500),
                    unselectedLabelStyle: const TextStyle(fontSize: 14),
                    tabs: [
                      Tab(text: 'Membres (${_membres.length})'),
                      Tab(text: 'Cycles (${_cycles.length})'),
                    ],
                  ),
                  Expanded(
                    child: TabBarView(controller: _onglets, children: [
                      _listeMembres(),
                      _listeCycles(),
                    ]),
                  ),
                ]),
    );
  }

  Widget _entete() {
    final t = _tontine!;
    return Container(
      margin: const EdgeInsets.all(Jetons.e4),
      padding: const EdgeInsets.all(Jetons.e4),
      decoration: BoxDecoration(
        border: Border.all(color: Jetons.bordure),
        borderRadius: BorderRadius.circular(Jetons.rCarte),
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          BadgeStatut(t.statut),
          const Spacer(),
          Text(libellePeriodicite(t.periodicite),
              style: Theme.of(context).textTheme.bodySmall),
        ]),
        const SizedBox(height: Jetons.e4),
        Row(children: [
          Expanded(child: _info('Cotisation', fcfa(t.montantCotisation))),
          Expanded(child: _info('Membres', '${t.nombreMembresInscrits} / ${t.nombreMembres}')),
        ]),
        const SizedBox(height: Jetons.e3),
        Row(children: [
          Expanded(child: _info('Collecté', fcfa(_collecte))),
          Expanded(child: _info('Cycles', '${t.nombreCycles}')),
        ]),
      ]),
    );
  }

  Widget _info(String libelle, String valeur) =>
      Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(libelle.toUpperCase(), style: Theme.of(context).textTheme.labelSmall),
        const SizedBox(height: 2),
        Text(valeur, style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w500)),
      ]);

  Widget _listeMembres() {
    if (_membres.isEmpty) {
      return const EtatVide(
          icone: Icons.person_outline,
          titre: 'Aucun membre',
          texte: 'Ajoutez des membres avant de générer les cycles.');
    }
    return ListView(children: _membres.map((mb) => LigneListe(
          tete: CircleAvatar(
            radius: 16, backgroundColor: Jetons.bleuFond,
            child: Text('${mb.ordreTour}',
                style: const TextStyle(
                    fontSize: 12, color: Jetons.bleu, fontWeight: FontWeight.w600)),
          ),
          titre: mb.nomComplet ?? '—',
          sousTitre: mb.telephone,
          fin: BadgeStatut(mb.statut),
        )).toList());
  }

  Widget _listeCycles() {
    if (_cycles.isEmpty) {
      return const EtatVide(
          icone: Icons.autorenew,
          titre: 'Aucun cycle',
          texte: 'Générez les cycles pour démarrer la rotation.');
    }
    return ListView(children: _cycles.map((c) => LigneListe(
          titre: 'Cycle n°${c.numero}',
          sousTitre:
              '${formaterDate(c.dateDebut)} → ${formaterDate(c.dateFin)}\nBénéficiaire : ${c.beneficiaireNom ?? '—'}',
          valeur: fcfa(c.montantCollecte),
          fin: BadgeStatut(c.statut),
        )).toList());
  }
}

/// Ajout d'un membre : recherche par numero de telephone, plutot qu'une
/// selection dans l'annuaire complet des comptes de la plateforme.
class _AjoutMembre extends StatefulWidget {
  final int tontineId;
  const _AjoutMembre({required this.tontineId});
  @override
  State<_AjoutMembre> createState() => _AjoutMembreState();
}

class _AjoutMembreState extends State<_AjoutMembre> {
  final _telephoneCtrl = TextEditingController();
  String _role = 'MEMBRE';
  bool _envoi = false;
  String? _erreur;

  @override
  void dispose() { _telephoneCtrl.dispose(); super.dispose(); }

  /// Verifie le numero puis ajoute le membre en une seule action : pas de
  /// bouton de verification separe.
  Future<void> _ajouter() async {
    final saisie = _telephoneCtrl.text.trim();
    if (saisie.isEmpty) {
      setState(() => _erreur = 'Saisissez le numéro de téléphone du membre à ajouter.');
      return;
    }
    setState(() { _envoi = true; _erreur = null; });
    try {
      final trouve = await Ressources.rechercherUtilisateur(saisie);
      if (trouve == null) {
        if (mounted) {
          setState(() {
            _erreur = "Ce numéro n'est inscrit sur Tontyn avec aucun compte.";
            _envoi = false;
          });
        }
        return;
      }
      await Ressources.ajouterMembre(widget.tontineId,
          {'utilisateurId': trouve.id, 'roleGroupe': _role});
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
      child: Column(mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Center(child: Container(width: 36, height: 4,
            decoration: BoxDecoration(color: Jetons.bordure,
                borderRadius: BorderRadius.circular(2)))),
        const SizedBox(height: Jetons.e5),
        Text('Ajouter un membre', style: Theme.of(context).textTheme.titleLarge),
        const SizedBox(height: Jetons.e5),
        if (_erreur != null) ...[Bandeau(_erreur!, erreur: true), const SizedBox(height: Jetons.e4)],
        TextField(
          controller: _telephoneCtrl,
          keyboardType: TextInputType.phone,
          onChanged: (_) { if (_erreur != null) setState(() => _erreur = null); },
          decoration: const InputDecoration(
              labelText: 'Numéro de téléphone du membre', hintText: '+221 77 000 00 00'),
        ),
        const SizedBox(height: Jetons.e3),
        DropdownButtonFormField<String>(
          initialValue: _role,
          decoration: const InputDecoration(labelText: 'Rôle dans le groupe'),
          items: const [
            DropdownMenuItem(value: 'MEMBRE', child: Text('Membre')),
            DropdownMenuItem(value: 'GESTIONNAIRE', child: Text('Gestionnaire de la tontine')),
          ],
          onChanged: (v) => setState(() => _role = v ?? 'MEMBRE'),
        ),
        const SizedBox(height: Jetons.e5),
        FilledButton(
          onPressed: _envoi ? null : _ajouter,
          style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(48)),
          child: _envoi
              ? const SizedBox(width: 18, height: 18,
                  child: CircularProgressIndicator(strokeWidth: 2, color: Jetons.blanc))
              : const Text('Ajouter'),
        ),
        const SizedBox(height: Jetons.e5),
      ]),
    );
  }
}
