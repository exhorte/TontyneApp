import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/modeles.dart' as m;
import '../services/api_service.dart';
import '../services/auth_service.dart';
import '../services/ressources.dart';
import '../theme/app_theme.dart';
import '../utils/format.dart';
import '../widgets/communs.dart';

/// Rappels et confirmations adressés à l'utilisateur.
class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key});
  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  bool _chargement = true;
  String? _erreur;
  List<m.Notification> _notifications = [];

  @override
  void initState() { super.initState(); _charger(); }

  Future<void> _charger() async {
    setState(() { _chargement = true; _erreur = null; });
    try {
      final id = context.read<AuthService>().utilisateur!.id;
      final l = await Ressources.notificationsDe(id);
      if (mounted) setState(() { _notifications = l; _chargement = false; });
    } on ErreurApi catch (e) {
      if (mounted) setState(() { _erreur = e.message; _chargement = false; });
    }
  }

  Future<void> _marquer(m.Notification n) async {
    try {
      await Ressources.marquerLue(n.id);
      _charger();
    } on ErreurApi catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final nonLues = _notifications.where((n) => n.nonLue).length;
    return Scaffold(
      appBar: AppBar(title: const Text('Notifications')),
      body: _chargement
          ? const Chargement()
          : _erreur != null
              ? ErreurVue(_erreur!, onReessayer: _charger)
              : _notifications.isEmpty
                  ? const EtatVide(
                      icone: Icons.notifications_none,
                      titre: 'Aucune notification',
                      texte: 'Les confirmations de paiement génèrent automatiquement une notification.')
                  : RefreshIndicator(
                      onRefresh: _charger, color: Jetons.bleu,
                      child: ListView(children: [
                        Padding(
                          padding: const EdgeInsets.all(Jetons.e4),
                          child: Text(
                              '$nonLues notification(s) non lue(s) sur ${_notifications.length}.',
                              style: Theme.of(context).textTheme.bodySmall),
                        ),
                        ..._notifications.map((n) => Container(
                              decoration: BoxDecoration(
                                color: n.nonLue ? Jetons.bleuFond.withValues(alpha: 0.35) : null,
                                border: Border(
                                    left: BorderSide(
                                        color: n.nonLue ? Jetons.bleu : Colors.transparent,
                                        width: 3)),
                              ),
                              child: LigneListe(
                                titre: n.type ?? 'Notification',
                                sousTitre: '${n.message ?? ''}\n${formaterDateHeure(n.dateEnvoi)}',
                                fin: n.nonLue ? const BadgeStatut('NON_LUE') : null,
                                onTap: n.nonLue ? () => _marquer(n) : null,
                              ),
                            )),
                      ]),
                    ),
    );
  }
}
