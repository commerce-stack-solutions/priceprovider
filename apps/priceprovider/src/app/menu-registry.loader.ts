import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { RegistryService } from 'core';

interface MenuRegistryEntityDefinition {
  type: string;
  routePrefix: string;
  navigationPath?: string;
  menuSection?: string;
  labelKey?: string;
  icon?: string;
  permission?: string;
  permissionMode?: 'entityRead' | 'permission';
}

interface MenuRegistryItemDefinition {
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

interface SidebarMenuSectionLike {
  name: string;
  items: MenuRegistryItemDefinition[];
}

interface MenuRegistryConfiguration {
  entities?: MenuRegistryEntityDefinition[];
  menuItems?: MenuRegistryItemDefinition[];
}

const OTHER_TYPES_SECTION = 'Other Types';

@Injectable({ providedIn: 'root' })
export class MenuRegistryLoader {
  private http = inject(HttpClient);
  private registry = inject(RegistryService);
  readonly sidebarMenuItems = signal<MenuRegistryItemDefinition[]>([]);
  readonly sidebarMenuSections = computed<SidebarMenuSectionLike[]>(() => {
    const sections = new Map<string, MenuRegistryItemDefinition[]>();

    this.sidebarMenuItems().forEach(item => {
      const section = item.section?.trim() || OTHER_TYPES_SECTION;
      const items = sections.get(section) ?? [];
      items.push(item);
      sections.set(section, items);
    });

    return Array.from(sections.entries()).map(([name, items]) => ({ name, items }));
  });

  async load(): Promise<void> {
    const registry = this.registry as RegistryService & {
      registerEntityType?: (entity: MenuRegistryEntityDefinition) => void;
      registerMenuItem?: (item: MenuRegistryItemDefinition) => void;
      registerRoutePrefix?: (type: string, prefix: string) => void;
      registerMenuSection?: (type: string, section: string) => void;
    };
    const configuration = await firstValueFrom(
      this.http.get<MenuRegistryConfiguration>('/assets/config/menu-registry.json')
    );
    this.sidebarMenuItems.set([
      ...(configuration.entities?.map(entity => ({
        key: entity.type.toLowerCase(),
        type: entity.type,
        path: entity.navigationPath ?? entity.routePrefix,
        section: entity.menuSection,
        labelKey: entity.labelKey,
        icon: entity.icon,
        permission: entity.permission ?? entity.type,
        permissionMode: entity.permissionMode ?? 'entityRead'
      })) ?? []),
      ...(configuration.menuItems ?? [])
    ]);

    configuration.entities?.forEach(entity => {
      if (registry.registerEntityType) {
        registry.registerEntityType(entity);
        return;
      }

      registry.registerRoutePrefix?.(entity.type, entity.routePrefix);
      if (entity.menuSection) {
        registry.registerMenuSection?.(entity.type, entity.menuSection);
      }
    });
    configuration.menuItems?.forEach(item => registry.registerMenuItem?.(item));
  }
}
