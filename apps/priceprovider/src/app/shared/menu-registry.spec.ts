import { getVisibleMenuSections } from './menu-registry';

describe('menu registry helpers', () => {
  it('uses fallback sections when the shared registry has no sections yet', () => {
    const sections = getVisibleMenuSections(
      { getSidebarMenuSections: () => [] } as any,
      {
        hasPermission: () => true,
        hasReadPermission: () => true
      } as any,
      [
        {
          name: 'Monitoring',
          items: [{ key: 'auditlog', path: 'audit-logs', type: 'AuditLog' }]
        }
      ]
    );

    expect(sections.map(section => section.name)).toEqual(['Monitoring']);
    expect(sections[0].items.map(item => item.key)).toEqual(['auditlog']);
  });
});
