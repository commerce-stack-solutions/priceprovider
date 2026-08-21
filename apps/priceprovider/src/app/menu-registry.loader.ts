import { inject, Injectable } from '@angular/core';
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

interface MenuRegistryConfiguration {
  entities?: MenuRegistryEntityDefinition[];
  menuItems?: MenuRegistryItemDefinition[];
}

@Injectable({ providedIn: 'root' })
export class MenuRegistryLoader {
  private http = inject(HttpClient);
  private registry = inject(RegistryService);

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
