import 'package:flutter/material.dart';
import '../services/api_service.dart';

// Tableau de bord : liste des tontines (endpoint protege par JWT).
class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});
  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  final _api = ApiService();
  List<dynamic> _tontines = [];

  @override
  void initState() {
    super.initState();
    _api.tontines().then((data) => setState(() => _tontines = data)).catchError((_) {});
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Mes tontines')),
      body: ListView(
        children: _tontines.map((t) => ListTile(
          title: Text(t['nom'] ?? ''),
          subtitle: Text('${t['montantCotisation']} FCFA - ${t['periodicite']}'),
        )).toList(),
      ),
    );
  }
}
