import { Routes, UrlTree, CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { HomeComponent } from './pages/home/home';
import { PriceRowsComponent } from './pages/pricerow/pricerows.component';
import { PricerowDetailComponent } from './pages/pricerow/detail/pricerow-detail.component';
import { PricerowFormComponent } from './pages/pricerow/form/pricerow-form.component';
import { TranslocoService } from '@jsverse/transloco';
import { Router } from '@angular/router';
import { AppPermissionsComponent, AppPermissionDetailComponent, AppPermissionFormComponent, AppRolesComponent, AppRoleDetailComponent, AppRoleFormComponent, GenericFormComponent, GenericListComponent, GenericDetailComponent, ServiceInitializationComponent, SessionService } from 'core';
import { UnitsComponent, UnitDetailComponent, UnitFormComponent, LanguagesComponent, LanguageDetailComponent, LanguageFormComponent, CurrenciesComponent, CurrencyFormComponent, CurrencyDetailComponent, TaxClassesComponent, TaxClassFormComponent, TaxClassDetailComponent, GroupsComponent, GroupDetailComponent, GroupFormComponent, OrganizationsComponent, OrganizationDetailComponent, OrganizationFormComponent, CountriesComponent, CountryFormComponent, CountryDetailComponent, ChannelsComponent, ChannelFormComponent, ChannelDetailComponent } from 'corebusiness';

// Guard to redirect root to user's preferred language
export const rootRedirectGuard: CanActivateFn = (): boolean | UrlTree => {
  const router = inject(Router);
  const sessionService = inject(SessionService);
  const currentLang = sessionService.language();
  return router.parseUrl(`/${currentLang}/home`);
};

// Language resolver function to sync URL language with Transloco
export const languageResolver = (route: any) => {
  const translocoService = inject(TranslocoService);
  const sessionService = inject(SessionService);
  const lang = route.params['lang'];
  
  // Valid languages list
  const validLangs = ['de', 'en', 'es', 'fr', 'pt', 'nl', 'da', 'sv', 'no', 'fi', 'zh', 'ja', 'sl', 'cs', 'pl', 'hr', 'et', 'lv', 'lt'];
  
  if (lang && validLangs.includes(lang)) {
    // Update Transloco and SessionService without triggering navigation
    translocoService.setActiveLang(lang);
    // Use internal update to avoid circular navigation
    sessionService.language.set(lang);
    localStorage.setItem('app-language', lang);
  }
  
  return true;
};

export const routes: Routes = [
  { 
    path: '', 
    canActivate: [rootRedirectGuard],
    children: []
  },
  {
    path: ':lang',
    resolve: { language: languageResolver },
    children: [
      { path: 'home', component: HomeComponent },
      { path: 'pricerows', component: PriceRowsComponent },
      { path: 'pricerows/add', component: PricerowFormComponent },
      { path: 'pricerows/:id', component: PricerowDetailComponent },
      { path: 'pricerows/:id/edit', component: PricerowFormComponent },
      { path: 'units', component: UnitsComponent },
      { path: 'units/add', component: UnitFormComponent },
      { path: 'units/:symbol', component: UnitDetailComponent },
      { path: 'units/:symbol/edit', component: UnitFormComponent },
      { path: 'languages', component: LanguagesComponent },
      { path: 'languages/add', component: LanguageFormComponent },
      { path: 'languages/:isoKey', component: LanguageDetailComponent },
      { path: 'languages/:isoKey/edit', component: LanguageFormComponent },
      { path: 'currencies', component: CurrenciesComponent },
      { path: 'currencies/add', component: CurrencyFormComponent },
      { path: 'currencies/:currencyKey', component: CurrencyDetailComponent },
      { path: 'currencies/:currencyKey/edit', component: CurrencyFormComponent },
      { path: 'taxclasses', component: TaxClassesComponent },
      { path: 'taxclasses/add', component: TaxClassFormComponent },
      { path: 'taxclasses/:taxClassId', component: TaxClassDetailComponent },
      { path: 'taxclasses/:taxClassId/edit', component: TaxClassFormComponent },
      { path: 'groups', component: GroupsComponent },
      { path: 'groups/add', component: GroupFormComponent },
      { path: 'groups/:id', component: GroupDetailComponent },
      { path: 'groups/:id/edit', component: GroupFormComponent },
      { path: 'organizations', component: OrganizationsComponent },
      { path: 'organizations/add', component: OrganizationFormComponent },
      { path: 'organizations/:id', component: OrganizationDetailComponent },
      { path: 'organizations/:id/edit', component: OrganizationFormComponent },
      { path: 'countries', component: CountriesComponent },
      { path: 'countries/add', component: CountryFormComponent },
      { path: 'countries/:isoKey', component: CountryDetailComponent },
      { path: 'countries/:isoKey/edit', component: CountryFormComponent },
      { path: 'channels', component: ChannelsComponent },
      { path: 'channels/add', component: ChannelFormComponent },
      { path: 'channels/:id', component: ChannelDetailComponent },
      { path: 'channels/:id/edit', component: ChannelFormComponent },
      { path: 'app-permissions', component: AppPermissionsComponent },
      { path: 'app-permissions/add', component: AppPermissionFormComponent },
      { path: 'app-permissions/:id', component: AppPermissionDetailComponent },
      { path: 'app-permissions/:id/edit', component: AppPermissionFormComponent },
      { path: 'app-roles', component: AppRolesComponent },
      { path: 'app-roles/add', component: AppRoleFormComponent },
      { path: 'app-roles/:id', component: AppRoleDetailComponent },
      { path: 'app-roles/:id/edit', component: AppRoleFormComponent },
      { path: 'generic/:entityType', component: GenericListComponent },
      { path: 'generic/:entityType/add', component: GenericFormComponent },
      { path: 'generic/:entityType/:id', component: GenericDetailComponent },
      { path: 'generic/:entityType/:id/edit', component: GenericFormComponent },
      { path: 'service-initialization', component: ServiceInitializationComponent }
    ]
  }
];
