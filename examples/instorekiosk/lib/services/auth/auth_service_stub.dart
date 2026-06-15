import 'auth_service_base.dart';

class AuthServiceImpl implements AuthService {
  @override
  bool get isLoggedIn => false;
  @override
  String? get userName => null;
  @override
  String? get organization => null;

  @override
  Future<void> init() async {}
  @override
  Future<void> login() async {}
  @override
  Future<void> logout() async {}
  @override
  Future<String?> getAccessToken() async => null;
}

AuthService createAuthService() => AuthServiceImpl();
