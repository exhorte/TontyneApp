import 'package:dio/dio.dart';

// Service HTTP centralise vers l'API Spring Boot.
// Sur emulateur Android, 'localhost' de la machine hote est 10.0.2.2.
class ApiService {
  static final ApiService _instance = ApiService._internal();
  factory ApiService() => _instance;

  late final Dio dio;
  String? _token;

  ApiService._internal() {
    dio = Dio(BaseOptions(baseUrl: 'http://10.0.2.2:8080/api'));
    dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) {
        if (_token != null) {
          options.headers['Authorization'] = 'Bearer $_token';
        }
        handler.next(options);
      },
    ));
  }

  void setToken(String token) => _token = token;

  Future<String> login(String email, String motDePasse) async {
    final res = await dio.post('/auth/login',
        data: {'email': email, 'motDePasse': motDePasse});
    return res.data.toString();
  }

  Future<String> verifierOtp(String email, String code) async {
    final res = await dio.post('/auth/verify-otp',
        data: {'email': email, 'code': code});
    final token = res.data['token'] as String;
    setToken(token);
    return token;
  }

  Future<List<dynamic>> tontines() async {
    final res = await dio.get('/tontines');
    return res.data as List<dynamic>;
  }
}
