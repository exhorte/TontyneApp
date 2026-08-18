import '../models/modeles.dart';
import 'api_service.dart';

/// Acces aux ressources metier de l'API (un objet par ressource),
/// calque sur la couche `api/` du frontend web.
class Ressources {
  static final api = ApiService.instance;

  static List<Map<String, dynamic>> _liste(dynamic d) =>
      (d as List).map((e) => Map<String, dynamic>.from(e)).toList();

  // --- Tontines ------------------------------------------------------------
  static Future<List<Tontine>> tontines({String? statut}) async =>
      _liste(await api.get('/tontines',
              params: statut == null ? null : {'statut': statut}))
          .map(Tontine.depuisJson).toList();

  static Future<List<Tontine>> tontinesDe(int utilisateurId) async =>
      _liste(await api.get('/tontines/utilisateur/$utilisateurId'))
          .map(Tontine.depuisJson).toList();

  static Future<Tontine> tontine(int id) async =>
      Tontine.depuisJson(Map<String, dynamic>.from(await api.get('/tontines/$id')));

  static Future<Tontine> creerTontine(Map<String, dynamic> c) async =>
      Tontine.depuisJson(Map<String, dynamic>.from(await api.post('/tontines', corps: c)));

  static Future<Tontine> modifierTontine(int id, Map<String, dynamic> c) async =>
      Tontine.depuisJson(Map<String, dynamic>.from(await api.put('/tontines/$id', corps: c)));

  static Future<void> cloturerTontine(int id) => api.patch('/tontines/$id/cloturer');
  static Future<void> supprimerTontine(int id) => api.delete('/tontines/$id');

  static Future<List<Membre>> membresDeTontine(int id) async =>
      _liste(await api.get('/tontines/$id/membres')).map(Membre.depuisJson).toList();

  static Future<Membre> ajouterMembre(int tontineId, Map<String, dynamic> c) async =>
      Membre.depuisJson(Map<String, dynamic>.from(
          await api.post('/tontines/$tontineId/membres', corps: c)));

  static Future<List<Cycle>> cyclesDeTontine(int id) async =>
      _liste(await api.get('/tontines/$id/cycles')).map(Cycle.depuisJson).toList();

  static Future<List<Cycle>> genererCycles(int id, String dateDebut) async =>
      _liste(await api.post('/tontines/$id/cycles/generer',
              corps: {'dateDebut': dateDebut}))
          .map(Cycle.depuisJson).toList();

  // --- Membres -------------------------------------------------------------
  static Future<List<Membre>> membres({int? tontineId, int? utilisateurId}) async {
    final p = <String, dynamic>{};
    if (tontineId != null) p['tontineId'] = tontineId;
    if (utilisateurId != null) p['utilisateurId'] = utilisateurId;
    return _liste(await api.get('/membres', params: p.isEmpty ? null : p))
        .map(Membre.depuisJson).toList();
  }

  static Future<List<Cotisation>> cotisationsDuMembre(int id) async =>
      _liste(await api.get('/membres/$id/cotisations')).map(Cotisation.depuisJson).toList();

  static Future<void> suspendreMembre(int id) => api.patch('/membres/$id/suspendre');
  static Future<void> reactiverMembre(int id) => api.patch('/membres/$id/reactiver');
  static Future<void> retirerMembre(int id) => api.delete('/membres/$id');

  // --- Cycles --------------------------------------------------------------
  static Future<List<Cycle>> cycles({int? tontineId}) async =>
      _liste(await api.get('/cycles',
              params: tontineId == null ? null : {'tontineId': tontineId}))
          .map(Cycle.depuisJson).toList();

  static Future<Cycle> cycle(int id) async =>
      Cycle.depuisJson(Map<String, dynamic>.from(await api.get('/cycles/$id')));

  static Future<List<Cotisation>> cotisationsDuCycle(int id) async =>
      _liste(await api.get('/cycles/$id/cotisations')).map(Cotisation.depuisJson).toList();

