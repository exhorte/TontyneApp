import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../services/api_service.dart';
import '../services/auth_service.dart';
import '../theme/app_theme.dart';
import '../widgets/communs.dart';
import 'register_screen.dart';

/// Connexion en deux etapes (double facteur), calquee sur la page web.
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});
  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _email = TextEditingController();
  final _motDePasse = TextEditingController();
  final _code = TextEditingController();
  bool _etapeCode = false, _envoi = false;
  String? _erreur, _info;

  @override
  void dispose() {
    _email.dispose(); _motDePasse.dispose(); _code.dispose();
    super.dispose();
  }

  Future<void> _demanderCode() async {
    setState(() { _envoi = true; _erreur = null; });
    try {
      final msg = await context.read<AuthService>()
          .demanderCode(_email.text.trim(), _motDePasse.text);
      if (!mounted) return;
      setState(() { _etapeCode = true; _info = msg; });
    } on ErreurApi catch (e) {
      if (mounted) setState(() => _erreur = e.message);
    } finally {
      if (mounted) setState(() => _envoi = false);
    }
  }

  Future<void> _valider() async {
    setState(() { _envoi = true; _erreur = null; });
    try {
      await context.read<AuthService>()
          .validerCode(_email.text.trim(), _code.text.trim());
    } on ErreurApi catch (e) {
      if (mounted) setState(() => _erreur = e.message);
    } finally {
      if (mounted) setState(() => _envoi = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    return Scaffold(
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(Jetons.e6, Jetons.e8, Jetons.e6, Jetons.e6),
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            // Marque
            Row(children: [
              Container(
                width: 30, height: 30,
                decoration: BoxDecoration(
                    color: Jetons.bleu, borderRadius: BorderRadius.circular(8)),
                alignment: Alignment.center,
                child: const Text('T',
                    style: TextStyle(color: Jetons.blanc, fontWeight: FontWeight.w700)),
              ),
              const SizedBox(width: Jetons.e2),
              const Text('Tontyn',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600)),
            ]),
            const SizedBox(height: 56),

            Text(_etapeCode ? 'Vérification' : 'Bienvenue sur Tontyn',
                style: t.headlineMedium),
            const SizedBox(height: Jetons.e1),
            Text(
              _etapeCode
                  ? 'Saisissez le code à six chiffres reçu par e-mail.'
                  : 'Connectez-vous pour gérer vos tontines en toute sécurité.',
              style: t.bodyMedium!.copyWith(color: Jetons.texteSecondaire),
            ),
            const SizedBox(height: Jetons.e6),

            if (_erreur != null) ...[
              Bandeau(_erreur!, erreur: true), const SizedBox(height: Jetons.e4),
            ],

            if (!_etapeCode) ...[
              _champ('Adresse e-mail', _email,
                  clavier: TextInputType.emailAddress, indice: 'prenom.nom@exemple.sn'),
              const SizedBox(height: Jetons.e4),
              _champ('Mot de passe', _motDePasse, masque: true),
              const SizedBox(height: Jetons.e5),
              _boutonPrincipal('Se connecter',
                  actif: _email.text.isNotEmpty || true, action: _demanderCode),
              const SizedBox(height: Jetons.e4),
              Center(
                child: Wrap(alignment: WrapAlignment.center, children: [
                  Text('Pas encore de compte ? ', style: t.bodyMedium),
                  GestureDetector(
                    onTap: () => Navigator.push(context,
                        MaterialPageRoute(builder: (_) => const RegisterScreen())),
                    child: const Text('Créer un compte',
                        style: TextStyle(
                            fontSize: 14,
                            decoration: TextDecoration.underline,
                            color: Jetons.encre)),
                  ),
                ]),
              ),
            ] else ...[
              if (_info != null) ...[Bandeau(_info!), const SizedBox(height: Jetons.e4)],
              _champ('Code de vérification (6 chiffres)', _code,
                  clavier: TextInputType.number, maxLongueur: 6),
              const SizedBox(height: Jetons.e2),
              Text('Code envoyé à ${_email.text}. Il expire au bout de 5 minutes.',
                  style: t.bodySmall),
              const SizedBox(height: Jetons.e5),
              _boutonPrincipal('Valider et accéder', action: _valider),
              const SizedBox(height: Jetons.e3),
              OutlinedButton.icon(
                onPressed: _envoi ? null : () => setState(() {
                  _etapeCode = false; _code.clear(); _erreur = null;
                }),
                icon: const Icon(Icons.chevron_left, size: 18),
                label: const Text('Modifier mes identifiants'),
                style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(52)),
              ),
            ],
          ]),
        ),
      ),
    );
  }

  Widget _champ(String etiquette, TextEditingController c,
      {bool masque = false, TextInputType? clavier, String? indice, int? maxLongueur}) {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text(etiquette, style: const TextStyle(fontSize: 13.5, color: Jetons.texteSecondaire)),
      const SizedBox(height: 6),
      TextField(
        controller: c,
        obscureText: masque,
        keyboardType: clavier,
        maxLength: maxLongueur,
        onChanged: (_) => setState(() {}),
        decoration: InputDecoration(
          hintText: indice,
          counterText: '',
          contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 17),
        ),
      ),
    ]);
  }

  Widget _boutonPrincipal(String libelle, {bool actif = true, required VoidCallback action}) {
    return FilledButton(
      onPressed: _envoi || !actif ? null : action,
      style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(52)),
      child: _envoi
          ? const SizedBox(width: 18, height: 18,
              child: CircularProgressIndicator(strokeWidth: 2, color: Jetons.blanc))
          : Text(libelle, style: const TextStyle(fontSize: 15)),
    );
  }
}
