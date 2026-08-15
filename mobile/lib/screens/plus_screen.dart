import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../services/auth_service.dart';
import '../theme/app_theme.dart';
import '../utils/format.dart';
import '../widgets/communs.dart';
import 'membres_screen.dart';
import 'cycles_screen.dart';
import 'recus_screen.dart';
import 'notifications_screen.dart';

/// Sections secondaires et compte, dans l'esprit du menu lateral du web.
class PlusScreen extends StatelessWidget {
  const PlusScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthService>();
    final u = auth.utilisateur;

    return ListView(children: [
      // Carte de compte
      Container(
        margin: const EdgeInsets.all(Jetons.e4),
        padding: const EdgeInsets.all(Jetons.e4),
        decoration: BoxDecoration(
          border: Border.all(color: Jetons.bordure),
          borderRadius: BorderRadius.circular(Jetons.rCarte),
        ),
        child: Row(children: [
          CircleAvatar(
            radius: 22, backgroundColor: Jetons.bleu,
            child: Text(initiales(u?.nomComplet),
                style: const TextStyle(color: Jetons.blanc, fontWeight: FontWeight.w600)),
          ),
          const SizedBox(width: Jetons.e3),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(u?.nomComplet ?? '',
                  style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w500)),
              const SizedBox(height: 2),
              Text(u?.telephone ?? '', style: Theme.of(context).textTheme.bodySmall),
              const SizedBox(height: Jetons.e2),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
                decoration: BoxDecoration(
                    color: Jetons.bleuFond,
                    borderRadius: BorderRadius.circular(Jetons.rPastille)),
                child: Text(libelleRole(u?.role).toUpperCase(),
                    style: const TextStyle(
                        fontSize: 11, fontWeight: FontWeight.w600, color: Jetons.bleu)),
              ),
            ]),
          ),
        ]),
      ),

      const TitreSection('Sections'),
      _entree(context, Icons.person_outline, 'Membres',
          'Adhésions aux tontines', const MembresScreen()),
      _entree(context, Icons.autorenew, 'Cycles',
          'Tours de cotisation et bénéficiaires', const CyclesScreen()),
      _entree(context, Icons.receipt_long_outlined, 'Reçus',
          'Justificatifs de cotisation', const RecusScreen()),
      _entree(context, Icons.notifications_none, 'Notifications',
          'Rappels et confirmations', const NotificationsScreen()),

      const TitreSection('Session'),
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: Jetons.e4),
        child: Text(
            auth.peutGerer
                ? 'Vous pouvez créer et modifier les tontines, les membres et les cycles.'
                : 'Vous pouvez cotiser et régler vos paiements.',
            style: Theme.of(context).textTheme.bodySmall),
      ),
      Padding(
        padding: const EdgeInsets.all(Jetons.e4),
        child: OutlinedButton.icon(
          onPressed: () => auth.deconnexion(),
          icon: const Icon(Icons.logout, size: 18),
          label: const Text('Déconnexion'),
          style: OutlinedButton.styleFrom(
              minimumSize: const Size.fromHeight(46),
              foregroundColor: Jetons.rouge,
              side: const BorderSide(color: Jetons.rougeFond)),
        ),
      ),
      const SizedBox(height: Jetons.e8),
    ]);
  }

  Widget _entree(BuildContext context, IconData icone, String titre,
          String sousTitre, Widget page) =>
      LigneListe(
        tete: PastilleIcone(icone),
        titre: titre,
        sousTitre: sousTitre,
        onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => page)),
      );
}
