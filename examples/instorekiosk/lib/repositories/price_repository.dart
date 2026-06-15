import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/models.dart';

class PriceRepository {
  final String baseUrl;
  final String channelId;
  final String countryKey;
  final String priceType;

  PriceRepository({
    required this.baseUrl,
    required this.channelId,
    required this.countryKey,
    required this.priceType,
  });

  Future<Price> fetchPrice(String sku, int quantity, {String? token}) async {
    final url = Uri.parse(
      '$baseUrl/public/api/$channelId/$countryKey/pricerows/$priceType/of/$sku'
      '?quantity=$quantity&unit=piece&currency=USD',
    );

    final headers = <String, String>{
      'Accept': 'application/json',
    };

    if (token != null) {
      headers['Authorization'] = 'Bearer $token';
    }

    final response = await http.get(url, headers: headers);

    if (response.statusCode == 200) {
      return Price.fromJson(json.decode(response.body));
    } else if (response.statusCode == 404) {
      throw Exception('No price found for this product.');
    } else {
      throw Exception('Failed to load price: ${response.statusCode}');
    }
  }
}
