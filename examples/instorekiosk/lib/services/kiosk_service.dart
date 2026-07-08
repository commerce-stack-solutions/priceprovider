import 'package:flutter/foundation.dart';
import '../models/models.dart';
import '../repositories/price_repository.dart';
import 'auth/auth_service.dart';

class KioskService extends ChangeNotifier {
  final PriceRepository _priceRepository;
  final _authService = createAuthService();

  Product? _selectedProduct;
  int _quantity = 1;
  Price? _currentPrice;
  String? _rawPriceData;
  bool _isLoading = false;
  String? _error;

  KioskService(this._priceRepository);

  Product? get selectedProduct => _selectedProduct;
  int get quantity => _quantity;
  Price? get currentPrice => _currentPrice;
  String? get rawPriceData => _rawPriceData;
  bool get isLoading => _isLoading;
  String? get error => _error;
  bool get isLoggedIn => _authService.isLoggedIn;
  String? get userName => _authService.userName;
  String? get organization => _authService.organization;

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
    await _authService.init();
    await updatePrice();
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
      final token = await _authService.getAccessToken();

      final response = await _priceRepository.fetchPrice(
        _selectedProduct!.sku,
        _quantity,
        token: token,
      );
      _currentPrice = response.price;
      _rawPriceData = response.rawJson;
    } catch (e) {
      _error = e.toString();
      _currentPrice = null;
      _rawPriceData = null;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> login() async {
    await _authService.login();
    notifyListeners();
    await updatePrice();
  }

  Future<void> logout() async {
    await _authService.logout();
    notifyListeners();
    await updatePrice();
  }
}
