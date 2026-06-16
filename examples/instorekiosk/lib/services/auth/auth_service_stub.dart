import 'dart:convert';
import 'dart:developer' as developer;
import 'package:flutter/foundation.dart' as foundation;
import 'package:flutter_appauth/flutter_appauth.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;
import 'package:openid_client/openid_client_io.dart' as openid;
import 'package:url_launcher/url_launcher.dart';
import 'auth_service_base.dart';

class AuthServiceImpl implements AuthService {
  final FlutterAppAuth _appAuth = const FlutterAppAuth();
  final FlutterSecureStorage _secureStorage = const FlutterSecureStorage();

  bool _isLoggedIn = false;
  String? _userName;
  String? _organization;

  String? _accessToken;
  String? _refreshToken;
  DateTime? _accessTokenExpiration;

  static const String _clientId = 'instorekiosk';
  static const String _redirectUri = 'io.commercestacksolutions.instorekiosk://login-callback';
  static const String _desktopRedirectUri = 'http://localhost:4000/';
  static const String _discoveryUrl = 'http://localhost:8081/realms/priceprovider/.well-known/openid-configuration';

  @override
  bool get isLoggedIn => _isLoggedIn;
  @override
  String? get userName => _userName;
  @override
  String? get organization => _organization;

  @override
  Future<void> init() async {
    _accessToken = await _secureStorage.read(key: 'access_token');
    _refreshToken = await _secureStorage.read(key: 'refresh_token');
    final expirationStr = await _secureStorage.read(key: 'access_token_expiration');
    if (expirationStr != null) {
      _accessTokenExpiration = DateTime.parse(expirationStr);
    }

    if (_refreshToken != null) {
      await refresh();
    }
  }

  @override
  Future<void> login() async {
    try {
      final bool isDesktop = !foundation.kIsWeb &&
          (foundation.defaultTargetPlatform == foundation.TargetPlatform.linux ||
           foundation.defaultTargetPlatform == foundation.TargetPlatform.windows ||
           foundation.defaultTargetPlatform == foundation.TargetPlatform.macOS);

      if (isDesktop) {
        await _loginDesktop();
      } else {
        await _loginMobile();
      }
    } catch (e) {
      developer.log('Login error', error: e);
    }
  }

  Future<void> _loginMobile() async {
    final AuthorizationTokenResponse? result = await _appAuth.authorizeAndExchangeCode(
      AuthorizationTokenRequest(
        _clientId,
        _redirectUri,
        discoveryUrl: _discoveryUrl,
        scopes: ['profile', 'email'],
      ),
    );

    if (result != null) {
      await _processAuthResult(result);
    }
  }

  Future<void> _loginDesktop() async {
    final issuer = await openid.Issuer.discover(Uri.parse('http://localhost:8081/realms/priceprovider'));
    final client = openid.Client(issuer, _clientId);

    final authenticator = openid.Authenticator(
      client,
      scopes: ['profile', 'email'],
      port: 4000,
      urlLancher: (url) async {
        final uri = Uri.parse(url);
        if (await canLaunchUrl(uri)) {
          await launchUrl(uri, mode: LaunchMode.externalApplication);
        } else {
          developer.log('Could not launch $url');
        }
      },
    );

    final credential = await authenticator.authorize();
    final tokenResponse = await credential.getTokenResponse();

    _accessToken = tokenResponse.accessToken;
    _refreshToken = tokenResponse.refreshToken;
    _accessTokenExpiration = tokenResponse.expiresAt;

    await _saveTokens();
    await _fetchUserInfo();
  }

  @override
  Future<void> logout() async {
    _accessToken = null;
    _refreshToken = null;
    _accessTokenExpiration = null;
    _isLoggedIn = false;
    _userName = null;
    _organization = null;

    await _secureStorage.delete(key: 'access_token');
    await _secureStorage.delete(key: 'refresh_token');
    await _secureStorage.delete(key: 'access_token_expiration');
  }

  @override
  Future<String?> getAccessToken() async {
    if (_accessToken == null) return null;

    if (_accessTokenExpiration != null &&
        DateTime.now().isAfter(_accessTokenExpiration!.subtract(const Duration(seconds: 30)))) {
      await refresh();
    }

    return _accessToken;
  }

  @override
  Future<void> refresh() async {
    if (_refreshToken == null) return;

    try {
      final bool isDesktop = !foundation.kIsWeb &&
          (foundation.defaultTargetPlatform == foundation.TargetPlatform.linux ||
           foundation.defaultTargetPlatform == foundation.TargetPlatform.windows ||
           foundation.defaultTargetPlatform == foundation.TargetPlatform.macOS);

      final TokenResponse? result = await _appAuth.token(
        TokenRequest(
          _clientId,
          isDesktop ? _desktopRedirectUri : _redirectUri,
          discoveryUrl: _discoveryUrl,
          refreshToken: _refreshToken,
          scopes: ['profile', 'email'],
        ),
      );

      if (result != null) {
        await _processTokenResult(result);
      } else {
        await logout();
      }
    } catch (e) {
      developer.log('Refresh error', error: e);
      await logout();
    }
  }

  Future<void> _processAuthResult(AuthorizationTokenResponse result) async {
    _accessToken = result.accessToken;
    _refreshToken = result.refreshToken;
    _accessTokenExpiration = result.accessTokenExpirationDateTime;

    await _saveTokens();
    await _fetchUserInfo();
  }

  Future<void> _processTokenResult(TokenResponse result) async {
    _accessToken = result.accessToken;
    _refreshToken = result.refreshToken;
    _accessTokenExpiration = result.accessTokenExpirationDateTime;

    await _saveTokens();
    await _fetchUserInfo();
  }

  Future<void> _saveTokens() async {
    await _secureStorage.write(key: 'access_token', value: _accessToken);
    await _secureStorage.write(key: 'refresh_token', value: _refreshToken);
    if (_accessTokenExpiration != null) {
      await _secureStorage.write(key: 'access_token_expiration', value: _accessTokenExpiration!.toIso8601String());
    }
  }

  Future<void> _fetchUserInfo() async {
    if (_accessToken == null) return;

    try {
      // Typically you'd get this from the ID token or a userInfo endpoint
      final discoveryResponse = await http.get(Uri.parse(_discoveryUrl));
      final discoveryData = json.decode(discoveryResponse.body);
      final userInfoEndpoint = discoveryData['userinfo_endpoint'];

      final response = await http.get(
        Uri.parse(userInfoEndpoint),
        headers: {'Authorization': 'Bearer $_accessToken'},
      );

      if (response.statusCode == 200) {
        final userInfo = json.decode(response.body);
        _userName = userInfo['preferred_username'] ?? userInfo['name'];
        _isLoggedIn = true;

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
    } catch (e) {
      developer.log('Error fetching user info', error: e);
    }
  }
}

AuthService createAuthService() => AuthServiceImpl();
