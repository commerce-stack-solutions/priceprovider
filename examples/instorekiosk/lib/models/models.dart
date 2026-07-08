class Product {
  final String sku;
  final String name;
  final String description;
  final String icon;

  Product({
    required this.sku,
    required this.name,
    required this.description,
    required this.icon,
  });
}

class Price {
  final double value;
  final String currency;
  final bool taxIncluded;
  final String? validFrom;
  final String? validTo;

  Price({
    required this.value,
    required this.currency,
    required this.taxIncluded,
    this.validFrom,
    this.validTo,
  });

  factory Price.fromJson(Map<String, dynamic> json) {
    return Price(
      value: (json['priceValue'] ?? json['price'] ?? 0.0).toDouble(),
      currency: json['currency'] ?? json['currencyRef'] ?? '',
      taxIncluded: json['taxIncluded'] ?? false,
      validFrom: json['validFrom'],
      validTo: json['validTo'],
    );
  }
}
