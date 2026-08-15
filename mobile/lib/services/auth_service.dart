import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../models/modeles.dart';
import 'api_service.dart';

/// Authentification a double facteur et session courante.
class AuthService extends ChangeNotifier {
  static const _cle = 'tontyn_jeton';

  String? _jeton;
  Utilisateur? _utilisateur;
  bool _pret = false;

  String? get jeton => _jeton;
  Utilisateur? get utilisateur => _utilisateur;
  bool get pret => _pret;
  bool get connecte => _jeton != null && _utilisateur != null;
  String get role => _utilisateur?.role ?? 'MEMBRE';
  /// Administrateur de la plateforme (supervision globale).
  bool get estAdminPlateforme => role == 'ADMINISTRATEUR';

  /// Tout compte peut desormais creer une tontine et tenter une action de
  /// gestion : c'est le serveur qui tranche, tontine par tontine. Lorsqu'un
  /// ecran connait la tontine, il s'appuie plutot sur Tontine.administrateur.
  bool get peutGerer => connecte;

  AuthService() {
    ApiService.instance.surExpiration = () => deconnexion();
    _restaurer();
  }

  Future<void> _restaurer() async {
    final prefs = await SharedPreferences.getInstance();
    final j = prefs.getString(_cle);
    if (j != null && !_expire(j)) {
      _jeton = j;
      ApiService.instance.definirJeton(j);
      try {
        final d = await ApiService.instance.get('/auth/me');
        _utilisateur = Utilisateur.depuisJson(Map<String, dynamic>.from(d));
      } catch (_) {
        _jeton = null;
        ApiService.instance.definirJeton(null);
        await prefs.remove(_cle);
      }
    } else if (j != null) {
      await prefs.remove(_cle);
    }
    _pret = true;
    notifyListeners();
  }

  /// Etape 1 : verification du code PIN, envoi du code par message court.
  Future<String> demanderCode(String telephone, String codePin) async {
    final r = await ApiService.instance
        .post('/auth/login', corps: {'telephone': telephone, 'codePin': codePin});
    return r.toString();
  }

  /// Etape 2 : verification du code recu et ouverture de la session.
  Future<void> validerCode(String telephone, String code) async {
    final r = await ApiService.instance
        .post('/auth/verify-otp', corps: {'telephone': telephone, 'code': code});
    final jeton = (r is Map ? r['token'] : null)?.toString();
    if (jeton == null || jeton.isEmpty) {
      throw ErreurApi('Le jeton renvoyé par le serveur est invalide.');
    }
    _jeton = jeton;
    ApiService.instance.definirJeton(jeton);
    final d = await ApiService.instance.get('/auth/me');
    _utilisateur = Utilisateur.depuisJson(Map<String, dynamic>.from(d));
    (await SharedPreferences.getInstance()).setString(_cle, jeton);
    notifyListeners();
  }

  Future<String> inscrire(Map<String, dynamic> donnees) async {
    final r = await ApiService.instance.post('/auth/register', corps: donnees);
    return r.toString();
  }

  /// Associe une adresse electronique : un code part vers celle-ci.
  Future<String> demanderAjoutEmail(String email) async {
    final r = await ApiService.instance.post('/auth/email', corps: {'email': email});
    return r.toString();
  }

  /// Confirme l'adresse en attente au moyen du code recu.
  Future<void> confirmerEmail(String code) async {
    final d = await ApiService.instance
        .post('/auth/email/confirmer', corps: {'code': code});
    _utilisateur = Utilisateur.depuisJson(Map<String, dynamic>.from(d));
    notifyListeners();
  }

  /// Dissocie l'adresse electronique du compte.
  Future<void> retirerEmail() async {
    final d = await ApiService.instance.delete('/auth/email');
    _utilisateur = Utilisateur.depuisJson(Map<String, dynamic>.from(d));
    notifyListeners();
  }

  /// Recharge le profil depuis le serveur.
  Future<void> rafraichirProfil() async {
    if (!connecte) return;
    final d = await ApiService.instance.get('/auth/me');
    _utilisateur = Utilisateur.depuisJson(Map<String, dynamic>.from(d));
    notifyListeners();
  }

  Future<void> deconnexion() async {
    _jeton = null;
    _utilisateur = null;
    ApiService.instance.definirJeton(null);
    (await SharedPreferences.getInstance()).remove(_cle);
    notifyListeners();
  }

  /// Verifie l'echeance inscrite dans le jeton, sans dependance externe.
  bool _expire(String jeton) {
    try {
      final parties = jeton.split('.');
      if (parties.length != 3) return true;
      var charge = parties[1].replaceAll('-', '+').replaceAll('_', '/');
      while (charge.length % 4 != 0) {
        charge += '=';
      }
      final donnees = json.decode(utf8.decode(base64.decode(charge)));
      final exp = donnees['exp'];
      if (exp is! int) return false;
      return DateTime.now().millisecondsSinceEpoch >= exp * 1000;
    } catch (_) {
      return true;
    }
  }
}
