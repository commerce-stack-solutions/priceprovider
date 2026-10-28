import { Component, inject, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { LabelService, PermissionService, RegistryService, SessionService } from 'core';
import { MenuRegistryLoader } from '../../menu-registry.loader';
import { getItemLabel, getLink, getSectionLabel, getVisibleMenuSections } from '../../shared/menu-registry';

@Component({
  selector: 'app-home',
  templateUrl: './home.html',
  styleUrls: ['./home.scss'],
  standalone: true,
  imports: [RouterLink, TranslocoModule]
})
export class HomeComponent {
  private labelService = inject(LabelService);
  private menuRegistryLoader = inject(MenuRegistryLoader);
  private permissionService = inject(PermissionService);
  private registry = inject(RegistryService);
  private sessionService = inject(SessionService);
  lang = computed(() => this.sessionService.language());

  sections = computed(() =>
    getVisibleMenuSections(this.registry, this.permissionService, this.menuRegistryLoader.sidebarMenuSections())
  );
  protected readonly getSectionLabel = getSectionLabel;
  protected getItemLabel = (item: any) => getItemLabel(this.labelService, item);
  protected getLink = (item: any) => getLink(this.lang(), item);
}
