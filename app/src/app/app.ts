import { Component, signal, ViewContainerRef, inject, EnvironmentInjector, AfterViewInit, computed } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './components/header/header.component';
import { SidebarComponent } from './components/sidebar/sidebar.component';
import { ModalService } from './service/modal.service';
import { AuthService } from './service/auth.service';
import { PermissionService } from './service/permission.service';
import { TranslocoModule } from '@jsverse/transloco';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, HeaderComponent, SidebarComponent, TranslocoModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
  standalone: true
})
export class App implements AfterViewInit {
  private modalService = inject(ModalService);
  private viewContainerRef = inject(ViewContainerRef);
  private environmentInjector = inject(EnvironmentInjector);
  protected authService = inject(AuthService);
  protected permissionService = inject(PermissionService);
  
  protected readonly title = signal('pricemanager-app');
  sidebarVisible = signal(true);

  /**
   * Sidebar should only be visible if:
   * 1. The user has toggled it on (sidebarVisible() is true)
   * 2. The user is authenticated
   * 3. The user has admin permissions (otherwise there's nothing to show in sidebar)
   */
  isSidebarEffectivelyVisible = computed(() =>
    this.sidebarVisible() &&
    this.authService.isAuthenticated() &&
    this.permissionService.hasAdminPermission()
  );

  ngAfterViewInit(): void {
    this.modalService.setContainer(this.viewContainerRef, this.environmentInjector);
  }

  toggleSidebar(): void {
    this.sidebarVisible.update(v => !v);
  }
}