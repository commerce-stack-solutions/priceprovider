export 'auth_service_base.dart'
    if (dart.library.js_interop) 'auth_service_web.dart'
    if (dart.library.io) 'auth_service_stub.dart';
