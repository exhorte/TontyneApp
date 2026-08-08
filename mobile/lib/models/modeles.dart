/// Modeles alignes sur les DTO du backend (structures plates).
library;

class Utilisateur {
  final int id;
  final String nom, prenom, email, role;
  final String? telephone;
  Utilisateur.depuisJson(Map<String, dynamic> j)
      : id = j['id'],
        nom = j['nom'] ?? '',
        prenom = j['prenom'] ?? '',
        email = j['email'] ?? '',
        telephone = j['telephone'],
        role = j['role'] ?? 'MEMBRE';
  String get nomComplet => '$prenom $nom'.trim().isEmpty ? email : '$prenom $nom'.trim();
}

class Tontine {
  final int id;
  final String nom, periodicite, statut;
  final String? description, dateCreation, dateDebut;
  final double montantCotisation;
  final int nombreMembres;
  final int nombreMembresInscrits, nombreCycles;
  /// Position de l'utilisateur courant vis-a-vis de cette tontine, renseignee
  /// par le serveur : les droits ne decoulent plus d'un role global.
  final bool administrateur, membre;
  Tontine.depuisJson(Map<String, dynamic> j)
      : id = j['id'],
        nom = j['nom'] ?? '',
        description = j['description'],
        montantCotisation = (j['montantCotisation'] ?? 0).toDouble(),
        periodicite = j['periodicite'] ?? '',
        nombreMembres = j['nombreMembres'] ?? 0,
        nombreMembresInscrits = (j['nombreMembresInscrits'] ?? 0).toInt(),
        nombreCycles = (j['nombreCycles'] ?? 0).toInt(),
        dateCreation = j['dateCreation']?.toString(),
        dateDebut = j['dateDebut']?.toString(),
        administrateur = j['administrateur'] == true,
        membre = j['membre'] == true,
        statut = j['statut'] ?? '';
}

class Membre {
  final int id;
  final String? dateAdhesion, roleGroupe, nomComplet, email, tontineNom;
  final int ordreTour;
  final String statut;
  final int? utilisateurId, tontineId;
  Membre.depuisJson(Map<String, dynamic> j)
      : id = j['id'],
        dateAdhesion = j['dateAdhesion']?.toString(),
        roleGroupe = j['roleGroupe'],
        ordreTour = j['ordreTour'] ?? 0,
        statut = j['statut'] ?? '',
        utilisateurId = j['utilisateurId'],
        nomComplet = j['nomComplet'],
        email = j['email'],
        tontineId = j['tontineId'],
        tontineNom = j['tontineNom'];
}

class Cycle {
  final int id, numero;
  final String? dateDebut, dateFin, tontineNom, beneficiaireNom;
  final String statut;
  final int? tontineId, beneficiaireId;
  final double montantCollecte;
  Cycle.depuisJson(Map<String, dynamic> j)
      : id = j['id'],
        numero = j['numero'] ?? 0,
        dateDebut = j['dateDebut']?.toString(),
        dateFin = j['dateFin']?.toString(),
        statut = j['statut'] ?? '',
        tontineId = j['tontineId'],
        tontineNom = j['tontineNom'],
        beneficiaireId = j['beneficiaireId'],
        beneficiaireNom = j['beneficiaireNom'],
        montantCollecte = (j['montantCollecte'] ?? 0).toDouble();
}

class Cotisation {
  final int id;
  final double montant;
  final String? date, membreNom, tontineNom;
  final String statut;
  final int? cycleId, membreId, tontineId;
  final int cycleNumero;
  Cotisation.depuisJson(Map<String, dynamic> j)
      : id = j['id'],
        montant = (j['montant'] ?? 0).toDouble(),
        date = j['date']?.toString(),
        statut = j['statut'] ?? '',
        cycleId = j['cycleId'],
        cycleNumero = j['cycleNumero'] ?? 0,
        membreId = j['membreId'],
        membreNom = j['membreNom'],
        tontineId = j['tontineId'],
        tontineNom = j['tontineNom'];
}

class Paiement {
  final int id;
  final double montant;
  final String? date, methode, reference, membreNom;
  final String statut;
  final int? cotisationId, membreId, recuId;
  Paiement.depuisJson(Map<String, dynamic> j)
      : id = j['id'],
        montant = (j['montant'] ?? 0).toDouble(),
        date = j['date']?.toString(),
        methode = j['methode']?.toString(),
        reference = j['reference'],
        statut = j['statut'] ?? '',
        cotisationId = j['cotisationId'],
        membreId = j['membreId'],
        membreNom = j['membreNom'],
        recuId = j['recuId'];
}

class Recu {
  final int id;
  final String? numero, dateEmission, referencePaiement, methode,
      membreNom, tontineNom;
  final double montant;
  final int? paiementId, membreId, tontineId;
  final int cycleNumero;
  Recu.depuisJson(Map<String, dynamic> j)
      : id = j['id'],
        numero = j['numero'],
        dateEmission = j['dateEmission']?.toString(),
        montant = (j['montant'] ?? 0).toDouble(),
        paiementId = j['paiementId'],
        referencePaiement = j['referencePaiement'],
        methode = j['methode']?.toString(),
        membreId = j['membreId'],
        membreNom = j['membreNom'],
        tontineId = j['tontineId'],
        tontineNom = j['tontineNom'],
        cycleNumero = j['cycleNumero'] ?? 0;
}

class Notification {
  final int id;
  final String? type, message, dateEnvoi, canal, destinataire;
  final String statut;
  final int? utilisateurId;
  Notification.depuisJson(Map<String, dynamic> j)
      : id = j['id'],
        type = j['type'],
        message = j['message'],
        dateEnvoi = j['dateEnvoi']?.toString(),
        canal = j['canal'],
        statut = j['statut'] ?? '',
        utilisateurId = j['utilisateurId'],
        destinataire = j['destinataire'];
  bool get nonLue => statut == 'NON_LUE';
}
