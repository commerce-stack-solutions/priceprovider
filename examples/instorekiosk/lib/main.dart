import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'pages/kiosk_page.dart';
import 'repositories/price_repository.dart';
import 'services/kiosk_service.dart';

void main() {
  final priceRepository = PriceRepository(
    baseUrl: 'http://localhost:8080',
    channelId: 'global-b2b-sales-channel',
    countryKey: 'US',
    priceType: 'SALES_PRICE',
  );

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider(
          create: (_) => KioskService(priceRepository)..init(),
        ),
      ],
      child: const KioskApp(),
    ),
  );
}

class KioskApp extends StatelessWidget {
  const KioskApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'In-Store Kiosk',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF1A1A2E),
          primary: const Color(0xFF1A1A2E),
          secondary: const Color(0xFFE94560),
        ),
        useMaterial3: true,
        fontFamily: 'Segoe UI',
      ),
      home: const KioskPage(),
      debugShowCheckedModeBanner: false,
    );
  }
}
