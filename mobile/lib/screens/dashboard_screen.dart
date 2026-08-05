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

/// Synthese de la situation de l'utilisateur : indicateurs, cotisations
/// a regler, dernieres notifications et liste de ses tontines.
class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});
  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  bool _chargement = true;
  String? _erreur;
  List<m.Tontine> _tontines = [];
  List<m.Cotisation> _aRegler = [];
  List<m.Notification> _notifications = [];
  double _totalVerse = 0;
  int _payees = 0;

  @override
  void initState() { super.initState(); _charger(); }

  Future<void> _charger() async {
    setState(() { _chargement = true; _erreur = null; });
    try {
      final auth = context.read<AuthService>();
      final id = auth.utilisateur!.id;

      final tontines = auth.peutGerer
          ? await Ressources.tontines()
          : await Ressources.tontinesDe(id);

      // Cotisations de l'utilisateur, via ses adhesions
      final mesMembres = await Ressources.membres(utilisateurId: id);
      final cotis = <m.Cotisation>[];
      for (final mb in mesMembres) {
        cotis.addAll(await Ressources.cotisationsDuMembre(mb.id));
      }
      final notifs = await Ressources.notificationsDe(id);

      if (!mounted) return;
      setState(() {
        _tontines = tontines;
        _aRegler = cotis.where((c) => c.statut != 'PAYEE').toList();
        _payees = cotis.where((c) => c.statut == 'PAYEE').length;
        _totalVerse = cotis
            .where((c) => c.statut == 'PAYEE')
            .fold(0.0, (s, c) => s + c.montant);
        _notifications = notifs.take(3).toList();
        _chargement = false;
      });
    } on ErreurApi catch (e) {
      if (mounted) setState(() { _erreur = e.message; _chargement = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_chargement) return const Chargement();
    if (_erreur != null) return ErreurVue(_erreur!, onReessayer: _charger);

    final auth = context.read<AuthService>();
    final t = Theme.of(context).textTheme;
    final nonLues = _notifications.where((n) => n.nonLue).length;

    return RefreshIndicator(
      onRefresh: _charger,
      color: Jetons.bleu,
      child: ListView(children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(Jetons.e4, Jetons.e5, Jetons.e4, Jetons.e2),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text('Bonjour ${auth.utilisateur!.prenom.isEmpty
                    ? auth.utilisateur!.email.split('@').first
                    : auth.utilisateur!.prenom}',
                style: t.headlineMedium),
            const SizedBox(height: 2),
            Text('Voici la situation de vos tontines et de vos cotisations.',
                style: t.bodySmall),
          ]),
        ),

        // Indicateurs
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: Jetons.e4),
          child: Column(children: [
            Row(children: [
              Expanded(child: CarteStat(
                  libelle: 'Mes tontines', valeur: '${_tontines.length}',
                  detail: '${_tontines.where((x) => x.statut == 'ACTIVE').length} active(s)')),
              const SizedBox(width: Jetons.e3),
              Expanded(child: CarteStat(
                  libelle: 'À régler', valeur: '${_aRegler.length}',
                  detail: fcfa(_aRegler.fold<double>(0, (s, c) => s + c.montant)))),
            ]),
            const SizedBox(height: Jetons.e3),
            Row(children: [
              Expanded(child: CarteStat(
                  libelle: 'Total versé', valeur: fcfa(_totalVerse),
                  detail: '$_payees cotisation(s) payée(s)')),
              const SizedBox(width: Jetons.e3),
              Expanded(child: CarteStat(
                  libelle: 'Non lues', valeur: '$nonLues',
                  detail: '${_notifications.length} au total')),
            ]),
          ]),
        ),

        // Cotisations a regler
        const TitreSection('Cotisations à régler'),
        if (_aRegler.isEmpty)
          const EtatVide(
              icone: Icons.check_circle_outline,
              titre: 'Aucune cotisation en attente',
              texte: 'Toutes vos cotisations enregistrées sont réglées.')
        else
          ..._aRegler.map((c) => LigneListe(
                titre: c.membreNom ?? 'Cotisation',
                sousTitre: '${c.tontineNom ?? ''} — cycle n°${c.cycleNumero}',
                valeur: fcfa(c.montant),
                fin: BadgeStatut(c.statut),
              )),

        // Notifications
        const TitreSection('Dernières notifications'),
        if (_notifications.isEmpty)
          const EtatVide(
              icone: Icons.notifications_none,
              titre: 'Aucune notification',
              texte: 'Les confirmations de paiement apparaîtront ici.')
        else
          ..._notifications.map((n) => LigneListe(
                titre: n.type ?? 'Notification',
                sousTitre: n.message,
                valeurSecondaire: formaterDateHeure(n.dateEnvoi),
                fin: n.nonLue ? const BadgeStatut('NON_LUE') : null,
              )),

        // Mes tontines
        const TitreSection('Mes tontines'),
        if (_tontines.isEmpty)
          const EtatVide(
              icone: Icons.groups_outlined,
              titre: 'Aucune tontine',
              texte: 'Vous ne participez à aucune tontine pour le moment.')
        else
          ..._tontines.map((x) => LigneListe(
                titre: x.nom,
                sousTitre:
                    '${libellePeriodicite(x.periodicite)} · ${x.nombreMembresInscrits}/${x.nombreMembres} membres',
                valeur: fcfa(x.montantCotisation),
                fin: BadgeStatut(x.statut),
                onTap: () => Navigator.push(context, MaterialPageRoute(
                    builder: (_) => TontineDetailScreen(tontineId: x.id))),
              )),
        const SizedBox(height: Jetons.e8),
      ]),
    );
  }
}
