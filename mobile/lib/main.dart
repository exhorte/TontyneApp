import 'package:flutter/material.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:provider/provider.dart';

import 'services/auth_service.dart';
import 'theme/app_theme.dart';
import 'screens/login_screen.dart';
import 'screens/coquille.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('fr_FR', null);
  runApp(const TontynApp());
}

class TontynApp extends StatelessWidget {
  const TontynApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => AuthService(),
      child: MaterialApp(
        title: 'Tontyn',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.construire(),
        home: const _Porte(),
      ),
    );
  }
}

/// Aiguillage : session en cours de restauration, puis connexion ou application.
class _Porte extends StatelessWidget {
  const _Porte();
  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthService>();
    if (!auth.pret) {
      return const Scaffold(
        body: Center(
          child: SizedBox(
              width: 26, height: 26,
              child: CircularProgressIndicator(strokeWidth: 2.2, color: Jetons.bleu)),
        ),
      );
    }
    return auth.connecte ? const Coquille() : const LoginScreen();
  }
}
