import { Component, OnInit, OnDestroy, Injector, Type, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Params } from '@angular/router';
import { BehaviorSubject, Subscription } from 'rxjs';
import { RegistryService } from '../../service/registry.service';
import { GenericDetailComponent } from '../generic-detail/generic-detail.component';

const PARAM_MAP: { [key: string]: string } = {
  'currency': 'currencyKey',
  'language': 'isoKey',
  'country': 'isoKey',
  'taxclass': 'taxClassId',
  'unit': 'symbol'
};

@Component({
  selector: 'app-unified-detail',
  standalone: true,
  imports: [CommonModule, GenericDetailComponent],
  template: `
    @if (customComponent) {
      <ng-container *ngComponentOutlet="customComponent; injector: customInjector"></ng-container>
    } @else if (entityType) {
      <app-generic-detail></app-generic-detail>
    }
  `
})
export class UnifiedDetailComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private registry = inject(RegistryService);
  private parentInjector = inject(Injector);

  entityType: string | null = null;
  customComponent: Type<any> | null = null;
  customInjector!: Injector;

  private paramsSubject = new BehaviorSubject<Params>({});
  private routeSub?: Subscription;

  ngOnInit() {
    this.routeSub = this.route.params.subscribe(params => {
      const entityPrefix = params['entityType'];
      if (!entityPrefix) return;

      const type = this.registry.getEntityTypeFromPrefix(entityPrefix) || entityPrefix;
      this.entityType = type;

      const paramKey = PARAM_MAP[type.toLowerCase()];
      const mappedParams: Params = { ...params };
      const id = params['id'];
      if (paramKey && id) {
        mappedParams[paramKey] = id;
      }

      this.paramsSubject.next(mappedParams);

      const customRoute = {
        ...this.route,
        snapshot: {
          ...this.route.snapshot,
          params: mappedParams
        },
        params: this.paramsSubject.asObservable()
      };

      const customView = this.registry.getCustomView(type);
      if (customView && customView.detail) {
        this.customComponent = customView.detail;
        this.customInjector = Injector.create({
          providers: [
            { provide: ActivatedRoute, useValue: customRoute }
          ],
          parent: this.parentInjector
        });
      } else {
        this.customComponent = null;
      }
    });
  }

  ngOnDestroy() {
    this.routeSub?.unsubscribe();
  }
}
