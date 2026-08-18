import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/modeles.dart';
import '../services/api_service.dart';
import '../services/auth_service.dart';
import '../theme/app_theme.dart';
import '../utils/format.dart';
import '../widgets/communs.dart';
import 'membres_screen.dart';
import 'cycles_screen.dart';
import 'recus_screen.dart';
import 'notifications_screen.dart';

/// Sections secondaires et compte, dans l'esprit du menu lateral du web
/// (et de la page /profil qui n'a pas d'equivalent mobile dedie).
class PlusScreen extends StatefulWidget {
  const PlusScreen({super.key});
  @override
  State<PlusScreen> createState() => _PlusScreenState();
}

class _PlusScreenState extends State<PlusScreen> {
  // 'repos' | 'saisie' | 'confirmation' -- calque sur Profil.jsx (web).
  String _etapeEmail = 'repos';
  final _email = TextEditingController();
  final _code = TextEditingController();
  String? _erreur, _succes;
  bool _envoi = false;

  @override
  void dispose() {
    _email.dispose();
    _code.dispose();
    super.dispose();
  }

  void _reinitialiser() {
    setState(() {
      _etapeEmail = 'repos';
      _email.clear();
      _code.clear();
      _erreur = null;
    });
  }

  Future<void> _soumettreEmail(AuthService auth) async {
    setState(() { _envoi = true; _erreur = null; _succes = null; });
    try {
      final msg = await auth.demanderAjoutEmail(_email.text.trim());
      if (!mounted) return;
      setState(() {
        _succes = msg.isNotEmpty ? msg : 'Un code de confirmation vous a été envoyé.';
        _etapeEmail = 'confirmation';
      });
    } on ErreurApi catch (e) {
      if (mounted) setState(() => _erreur = e.message);
    } finally {
      if (mounted) setState(() => _envoi = false);
    }
  }

  Future<void> _soumettreCode(AuthService auth) async {
    setState(() { _envoi = true; _erreur = null; });
    try {
      await auth.confirmerEmail(_code.text.trim());
      if (!mounted) return;
      _reinitialiser();
    } on ErreurApi catch (e) {
      if (mounted) setState(() => _erreur = e.message);
    } finally {
      if (mounted) setState(() => _envoi = false);
    }
  }

  Future<void> _retirerEmail(AuthService auth) async {
    setState(() { _envoi = true; _erreur = null; _succes = null; });
    try {
      await auth.retirerEmail();
    } on ErreurApi catch (e) {
      if (mounted) setState(() => _erreur = e.message);
    } finally {
      if (mounted) setState(() => _envoi = false);
    }
  }

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

