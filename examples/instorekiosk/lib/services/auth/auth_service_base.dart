abstract class AuthService {
  bool get isLoggedIn;
  String? get userName;
  String? get organization;

  Future<void> init();
  Future<void> login();
  Future<void> logout();
  Future<String?> getAccessToken();
  Future<void> refresh();
}

AuthService createAuthService() => throw UnsupportedError('Cannot create AuthService without dart:html or dart:io');
