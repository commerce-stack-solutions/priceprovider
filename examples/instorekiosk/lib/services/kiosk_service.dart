import 'package:flutter/foundation.dart';
import 'package:openid_client/openid_client_browser.dart';
import '../models/models.dart';
import '../repositories/price_repository.dart';

class KioskService extends ChangeNotifier {
  final PriceRepository _priceRepository;

  Product? _selectedProduct;
  int _quantity = 1;
  Price? _currentPrice;
  bool _isLoading = false;
  String? _error;

  // Auth state
  Authenticator? _authenticator;
  Credential? _credential;
  bool _isLoggedIn = false;
  String? _userName;
  String? _organization;

  KioskService(this._priceRepository);

  Product? get selectedProduct => _selectedProduct;
  int get quantity => _quantity;
  Price? get currentPrice => _currentPrice;
  bool get isLoading => _isLoading;
  String? get error => _error;
  bool get isLoggedIn => _isLoggedIn;
  String? get userName => _userName;
  String? get organization => _organization;

  final List<Product> products = [
    Product(
      sku: 'TOOL-TORQUE-25NM',
      name: 'Industrial Torque Wrench 25Nm',
      description: 'Professional-grade torque wrench for precision tightening. Features a dual-direction mechanism, ergonomic grip and ±4% accuracy.',
      icon: '🔧',
    ),
    Product(
      sku: 'TOOL-TORQUE-50NM',
      name: 'Industrial Torque Wrench 50Nm',
      description: 'Professional-grade torque wrench for precision tightening. Features a dual-direction mechanism, ergonomic grip and ±4% accuracy.',
      icon: '🔧',
    ),
    Product(
      sku: 'TOOL-TORQUE-100NM',
      name: 'Industrial Torque Wrench 100Nm',
      description: 'Professional-grade torque wrench for precision tightening. Features a dual-direction mechanism, ergonomic grip and ±4% accuracy.',
      icon: '🔧',
    ),
  ];

  Future<void> init() async {
    _selectedProduct = products.first;
    await _initAuth();
    await updatePrice();
  }

  Future<void> _initAuth() async {
    if (kIsWeb) {
      final issuer = await Issuer.discover(Uri.parse('http://localhost:8081/realms/priceprovider'));
      final client = Client(issuer, 'instorekiosk');
      _authenticator = Authenticator(client, scopes: ['openid', 'profile', 'email']);

      _credential = await _authenticator!.credential;
      if (_credential != null) {
        final userInfo = await _credential!.getUserInfo();
        _isLoggedIn = true;
        _userName = userInfo.preferredUsername ?? userInfo.name;

        // Extract organization from groups
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
        notifyListeners();
      }
    }
  }

  void selectProduct(Product product) {
    _selectedProduct = product;
    updatePrice();
  }

  void setQuantity(int quantity) {
    if (quantity > 0) {
      _quantity = quantity;
      updatePrice();
    }
  }

  Future<void> updatePrice() async {
    if (_selectedProduct == null) return;

    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      String? token;
      if (_credential != null) {
        final t = await _credential!.getTokenResponse();
        token = t.accessToken;
      }

      _currentPrice = await _priceRepository.fetchPrice(
        _selectedProduct!.sku,
        _quantity,
        token: token,
      );
    } catch (e) {
      _error = e.toString();
      _currentPrice = null;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  void login() {
    _authenticator?.authorize();
  }

  void logout() {
    // Simple local logout, Keycloak logout would need a redirect
    _credential = null;
    _isLoggedIn = false;
    _userName = null;
    _organization = null;
    updatePrice();
  }
}
