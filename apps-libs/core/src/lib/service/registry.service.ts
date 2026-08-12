import { Injectable, Type } from '@angular/core';

export interface CustomView {
  list?: Type<any>;
  detail?: Type<any>;
  form?: Type<any>;
}

export interface SidebarMenuItem {
  type: string;
  path: string;
  labelKey: string;
  icon: string;
  permission?: string;
}

@Injectable({
  providedIn: 'root'
})
export class RegistryService {
  private customViews = new Map<string, CustomView>();
  private menuSections = new Map<string, string>(); // type -> section name
  private routePrefixes = new Map<string, string>(); // type -> plural route prefix e.g. 'Currency' -> 'currencies'

  constructor() {
    // Register default menu sections
    this.registerMenuSection('Channel', 'Commerce Management');
    this.registerMenuSection('PriceRow', 'Commerce Management');
    this.registerMenuSection('TaxClass', 'Commerce Management');
    this.registerMenuSection('Organization', 'Organizations & Groups');
    this.registerMenuSection('Group', 'Organizations & Groups');
    this.registerMenuSection('Country', 'Master Data');
    this.registerMenuSection('Currency', 'Master Data');
    this.registerMenuSection('Unit', 'Master Data');
    this.registerMenuSection('Language', 'Master Data');
    this.registerMenuSection('AppRole', 'System & Access Management');
    this.registerMenuSection('AppPermission', 'System & Access Management');
    this.registerMenuSection('ServiceInitialization', 'System & Access Management');

    // Register route prefixes
    this.registerRoutePrefix('Channel', 'channels');
    this.registerRoutePrefix('PriceRow', 'pricerows');
    this.registerRoutePrefix('TaxClass', 'taxclasses');
    this.registerRoutePrefix('Organization', 'organizations');
    this.registerRoutePrefix('Group', 'groups');
    this.registerRoutePrefix('Country', 'countries');
    this.registerRoutePrefix('Currency', 'currencies');
    this.registerRoutePrefix('Unit', 'units');
    this.registerRoutePrefix('Language', 'languages');
    this.registerRoutePrefix('AppRole', 'app-roles');
    this.registerRoutePrefix('AppPermission', 'app-permissions');
  }

  registerCustomView(type: string, view: CustomView) {
    this.customViews.set(type.toLowerCase(), view);
  }

  getCustomView(type: string): CustomView | undefined {
    return this.customViews.get(type.toLowerCase());
  }

  registerMenuSection(type: string, section: string) {
    this.menuSections.set(type.toLowerCase(), section);
  }

  getMenuSection(type: string): string | undefined {
    return this.menuSections.get(type.toLowerCase());
  }

  getMenuSections(): Map<string, string> {
    return this.menuSections;
  }

  registerRoutePrefix(type: string, prefix: string) {
    this.routePrefixes.set(type.toLowerCase(), prefix);
    this.routePrefixes.set(prefix.toLowerCase(), type); // dual mapping
  }

  getRoutePrefix(type: string): string | undefined {
    return this.routePrefixes.get(type.toLowerCase());
  }

  getEntityTypeFromPrefix(prefix: string): string | undefined {
    return this.routePrefixes.get(prefix.toLowerCase());
  }
}
