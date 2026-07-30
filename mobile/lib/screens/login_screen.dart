import 'package:flutter/material.dart';
import '../services/api_service.dart';
import 'dashboard_screen.dart';

// Connexion avec double facteur (2FA) : mot de passe puis code OTP.
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});
  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _api = ApiService();
  final _email = TextEditingController();
  final _motDePasse = TextEditingController();
  final _code = TextEditingController();
  bool _etapeOtp = false;
  String _message = '';

  Future<void> _connexion() async {
    try {
      final msg = await _api.login(_email.text, _motDePasse.text);
      setState(() { _message = msg; _etapeOtp = true; });
    } catch (e) {
      setState(() => _message = 'Erreur de connexion');
    }
  }

  Future<void> _verifierOtp() async {
    try {
      await _api.verifierOtp(_email.text, _code.text);
      if (mounted) {
        Navigator.pushReplacement(context,
            MaterialPageRoute(builder: (_) => const DashboardScreen()));
      }
    } catch (e) {
      setState(() => _message = 'Code invalide');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('TontineSafe')),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(children: [
          if (!_etapeOtp) ...[
            TextField(controller: _email, decoration: const InputDecoration(labelText: 'E-mail')),
            TextField(controller: _motDePasse, obscureText: true, decoration: const InputDecoration(labelText: 'Mot de passe')),
            const SizedBox(height: 16),
            ElevatedButton(onPressed: _connexion, child: const Text('Se connecter')),
          ] else ...[
            TextField(controller: _code, decoration: const InputDecoration(labelText: 'Code recu par e-mail')),
            const SizedBox(height: 16),
            ElevatedButton(onPressed: _verifierOtp, child: const Text('Valider le code')),
          ],
          const SizedBox(height: 16),
          Text(_message),
        ]),
      ),
    );
  }
}
