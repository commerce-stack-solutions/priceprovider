import { Injectable, Type } from '@angular/core';

export interface CustomView {
  list?: Type<any>;
  detail?: Type<any>;
  form?: Type<any>;
}

export interface SidebarMenuItem {
  key: string;
  path: string;
  type?: string;
  section?: string;
  labelKey?: string;
  label?: string;
  icon?: string;
  permission?: string;
  permissionMode?: 'entityRead' | 'permission';
}

export interface SidebarMenuSection {
  name: string;
  items: SidebarMenuItem[];
}

interface RegisteredEntityType {
  type: string;
  routePrefix?: string;
  navigationPath?: string;
  menuSection?: string;
  labelKey?: string;
  icon?: string;
  permission?: string;
  permissionMode?: 'entityRead' | 'permission';
}

const OTHER_TYPES_SECTION = 'Other Types';
const DEFAULT_ENTITY_TYPES: Array<[string, string, string, string, string]> = [
  ['Channel', 'channels', 'Commerce Management', 'channels', 'bi bi-broadcast'],
  ['PriceRow', 'pricerows', 'Commerce Management', 'priceRows', 'bi bi-card-list'],
  ['TaxClass', 'taxclasses', 'Commerce Management', 'taxClasses', 'bi bi-percent'],
  ['Organization', 'organizations', 'Organizations & Groups', 'organizations', 'bi bi-building'],
  ['Group', 'groups', 'Organizations & Groups', 'groups', 'bi bi-diagram-3'],
  ['Country', 'countries', 'Master Data', 'countries', 'bi bi-flag'],
  ['Currency', 'currencies', 'Master Data', 'currencies', 'bi bi-currency-exchange'],
  ['Unit', 'units', 'Master Data', 'units', 'bi bi-box'],
  ['Language', 'languages', 'Master Data', 'languages', 'bi bi-translate'],
  ['AppRole', 'app-roles', 'System & Access Management', 'appRoles', 'bi bi-person-badge'],
  ['AppPermission', 'app-permissions', 'System & Access Management', 'appPermissions', 'bi bi-shield-check']
];

@Injectable({
  providedIn: 'root'
})
export class RegistryService {
  private customViews = new Map<string, CustomView>();
  private registeredEntityTypes = new Map<string, RegisteredEntityType>();
  private routePrefixToType = new Map<string, string>();
  private customMenuItems = new Map<string, SidebarMenuItem>();

  constructor() {
    DEFAULT_ENTITY_TYPES.forEach(([type, routePrefix, menuSection, labelKey, icon]) =>
      this.registerEntityType({ type, routePrefix, menuSection, labelKey, icon })
    );

    this.registerMenuItem({
      key: 'service-initialization',
      path: 'service-initialization',
      section: 'System & Access Management',
      labelKey: 'serviceInitialization',
      icon: 'bi bi-gear',
      permission: 'priceprovider.admin:ServiceInitialization:write',
      permissionMode: 'permission'
    });
  }

  registerCustomView(type: string, view: CustomView) {
    this.customViews.set(type.toLowerCase(), view);
  }

  getCustomView(type: string): CustomView | undefined {
    return this.customViews.get(type.toLowerCase());
  }

  registerEntityType(entity: RegisteredEntityType & { routePrefix: string }) {
    const key = entity.type.toLowerCase();
    const existing = this.registeredEntityTypes.get(key);
    const registered: RegisteredEntityType = {
      type: existing?.type ?? entity.type,
      routePrefix: entity.routePrefix,
      navigationPath: existing?.navigationPath ?? entity.routePrefix,
      menuSection: entity.menuSection ?? existing?.menuSection,
      labelKey: entity.labelKey ?? existing?.labelKey,
      icon: entity.icon ?? existing?.icon,
      permission: entity.permission ?? existing?.permission ?? entity.type,
      permissionMode: entity.permissionMode ?? existing?.permissionMode ?? 'entityRead'
    };

    this.registeredEntityTypes.set(key, registered);
    this.routePrefixToType.set(entity.routePrefix.toLowerCase(), registered.type);
  }

  registerMenuSection(type: string, section: string) {
    const key = type.toLowerCase();
    const existing = this.registeredEntityTypes.get(key);
    this.registeredEntityTypes.set(key, {
      type: existing?.type ?? type,
      routePrefix: existing?.routePrefix,
      navigationPath: existing?.navigationPath,
      menuSection: section,
      labelKey: existing?.labelKey,
      icon: existing?.icon,
      permission: existing?.permission ?? type,
      permissionMode: existing?.permissionMode ?? 'entityRead'
    });
  }

  getMenuSection(type: string): string | undefined {
    return this.registeredEntityTypes.get(type.toLowerCase())?.menuSection;
  }

  registerMenuSectionAssignments(assignments: Record<string, string>) {
    Object.entries(assignments).forEach(([type, section]) => this.registerMenuSection(type, section));
  }

  getMenuSections(): Map<string, string> {
    return new Map(
      Array.from(this.registeredEntityTypes.values())
        .filter(entity => !!entity.menuSection)
        .map(entity => [entity.type.toLowerCase(), entity.menuSection!])
    );
  }

  registerRoutePrefix(type: string, prefix: string) {
    const key = type.toLowerCase();
    const existing = this.registeredEntityTypes.get(key);
    this.registeredEntityTypes.set(key, {
      type: existing?.type ?? type,
      routePrefix: prefix,
      navigationPath: existing?.navigationPath ?? `generic/${prefix}`,
      menuSection: existing?.menuSection,
      labelKey: existing?.labelKey,
      icon: existing?.icon,
      permission: existing?.permission ?? type,
      permissionMode: existing?.permissionMode ?? 'entityRead'
    });
    this.routePrefixToType.set(prefix.toLowerCase(), existing?.type ?? type);
  }

  getRoutePrefix(type: string): string | undefined {
    return this.registeredEntityTypes.get(type.toLowerCase())?.routePrefix;
  }

  getEntityTypeFromPrefix(prefix: string): string | undefined {
    return this.routePrefixToType.get(prefix.toLowerCase());
  }

  registerMenuItem(item: SidebarMenuItem) {
    this.customMenuItems.set(item.key.toLowerCase(), item);
  }

  getSidebarMenuItems(): SidebarMenuItem[] {
    const entityItems = Array.from(this.registeredEntityTypes.values())
      .filter(entity => !!entity.routePrefix)
      .map(entity => ({
        key: entity.type.toLowerCase(),
        type: entity.type,
        path: entity.navigationPath ?? entity.routePrefix!,
        section: entity.menuSection,
        labelKey: entity.labelKey,
        icon: entity.icon,
        permission: entity.permission ?? entity.type,
        permissionMode: entity.permissionMode ?? 'entityRead'
      }));

    return [...entityItems, ...this.customMenuItems.values()];
  }

  getSidebarMenuSections(): SidebarMenuSection[] {
    const sections = new Map<string, SidebarMenuItem[]>();

    this.getSidebarMenuItems().forEach(item => {
      const section = item.section?.trim() || OTHER_TYPES_SECTION;
      const items = sections.get(section) ?? [];
      items.push(item);
      sections.set(section, items);
    });

    return Array.from(sections.entries()).map(([name, items]) => ({ name, items }));
  }

  getOtherTypesSectionName(): string {
    return OTHER_TYPES_SECTION;
  }
}
