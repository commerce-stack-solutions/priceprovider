import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/models.dart';
import '../services/kiosk_service.dart';

class KioskPage extends StatelessWidget {
  const KioskPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('🛒 In-Store Kiosk'),
        backgroundColor: const Color(0xFF1A1A2E),
        foregroundColor: Colors.white,
        actions: const [
          _UserInfo(),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Center(
          child: Container(
            constraints: const BoxConstraints(maxWidth: 800),
            child: const Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                _ProductCard(),
                SizedBox(height: 24),
                _StatusBar(),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _UserInfo extends StatelessWidget {
  const _UserInfo();

  @override
  Widget build(BuildContext context) {
    final service = context.watch<KioskService>();
    if (service.isLoggedIn) {
      return Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text('👤 ${service.userName}', style: const TextStyle(fontSize: 14)),
          const SizedBox(width: 8),
          TextButton(
            onPressed: service.logout,
            child: const Text('Logout', style: TextStyle(color: Colors.white70)),
          ),
          const SizedBox(width: 8),
        ],
      );
    } else {
      return Padding(
        padding: const EdgeInsets.only(right: 16.0),
        child: ElevatedButton(
          onPressed: service.login,
          style: ElevatedButton.styleFrom(
            backgroundColor: const Color(0xFFE94560),
            foregroundColor: Colors.white,
          ),
          child: const Text('Login'),
        ),
      );
    }
  }
}

class _ProductCard extends StatefulWidget {
  const _ProductCard();

  @override
  State<_ProductCard> createState() => _ProductCardState();
}

class _ProductCardState extends State<_ProductCard> {
  late TextEditingController _quantityController;

  @override
  void initState() {
    super.initState();
    final service = Provider.of<KioskService>(context, listen: false);
    _quantityController = TextEditingController(text: service.quantity.toString());
  }

  @override
  void dispose() {
    _quantityController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final service = context.watch<KioskService>();
    final product = service.selectedProduct;

    if (product == null) return const SizedBox.shrink();

    // Sync controller with service state if it changes from outside
    if (_quantityController.text != service.quantity.toString()) {
      _quantityController.text = service.quantity.toString();
    }

    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: IntrinsicHeight(
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Expanded(
              flex: 1,
              child: Container(
                color: Colors.grey[200],
                child: Center(
                  child: Text(
                    product.icon,
                    style: const TextStyle(fontSize: 80),
                  ),
                ),
              ),
            ),
            Expanded(
              flex: 2,
              child: Padding(
                padding: const EdgeInsets.all(24.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      product.name,
                      style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      product.description,
                      style: TextStyle(color: Colors.grey[600], height: 1.5),
                    ),
                    const SizedBox(height: 24),
                    const Text('Select Version:', style: TextStyle(fontWeight: FontWeight.bold)),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 8,
                      children: service.products.map((p) {
                        final isSelected = p.sku == product.sku;
                        return ChoiceChip(
                          label: Text(p.sku.split('-').last),
                          selected: isSelected,
                          onSelected: (selected) {
                            if (selected) service.selectProduct(p);
                          },
                          selectedColor: const Color(0xFFFFEBF0),
                          labelStyle: TextStyle(
                            color: isSelected ? const Color(0xFFE94560) : Colors.black,
                          ),
                        );
                      }).toList(),
                    ),
                    const SizedBox(height: 24),
                    Row(
                      children: [
                        const Text('Quantity:', style: TextStyle(fontWeight: FontWeight.bold)),
                        const SizedBox(width: 16),
                        SizedBox(
                          width: 80,
                          child: TextField(
                            decoration: const InputDecoration(
                              isDense: true,
                              border: OutlineInputBorder(),
                            ),
                            keyboardType: TextInputType.number,
                            controller: _quantityController,
                            onSubmitted: (value) {
                              final q = int.tryParse(value);
                              if (q != null) service.setQuantity(q);
                            },
                          ),
                        ),
                        const SizedBox(width: 12),
                        OutlinedButton(
                          onPressed: () {
                            final q = int.tryParse(_quantityController.text);
                            if (q != null) service.setQuantity(q);
                          },
                          style: OutlinedButton.styleFrom(
                            foregroundColor: const Color(0xFF1A1A2E),
                            side: const BorderSide(color: Color(0xFF1A1A2E)),
                          ),
                          child: const Text('Update Price'),
                        ),
                      ],
                    ),
                    const SizedBox(height: 24),
                    const _AuthNotice(),
                    const SizedBox(height: 16),
                    const _PriceBox(),
                    const SizedBox(height: 24),
                    ElevatedButton(
                      onPressed: () {},
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFFE94560),
                        foregroundColor: Colors.white,
                        minimumSize: const Size(double.infinity, 50),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                      ),
                      child: const Text('Add to Cart', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _AuthNotice extends StatelessWidget {
  const _AuthNotice();

  @override
  Widget build(BuildContext context) {
    final service = context.watch<KioskService>();

    if (service.isLoggedIn) {
      return Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: const Color(0xFFD1ECF1),
          border: Border.all(color: const Color(0xFF17A2B8)),
          borderRadius: BorderRadius.circular(6),
        ),
        child: Text(
          service.organization != null
              ? 'Organization pricing active: Showing prices for ${service.organization}.'
              : 'Authenticated: Showing prices for your account.',
          style: const TextStyle(fontSize: 13, color: Color(0xFF0C5460)),
        ),
      );
    } else {
      return Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: const Color(0xFFFFF3CD),
          border: Border.all(color: const Color(0xFFFFC107)),
          borderRadius: BorderRadius.circular(6),
        ),
        child: Row(
          children: [
            const Expanded(
              child: Text(
                'Public prices are available anonymously.',
                style: TextStyle(fontSize: 13, color: Color(0xFF856404)),
              ),
            ),
            TextButton(
              onPressed: service.login,
              child: const Text('Login', style: TextStyle(fontWeight: FontWeight.bold)),
            ),
          ],
        ),
      );
    }
  }
}

class _PriceBox extends StatelessWidget {
  const _PriceBox();

  @override
  Widget build(BuildContext context) {
    final service = context.watch<KioskService>();

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.grey[50],
        border: Border.all(color: Colors.grey[200]!),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Your Price (per piece)', style: TextStyle(fontSize: 12, color: Colors.grey, fontWeight: FontWeight.w500)),
          if (service.isLoading)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 8.0),
              child: SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2)),
            )
          else if (service.error != null)
            Text(service.error!, style: const TextStyle(color: Colors.red, fontSize: 14))
          else if (service.currentPrice != null)
            Row(
              crossAxisAlignment: CrossAxisAlignment.baseline,
              textBaseline: TextBaseline.alphabetic,
              children: [
                Text(
                  service.currentPrice!.value.toStringAsFixed(2),
                  style: const TextStyle(fontSize: 32, fontWeight: FontWeight.bold, color: Color(0xFF1A1A2E)),
                ),
                const SizedBox(width: 4),
                Text(
                  service.currentPrice!.currency,
                  style: const TextStyle(fontSize: 16, color: Colors.grey),
                ),
              ],
            ),
          if (service.currentPrice != null)
            Text(
              '${service.currentPrice!.taxIncluded ? "incl. VAT" : "excl. VAT"} · ${_formatValidity(service.currentPrice!)}',
              style: const TextStyle(fontSize: 12, color: Colors.grey),
            ),
        ],
      ),
    );
  }

  String _formatValidity(Price price) {
    if (price.validFrom != null && price.validTo != null) {
      return 'Valid: ${price.validFrom} - ${price.validTo}';
    } else if (price.validFrom != null) {
      return 'Valid from: ${price.validFrom}';
    }
    return '';
  }
}

class _StatusBar extends StatelessWidget {
  const _StatusBar();

  @override
  Widget build(BuildContext context) {
    final service = context.watch<KioskService>();
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.05), blurRadius: 8)],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Text('Status: ', style: TextStyle(fontWeight: FontWeight.bold)),
              Text(service.isLoggedIn ? 'Logged in' : 'Not logged in'),
            ],
          ),
          if (service.isLoggedIn) ...[
            const SizedBox(height: 8),
            Text('User: ${service.userName}', style: const TextStyle(fontSize: 12)),
            if (service.organization != null)
              Text('Org: ${service.organization}', style: const TextStyle(fontSize: 12)),
          ],
          if (service.rawPriceData != null) ...[
            const SizedBox(height: 16),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: Colors.grey[200],
                borderRadius: BorderRadius.circular(4),
              ),
              child: SelectableText(
                service.rawPriceData!,
                style: const TextStyle(fontFamily: 'monospace', fontSize: 11),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
