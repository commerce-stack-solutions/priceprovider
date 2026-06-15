import 'package:openid_client/openid_client_browser.dart';
import 'auth_service_base.dart';

class AuthServiceImpl implements AuthService {
  Authenticator? _authenticator;
  Credential? _credential;
  bool _isLoggedIn = false;
  String? _userName;
  String? _organization;

  @override
  bool get isLoggedIn => _isLoggedIn;
  @override
  String? get userName => _userName;
  @override
  String? get organization => _organization;

  @override
  Future<void> init() async {
    final issuer = await Issuer.discover(Uri.parse('http://localhost:8081/realms/priceprovider'));
    final client = Client(issuer, 'instorekiosk');
    _authenticator = Authenticator(client, scopes: ['openid', 'profile', 'email']);

    _credential = await _authenticator!.credential;
    if (_credential != null) {
      final userInfo = await _credential!.getUserInfo();
      _isLoggedIn = true;
      _userName = userInfo.preferredUsername ?? userInfo.name;

      final groups = userInfo['groups'] as List?;
      if (groups != null) {
        final prefix = '/organizations/';
        final orgGroups = groups
            .whereType<String>()
            .where((g) => g.startsWith(prefix))
            .toList();
        orgGroups.sort((a, b) => b.split('/').length - a.split('/').length);
        if (orgGroups.isNotEmpty) {
          _organization = orgGroups.first.substring(prefix.length);
        }
      }
    }
  }

  @override
  Future<void> login() async {
    _authenticator?.authorize();
  }

  @override
  Future<void> logout() async {
    _credential = null;
    _isLoggedIn = false;
    _userName = null;
    _organization = null;
  }

  @override
  Future<String?> getAccessToken() async {
    if (_credential == null) return null;
    final t = await _credential!.getTokenResponse();
    return t.accessToken;
  }
}

AuthService createAuthService() => AuthServiceImpl();
