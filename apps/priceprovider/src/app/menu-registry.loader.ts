import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { RegistryConfiguration, RegistryService } from 'core';

@Injectable({ providedIn: 'root' })
export class MenuRegistryLoader {
  private http = inject(HttpClient);
  private registry = inject(RegistryService);

  async load(): Promise<void> {
    const configuration = await firstValueFrom(
      this.http.get<RegistryConfiguration>('/assets/config/menu-registry.json')
    );

    this.registry.registerMenuConfiguration(configuration);
  }
}