  static Future<void> cloturerCycle(int id) => api.patch('/cycles/$id/cloturer');

  // --- Cotisations ---------------------------------------------------------
  static Future<List<Cotisation>> cotisations(
      {int? tontineId, int? cycleId, int? membreId, String? statut}) async {
    final p = <String, dynamic>{};
    if (tontineId != null) p['tontineId'] = tontineId;
    if (cycleId != null) p['cycleId'] = cycleId;
    if (membreId != null) p['membreId'] = membreId;
    if (statut != null) p['statut'] = statut;
    return _liste(await api.get('/cotisations', params: p.isEmpty ? null : p))
        .map(Cotisation.depuisJson).toList();
  }

  static Future<Cotisation> creerCotisation(Map<String, dynamic> c) async =>
      Cotisation.depuisJson(Map<String, dynamic>.from(
          await api.post('/cotisations', corps: c)));

  static Future<Paiement?> paiementDeCotisation(int id) async {
    final d = await api.get('/cotisations/$id/paiement');
    return d == null ? null : Paiement.depuisJson(Map<String, dynamic>.from(d));
  }

  // --- Paiements -----------------------------------------------------------
  static Future<List<Paiement>> paiements({String? statut}) async =>
      _liste(await api.get('/paiements',
              params: statut == null ? null : {'statut': statut}))
          .map(Paiement.depuisJson).toList();

  static Future<Paiement> initierPaiement(Map<String, dynamic> c) async =>
      Paiement.depuisJson(Map<String, dynamic>.from(await api.post('/paiements', corps: c)));

  static Future<Paiement> confirmerPaiement(int id) async =>
      Paiement.depuisJson(Map<String, dynamic>.from(
          await api.patch('/paiements/$id/confirmer')));

  static Future<void> annulerPaiement(int id) => api.patch('/paiements/$id/annuler');

  static Future<Recu?> recuDuPaiement(int id) async {
    final d = await api.get('/paiements/$id/recu');
    return d == null ? null : Recu.depuisJson(Map<String, dynamic>.from(d));
  }

  // --- Recus ---------------------------------------------------------------
  static Future<List<Recu>> recus({int? membreId}) async =>
      _liste(await api.get('/recus',
              params: membreId == null ? null : {'membreId': membreId}))
          .map(Recu.depuisJson).toList();

  static Future<Recu> recu(int id) async =>
      Recu.depuisJson(Map<String, dynamic>.from(await api.get('/recus/$id')));

  // --- Notifications -------------------------------------------------------
  static Future<List<Notification>> notifications() async =>
      _liste(await api.get('/notifications')).map(Notification.depuisJson).toList();

  static Future<List<Notification>> notificationsDe(int utilisateurId) async =>
      _liste(await api.get('/notifications/utilisateur/$utilisateurId'))
          .map(Notification.depuisJson).toList();

  static Future<int> nonLues(int utilisateurId) async {
    final d = await api.get('/notifications/utilisateur/$utilisateurId/non-lues');
    if (d is Map) return (d['nonLues'] ?? 0) as int;
    if (d is List) return d.length;
    return 0;
  }

  static Future<void> marquerLue(int id) => api.patch('/notifications/$id/lue');

  // --- Utilisateurs (annuaire) ---------------------------------------------
  static Future<List<Utilisateur>> utilisateurs() async =>
      _liste(await api.get('/utilisateurs')).map(Utilisateur.depuisJson).toList();

  /// Recherche un compte par numero de telephone exact (remplace l'annuaire
  /// complet pour designer la personne a ajouter a une tontine).
  /// Renvoie `null` si aucun compte n'est inscrit avec ce numero.
  static Future<Utilisateur?> rechercherUtilisateur(String telephone) async {
    try {
      final d = await api.get('/utilisateurs/recherche', params: {'telephone': telephone});
      return Utilisateur.depuisJson(Map<String, dynamic>.from(d));
    } on ErreurApi catch (e) {
      if (e.statut == 404) return null;
      rethrow;
    }
  }
}
