import 'dart:io';
import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

/// Erreur applicative normalisee a partir de l'ApiError du backend.
class ErreurApi implements Exception {
  final int? statut;
  final String message;
  final Map<String, String> champs;
  ErreurApi(this.message, {this.statut, this.champs = const {}});
  @override
  String toString() => message;
}

/// Client HTTP unique vers l'API Tontyn.
class ApiService {
  ApiService._interne() {
    dio = Dio(BaseOptions(
      baseUrl: baseUrl,
      connectTimeout: const Duration(seconds: 12),
      receiveTimeout: const Duration(seconds: 12),
      headers: {'Content-Type': 'application/json'},
    ));
    dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) {
        if (_jeton != null) {
          options.headers['Authorization'] = 'Bearer $_jeton';
        }
        handler.next(options);
      },
      onError: (e, handler) {
        if (e.response?.statusCode == 401 && surExpiration != null) {
          surExpiration!();
        }
        handler.next(e);
      },
    ));
  }
  static final ApiService instance = ApiService._interne();

  late final Dio dio;
  String? _jeton;
  void Function()? surExpiration;

  void definirJeton(String? j) => _jeton = j;

  /// Sur emulateur Android, le « localhost » de la machine hote est 10.0.2.2.
  static String get baseUrl {
    const surcharge = String.fromEnvironment('API_URL');
    if (surcharge.isNotEmpty) return surcharge;
    if (!kIsWeb && Platform.isAndroid) return 'http://10.0.2.2:8080/api';
    return 'http://localhost:8080/api';
  }

  // --- Methodes generiques -------------------------------------------------
  Future<dynamic> get(String chemin, {Map<String, dynamic>? params}) =>
      _executer(() => dio.get(chemin, queryParameters: params));

  Future<dynamic> post(String chemin, {dynamic corps}) =>
      _executer(() => dio.post(chemin, data: corps));

  Future<dynamic> put(String chemin, {dynamic corps}) =>
      _executer(() => dio.put(chemin, data: corps));

  Future<dynamic> patch(String chemin, {dynamic corps}) =>
      _executer(() => dio.patch(chemin, data: corps));

  Future<dynamic> delete(String chemin) => _executer(() => dio.delete(chemin));

  Future<dynamic> _executer(Future<Response> Function() appel) async {
    try {
      final r = await appel();
      return r.data;
    } on DioException catch (e) {
      throw _normaliser(e);
    }
  }

  ErreurApi _normaliser(DioException e) {
    if (e.type == DioExceptionType.connectionTimeout ||
        e.type == DioExceptionType.connectionError) {
      return ErreurApi(
          "Serveur injoignable. Vérifiez que l'API est démarrée "
          "et que l'adresse ${ApiService.baseUrl} est correcte.");
    }
    final d = e.response?.data;
    final code = e.response?.statusCode;
    if (d is Map) {
      final champs = <String, String>{};
      final det = d['champs'] ?? d['details'] ?? d['errors'];
      if (det is Map) {
        det.forEach((k, v) => champs[k.toString()] = v.toString());
      }
      return ErreurApi(
        (d['message'] ?? d['error'] ?? 'Une erreur est survenue.').toString(),
        statut: code,
        champs: champs,
      );
    }
    if (d is String && d.trim().isNotEmpty) {
      return ErreurApi(d, statut: code);
    }
    return ErreurApi('Une erreur est survenue (code $code).', statut: code);
  }
}
