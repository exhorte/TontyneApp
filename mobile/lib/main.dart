import 'package:flutter/material.dart';
import 'screens/login_screen.dart';

void main() => runApp(const TontineSafeApp());

class TontineSafeApp extends StatelessWidget {
  const TontineSafeApp({super.key});
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'TontineSafe',
      theme: ThemeData(colorSchemeSeed: Colors.teal, useMaterial3: true),
      home: const LoginScreen(),
    );
  }
}
