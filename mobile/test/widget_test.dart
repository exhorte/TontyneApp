// Test de fumee : l'application demarre et affiche l'ecran de connexion
// lorsqu'aucune session n'est enregistree.

import 'package:flutter_test/flutter_test.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:tontyn_mobile/main.dart';
import 'package:tontyn_mobile/screens/login_screen.dart';

void main() {
  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    await initializeDateFormatting('fr_FR', null);
  });

  testWidgets('Sans session, l\'application ouvre l\'ecran de connexion',
      (WidgetTester tester) async {
    await tester.pumpWidget(const TontynApp());
    await tester.pumpAndSettle();

    expect(find.byType(LoginScreen), findsOneWidget);
  });
}
