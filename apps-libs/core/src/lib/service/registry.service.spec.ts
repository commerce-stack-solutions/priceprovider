import { RegistryService } from './registry.service';

class TestListComponent {}

const DEFAULT_MENU_CONFIGURATION = {
  entities: [
    { type: 'Channel', routePrefix: 'channels', menuSection: 'Commerce Management', icon: 'bi bi-broadcast' },
    { type: 'PriceRow', routePrefix: 'pricerows', menuSection: 'Commerce Management', icon: 'bi bi-card-list' },
    { type: 'TaxClass', routePrefix: 'taxclasses', menuSection: 'Commerce Management', icon: 'bi bi-percent' },
    { type: 'Organization', routePrefix: 'organizations', menuSection: 'Organizations & Groups', icon: 'bi bi-building' },
    { type: 'Group', routePrefix: 'groups', menuSection: 'Organizations & Groups', icon: 'bi bi-diagram-3' },
    { type: 'Country', routePrefix: 'countries', menuSection: 'Master Data', icon: 'bi bi-flag' },
    { type: 'Currency', routePrefix: 'currencies', menuSection: 'Master Data', icon: 'bi bi-currency-exchange' },
    { type: 'Unit', routePrefix: 'units', menuSection: 'Master Data', icon: 'bi bi-box' },
    { type: 'Language', routePrefix: 'languages', menuSection: 'Master Data', icon: 'bi bi-translate' },
    { type: 'AppRole', routePrefix: 'app-roles', menuSection: 'System & Access Management', icon: 'bi bi-person-badge' },
    { type: 'AppPermission', routePrefix: 'app-permissions', menuSection: 'System & Access Management', icon: 'bi bi-shield-check' }
  ],
  menuItems: [
    {
      key: 'service-initialization',
      path: 'service-initialization',
      section: 'System & Access Management',
      label: 'Service Initialization',
      icon: 'bi bi-gear',
      permission: 'priceprovider.admin:ServiceInitialization:write',
      permissionMode: 'permission' as const
    }
  ]
};

describe('RegistryService', () => {
  let service: RegistryService;

  beforeEach(() => {
    service = new RegistryService();
    service.registerMenuConfiguration(DEFAULT_MENU_CONFIGURATION);
  });

  it('groups registered types by their configured menu section', () => {
    const sections = service.getSidebarMenuSections();
    const systemSection = sections.find(section => section.name === 'System & Access Management');

    expect(systemSection?.items.map(item => item.path)).toContain('app-roles');
    expect(systemSection?.items.map(item => item.path)).toContain('app-permissions');
    expect(systemSection?.items.map(item => item.path)).toContain('service-initialization');
  });

  it('places unassigned types into the other types section', () => {
    service.registerRoutePrefix('CustomType', 'custom-types');

    const sections = service.getSidebarMenuSections();
    const fallbackSection = sections.find(section => section.name === service.getOtherTypesSectionName());

    expect(fallbackSection?.items.map(item => item.path)).toContain('generic/custom-types');
  });

  it('supports bulk menu-section assignments and custom view lookup', () => {
    service.registerRoutePrefix('CustomType', 'custom-types');
    service.registerMenuSectionAssignments({ CustomType: 'Custom Section' });
    service.registerCustomView('CustomType', { list: TestListComponent });

    const sections = service.getSidebarMenuSections();
    const customSection = sections.find(section => section.name === 'Custom Section');

    expect(customSection?.items.map(item => item.path)).toContain('generic/custom-types');
    expect(service.getCustomView('customtype')?.list).toBe(TestListComponent);
  });
});
