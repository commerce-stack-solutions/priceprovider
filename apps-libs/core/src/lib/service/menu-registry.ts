import { LabelService } from './label.service';
import { PermissionService } from './permission.service';
import { RegistryService, SidebarMenuItem, SidebarMenuSection } from './registry.service';

const SECTION_LABELS: Record<string, string> = {
  'Commerce Management': 'components.sidebar.categories.commerceManagement',
  'Organizations & Groups': 'components.sidebar.categories.organizationsGroups',
  'Master Data': 'components.sidebar.categories.masterData',
  'System & Access Management': 'components.sidebar.categories.systemAccessManagement'
};

export function getVisibleMenuSections(
  registry: RegistryService,
  permissionService: PermissionService
): SidebarMenuSection[] {
  return registry
    .getSidebarMenuSections()
    .map(section => ({
      ...section,
      items: section.items.filter(item => canDisplay(item, permissionService))
    }))
    .filter(section => section.items.length > 0);
}

export function getSectionLabel(sectionName: string): string {
  return SECTION_LABELS[sectionName] ?? sectionName;
}

export function getItemLabel(labelService: LabelService, item: SidebarMenuItem): string {
  return item.label ?? labelService.formatLabel(item.type ?? item.path.replace(/-/g, ' '));
}

export function getItemIcon(item: SidebarMenuItem): string {
  return item.icon ?? 'bi bi-card-list';
}

export function getLink(lang: string, item: SidebarMenuItem): string[] {
  return ['/' + lang, ...item.path.split('/').filter(Boolean)];
}

function canDisplay(item: SidebarMenuItem, permissionService: PermissionService): boolean {
  if (item.permissionMode === 'permission' && item.permission) {
    return permissionService.hasPermission(item.permission);
  }

  if (item.type) {
    return permissionService.hasReadPermission(item.type);
  }

  return item.permission ? permissionService.hasPermission(item.permission) : true;
}
