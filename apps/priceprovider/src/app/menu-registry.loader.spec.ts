import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { MenuRegistryLoader } from './menu-registry.loader';
import { RegistryService } from 'core';

describe('MenuRegistryLoader', () => {
  let loader: MenuRegistryLoader;
  let httpTestingController: HttpTestingController;
  let registry: RegistryService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), MenuRegistryLoader, RegistryService]
    });

    loader = TestBed.inject(MenuRegistryLoader);
    httpTestingController = TestBed.inject(HttpTestingController);
    registry = TestBed.inject(RegistryService);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('loads the app menu registry configuration into the shared registry', async () => {
    const loadPromise = loader.load();

    const request = httpTestingController.expectOne('/assets/config/menu-registry.json');
    request.flush({
      entities: [{ type: 'AuditLog', routePrefix: 'audit-logs', menuSection: 'Monitoring' }],
      menuItems: [{ key: 'service-initialization', path: 'service-initialization', section: 'System', label: 'Service Initialization' }]
    });

    await loadPromise;

    expect(registry.getRoutePrefix('AuditLog')).toBe('audit-logs');
    expect(registry.getMenuSection('AuditLog')).toBe('Monitoring');
    expect(registry.getSidebarMenuItems().map(item => item.key)).toContain('service-initialization');
    expect(loader.sidebarMenuSections().map(section => section.name)).toContain('Monitoring');
    expect(loader.sidebarMenuSections().flatMap(section => section.items).map(item => item.key)).toContain('auditlog');
  });
});