      const TitreSection('Adresse électronique'),
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: Jetons.e4),
        child: Text(
          'Facultative : un second canal de récupération, en plus de votre numéro '
          'de téléphone.',
          style: Theme.of(context).textTheme.bodySmall,
        ),
      ),
      const SizedBox(height: Jetons.e3),
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: Jetons.e4),
        child: _sectionEmail(context, auth, u),
      ),
      const SizedBox(height: Jetons.e2),

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

  /// Reprend les quatre etats de la carte email de Profil.jsx (web) :
  /// confirmee / en attente de confirmation / au repos / saisie.
  Widget _sectionEmail(BuildContext context, AuthService auth, Utilisateur? u) {
    Widget carte(List<Widget> enfants) => Container(
          padding: const EdgeInsets.all(Jetons.e4),
          decoration: BoxDecoration(
            border: Border.all(color: Jetons.bordure),
            borderRadius: BorderRadius.circular(Jetons.rCarte),
          ),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: enfants),
        );

    if (u != null && u.email != null && u.emailVerifie) {
      return carte([
        Row(children: [
          Expanded(
            child: Text(u.email!,
                style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w500))),
          const SizedBox(width: Jetons.e2),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
            decoration: BoxDecoration(
                color: Jetons.vertFond,
                borderRadius: BorderRadius.circular(Jetons.rPastille)),
            child: const Text('VÉRIFIÉE',
                style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: Jetons.vert)),
          ),
        ]),
        const SizedBox(height: Jetons.e3),
        if (_erreur != null) ...[Bandeau(_erreur!, erreur: true), const SizedBox(height: Jetons.e3)],
        SizedBox(
          width: double.infinity,
          child: OutlinedButton(
            onPressed: _envoi ? null : () => _retirerEmail(auth),
            child: _envoi
                ? const SizedBox(width: 16, height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2))
                : const Text('Retirer cette adresse'),
          ),
        ),
      ]);
    }

    if (_etapeEmail == 'repos') {
      if (u?.emailEnCoursDeConfirmation ?? false) {
        return carte([
          Bandeau('Une confirmation est en attente pour ${u!.emailEnAttente}.'),
          const SizedBox(height: Jetons.e3),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: () => setState(() => _etapeEmail = 'confirmation'),
              child: const Text('Saisir le code reçu'),
            ),
          ),
        ]);
      }
      return carte([
        SizedBox(
          width: double.infinity,
          child: FilledButton.icon(
            onPressed: () => setState(() => _etapeEmail = 'saisie'),
            icon: const Icon(Icons.add, size: 18),
            label: const Text('Associer une adresse'),
          ),
        ),
      ]);
    }

    if (_etapeEmail == 'saisie') {
      return carte([
        if (_erreur != null) ...[Bandeau(_erreur!, erreur: true), const SizedBox(height: Jetons.e3)],
        const Text('Adresse électronique',
            style: TextStyle(fontSize: 13.5, color: Jetons.texteSecondaire)),
        const SizedBox(height: 6),
        TextField(
          controller: _email,
          keyboardType: TextInputType.emailAddress,
          onChanged: (_) => setState(() {}),
          decoration: const InputDecoration(
            hintText: 'prenom.nom@exemple.sn',
            contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 17),
          ),
        ),
        const SizedBox(height: Jetons.e4),
        Row(children: [
          Expanded(
            child: FilledButton(
              onPressed: _envoi || _email.text.trim().isEmpty
                  ? null
                  : () => _soumettreEmail(auth),
              child: _envoi
                  ? const SizedBox(width: 16, height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2, color: Jetons.blanc))
                  : const Text('Envoyer le code'),
            ),
          ),
          const SizedBox(width: Jetons.e2),
          OutlinedButton(
            onPressed: _envoi ? null : _reinitialiser,
            child: const Text('Annuler'),
          ),
        ]),
      ]);
    }

    // etape == 'confirmation'
    final codeValide = _code.text.trim().length == 6;
    return carte([
      if (_succes != null) ...[Bandeau(_succes!), const SizedBox(height: Jetons.e3)],
      if (_erreur != null) ...[Bandeau(_erreur!, erreur: true), const SizedBox(height: Jetons.e3)],
      const Text('Code de confirmation (6 chiffres)',
          style: TextStyle(fontSize: 13.5, color: Jetons.texteSecondaire)),
      const SizedBox(height: 6),
      TextField(
        controller: _code,
        keyboardType: TextInputType.number,
        maxLength: 6,
        onChanged: (_) => setState(() {}),
        decoration: const InputDecoration(
          counterText: '',
          contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 17),
        ),
      ),
      Text('Code envoyé à ${u?.emailEnAttente ?? 'votre adresse'}.',
          style: Theme.of(context).textTheme.bodySmall),
      const SizedBox(height: Jetons.e4),
      Row(children: [
        Expanded(
          child: FilledButton(
            onPressed: _envoi || !codeValide ? null : () => _soumettreCode(auth),
            child: _envoi
                ? const SizedBox(width: 16, height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Jetons.blanc))
                : const Text('Confirmer'),
          ),
        ),
        const SizedBox(width: Jetons.e2),
        OutlinedButton(
          onPressed: _envoi ? null : _reinitialiser,
          child: const Text('Annuler'),
        ),
      ]),
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
