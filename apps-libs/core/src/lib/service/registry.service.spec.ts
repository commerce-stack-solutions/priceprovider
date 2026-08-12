import { RegistryService } from './registry.service';

class TestListComponent {}

describe('RegistryService', () => {
  let service: RegistryService;

  beforeEach(() => {
    service = new RegistryService();
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
