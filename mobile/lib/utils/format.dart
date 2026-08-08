import 'package:intl/intl.dart';

/// Formatage francais : montants en FCFA, dates et heures.
final _montant = NumberFormat.decimalPattern('fr_FR');
final _date = DateFormat('dd/MM/yyyy', 'fr_FR');
final _dateHeure = DateFormat("dd/MM/yyyy 'à' HH:mm", 'fr_FR');

String fcfa(num? v) => '${_montant.format(v ?? 0)} FCFA';

String formaterDate(dynamic v) {
  final d = _parse(v);
  return d == null ? '—' : _date.format(d);
}

String formaterDateHeure(dynamic v) {
  final d = _parse(v);
  return d == null ? '—' : _dateHeure.format(d);
}

DateTime? _parse(dynamic v) {
  if (v == null) return null;
  if (v is DateTime) return v;
  return DateTime.tryParse(v.toString());
}

String initiales(String? valeur) {
  if (valeur == null || valeur.isEmpty) return '?';
  final propre = valeur.split('@').first.replaceAll(RegExp(r'[^A-Za-zÀ-ÿ ]'), ' ');
  final mots = propre.trim().split(RegExp(r'\s+')).where((m) => m.isNotEmpty).toList();
  if (mots.isEmpty) return valeur[0].toUpperCase();
  if (mots.length == 1) return mots.first[0].toUpperCase();
  return (mots[0][0] + mots[1][0]).toUpperCase();
}

/// Libelles lisibles pour les valeurs techniques renvoyees par l'API.
String libelleStatut(String? s) {
  switch (s) {
    case 'ACTIVE': return 'Active';
    case 'ACTIF': return 'Actif';
    case 'SUSPENDUE': return 'Suspendue';
    case 'SUSPENDU': return 'Suspendu';
    case 'CLOTUREE': return 'Clôturée';
    case 'EN_COURS': return 'En cours';
    case 'PLANIFIE': return 'Planifié';
    case 'PAYEE': return 'Payée';
    case 'EN_ATTENTE': return 'En attente';
    case 'INITIE': return 'Initié';
    case 'CONFIRME': return 'Confirmé';
    case 'ANNULE': return 'Annulé';
    case 'LUE': return 'Lue';
    case 'NON_LUE': return 'Non lue';
    default: return s ?? '—';
  }
}

String libellePeriodicite(String? p) {
  switch (p) {
    case 'QUOTIDIENNE': return 'Quotidienne';
    case 'HEBDOMADAIRE': return 'Hebdomadaire';
    case 'BIMENSUELLE': return 'Bimensuelle';
    case 'MENSUELLE': return 'Mensuelle';
    case 'TRIMESTRIELLE': return 'Trimestrielle';
    default: return p ?? '—';
  }
}

String libelleMethode(String? m) =>
    m == 'ORANGE_MONEY' ? 'Orange Money' : (m == 'WAVE' ? 'Wave' : (m ?? '—'));

String libelleRole(String? r) {
  switch (r) {
    case 'ADMINISTRATEUR': return 'Administrateur';
    case 'ADMINISTRATEUR_TONTINE': return 'Administrateur de la tontine';
    case 'MEMBRE': return 'Membre';
    default: return r ?? '—';
  }
}
