import { Component, OnInit, Injector, Type, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { RegistryService } from '../../service/registry.service';
import { GenericListComponent } from '../generic-list/generic-list.component';

@Component({
  selector: 'app-unified-list',
  standalone: true,
  imports: [CommonModule, GenericListComponent],
  template: `
    @if (customComponent) {
      <ng-container *ngComponentOutlet="customComponent; injector: customInjector"></ng-container>
    } @else if (entityType) {
      <app-generic-list></app-generic-list>
    }
  `
})
export class UnifiedListComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private registry = inject(RegistryService);
  private parentInjector = inject(Injector);

  entityType: string | null = null;
  customComponent: Type<any> | null = null;
  customInjector!: Injector;

  ngOnInit() {
    this.route.params.subscribe(params => {
      const entityPrefix = params['entityType'];
      if (!entityPrefix) return;

      const type = this.registry.getEntityTypeFromPrefix(entityPrefix) || entityPrefix;
      this.entityType = type;

      const customView = this.registry.getCustomView(type);
      if (customView && customView.list) {
        this.customComponent = customView.list;

        // Pass custom injector that overrides ActivatedRoute if needed
        this.customInjector = Injector.create({
          providers: [
            { provide: ActivatedRoute, useValue: this.route }
          ],
          parent: this.parentInjector
        });
      } else {
        this.customComponent = null;
      }
    });
  }
}
