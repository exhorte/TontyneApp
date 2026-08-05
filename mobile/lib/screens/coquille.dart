import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../services/auth_service.dart';
import '../services/ressources.dart';
import '../theme/app_theme.dart';
import '../utils/format.dart';
import 'dashboard_screen.dart';
import 'tontines_screen.dart';
import 'cotisations_screen.dart';
import 'paiements_screen.dart';
import 'plus_screen.dart';

/// Coquille applicative : barre de navigation basse (equivalent mobile
/// du menu lateral du web) et barre superieure avec le compte.
class Coquille extends StatefulWidget {
  const Coquille({super.key});
  @override
  State<Coquille> createState() => _CoquilleState();
}

class _CoquilleState extends State<Coquille> {
  int _index = 0;
  int _nonLues = 0;

  static const _titres = [
    'Tableau de bord', 'Tontines', 'Cotisations', 'Paiements', 'Plus'
  ];

  @override
  void initState() {
    super.initState();
    _rafraichirCompteur();
  }

  Future<void> _rafraichirCompteur() async {
    final id = context.read<AuthService>().utilisateur?.id;
    if (id == null) return;
    try {
      final n = await Ressources.nonLues(id);
      if (mounted) setState(() => _nonLues = n);
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthService>();
    final u = auth.utilisateur;

    final pages = [
      const DashboardScreen(),
      const TontinesScreen(),
      const CotisationsScreen(),
      const PaiementsScreen(),
      const PlusScreen(),
    ];

    return Scaffold(
      appBar: AppBar(
        title: Text(_titres[_index]),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: Jetons.e3),
            child: Row(children: [
              Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(u?.email ?? '',
                      style: const TextStyle(
                          fontSize: 12, fontWeight: FontWeight.w500, color: Jetons.encre)),
                  Text(libelleRole(u?.role),
                      style: const TextStyle(fontSize: 11, color: Jetons.texteSecondaire)),
                ],
              ),
              const SizedBox(width: Jetons.e2),
              CircleAvatar(
                radius: 16,
                backgroundColor: Jetons.bleu,
                child: Text(initiales(u?.email),
                    style: const TextStyle(
                        fontSize: 12, color: Jetons.blanc, fontWeight: FontWeight.w600)),
              ),
            ]),
          ),
        ],
      ),
      body: IndexedStack(index: _index, children: pages),
      bottomNavigationBar: Container(
        decoration: const BoxDecoration(
            border: Border(top: BorderSide(color: Jetons.ligne))),
        child: NavigationBar(
          selectedIndex: _index,
          onDestinationSelected: (i) {
            setState(() => _index = i);
            if (i == 4) _rafraichirCompteur();
          },
          destinations: [
            const NavigationDestination(
                icon: Icon(Icons.dashboard_outlined),
                selectedIcon: Icon(Icons.dashboard),
                label: 'Accueil'),
            const NavigationDestination(
                icon: Icon(Icons.groups_outlined),
                selectedIcon: Icon(Icons.groups),
                label: 'Tontines'),
            const NavigationDestination(
                icon: Icon(Icons.savings_outlined),
                selectedIcon: Icon(Icons.savings),
                label: 'Cotisations'),
            const NavigationDestination(
                icon: Icon(Icons.credit_card_outlined),
                selectedIcon: Icon(Icons.credit_card),
                label: 'Paiements'),
            NavigationDestination(
                icon: Badge(
                  isLabelVisible: _nonLues > 0,
                  label: Text('$_nonLues'),
                  backgroundColor: Jetons.bleu,
                  child: const Icon(Icons.more_horiz),
                ),
                label: 'Plus'),
          ],
        ),
      ),
    );
  }
}
