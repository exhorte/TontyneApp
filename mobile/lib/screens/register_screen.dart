import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../services/api_service.dart';
import '../services/auth_service.dart';
import '../theme/app_theme.dart';
import '../widgets/communs.dart';

/// Creation de compte : reprend les champs de la page web d'inscription.
class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});
  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _prenom = TextEditingController();
  final _nom = TextEditingController();
  final _email = TextEditingController();
  final _telephone = TextEditingController();
  final _motDePasse = TextEditingController();
  final _confirmation = TextEditingController();
  bool _envoi = false;
  String? _erreur;
  Map<String, String> _champsEnErreur = {};

  @override
  void dispose() {
    for (final c in [_prenom, _nom, _email, _telephone, _motDePasse, _confirmation]) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _soumettre() async {
    if (_motDePasse.text != _confirmation.text) {
      setState(() => _champsEnErreur = {'confirmation': 'Les mots de passe ne correspondent pas.'});
      return;
    }
    setState(() { _envoi = true; _erreur = null; _champsEnErreur = {}; });
    try {
      await context.read<AuthService>().inscrire({
        'prenom': _prenom.text.trim(),
        'nom': _nom.text.trim(),
        'email': _email.text.trim(),
        'telephone': _telephone.text.trim(),
        'motDePasse': _motDePasse.text,
      });
      if (!mounted) return;
      Navigator.pop(context);
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
          content: Text('Compte créé. Vous pouvez maintenant vous connecter.')));
    } on ErreurApi catch (e) {
      if (mounted) {
        setState(() { _erreur = e.message; _champsEnErreur = e.champs; });
      }
    } finally {
      if (mounted) setState(() => _envoi = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Créer un compte')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(Jetons.e6),
        child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Text('Rejoignez vos tontines en confiance.', style: t.headlineMedium),
          const SizedBox(height: Jetons.e1),
          Text(
              'Créez votre compte et suivez vos cotisations, vos cycles et vos reçus '
              'en toute sécurité.',
              style: t.bodyMedium!.copyWith(color: Jetons.texteSecondaire)),
          const SizedBox(height: Jetons.e6),

          if (_erreur != null) ...[Bandeau(_erreur!, erreur: true), const SizedBox(height: Jetons.e4)],

          _champ('Prénom', _prenom, cle: 'prenom'),
          _champ('Nom', _nom, cle: 'nom'),
          _champ('Adresse e-mail', _email, cle: 'email', clavier: TextInputType.emailAddress),
          _champ('Téléphone', _telephone, cle: 'telephone',
              clavier: TextInputType.phone, aide: 'Facultatif. Format : +221 77 000 00 00'),
          _champ('Mot de passe', _motDePasse, cle: 'motDePasse',
              masque: true, aide: '8 caractères minimum'),
          _champ('Confirmation', _confirmation, cle: 'confirmation', masque: true),

          const SizedBox(height: Jetons.e4),
          FilledButton(
            onPressed: _envoi ? null : _soumettre,
            style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(52)),
            child: _envoi
                ? const SizedBox(width: 18, height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Jetons.blanc))
                : const Text('Créer mon compte', style: TextStyle(fontSize: 15)),
          ),
          const SizedBox(height: Jetons.e5),
          const Divider(),
          const SizedBox(height: Jetons.e4),
          const Wrap(alignment: WrapAlignment.center, spacing: Jetons.e4, runSpacing: Jetons.e2,
              children: [
                _Atout('Traçabilité totale'),
                _Atout('Double authentification'),
                _Atout('Orange Money & Wave'),
              ]),
        ]),
      ),
    );
  }

  Widget _champ(String etiquette, TextEditingController c,
      {required String cle, bool masque = false, TextInputType? clavier, String? aide}) {
    final err = _champsEnErreur[cle];
    return Padding(
      padding: const EdgeInsets.only(bottom: Jetons.e4),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(etiquette,
            style: const TextStyle(fontSize: 13.5, fontWeight: FontWeight.w500)),
        const SizedBox(height: 6),
        TextField(
          controller: c,
          obscureText: masque,
          keyboardType: clavier,
          decoration: InputDecoration(errorText: err, helperText: aide),
        ),
      ]),
    );
  }
}

class _Atout extends StatelessWidget {
  final String texte;
  const _Atout(this.texte);
  @override
  Widget build(BuildContext context) => Row(mainAxisSize: MainAxisSize.min, children: [
        const Icon(Icons.check_circle_outline, size: 16, color: Jetons.bleu),
        const SizedBox(width: 6),
        Text(texte, style: const TextStyle(fontSize: 13, color: Jetons.texteSecondaire)),
      ]);
}
