import { Routes, UrlTree, CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { HomeComponent } from './pages/home/home';
import { PriceRowsComponent } from './pages/pricerow/pricerows.component';
import { UnitsComponent } from './pages/unit/units.component';
import { UnitDetailComponent } from './pages/unit/detail/unit-detail.component';
import { UnitFormComponent } from './pages/unit/form/unit-form.component';
import { PricerowDetailComponent } from './pages/pricerow/detail/pricerow-detail.component';
import { PricerowFormComponent } from './pages/pricerow/form/pricerow-form.component';
import { LanguagesComponent } from './pages/language/languages.component';
import { LanguageDetailComponent } from './pages/language/detail/language-detail.component';
import { LanguageFormComponent } from './pages/language/form/language-form.component';
import { CurrenciesComponent } from './pages/currency/currencies.component';
import { CurrencyFormComponent } from './pages/currency/form/currency-form.component';
import { CurrencyDetailComponent } from './pages/currency/detail/currency-detail.component';
import { TaxClassesComponent } from './pages/taxclass/taxclasses.component';
import { TaxClassFormComponent } from './pages/taxclass/form/taxclass-form.component';
import { TaxClassDetailComponent } from './pages/taxclass/detail/taxclass-detail.component';
import { GroupsComponent } from './pages/group/groups.component';
import { GroupDetailComponent } from './pages/group/detail/group-detail.component';
import { GroupFormComponent } from './pages/group/form/group-form.component';
import { OrganizationsComponent } from './pages/organization/organizations.component';
import { OrganizationDetailComponent } from './pages/organization/detail/organization-detail.component';
import { OrganizationFormComponent } from './pages/organization/form/organization-form.component';
import { CountriesComponent } from './pages/country/countries.component';
import { CountryFormComponent } from './pages/country/form/country-form.component';
import { CountryDetailComponent } from './pages/country/detail/country-detail.component';
import { ChannelsComponent } from './pages/channel/channels.component';
import { ChannelFormComponent } from './pages/channel/form/channel-form.component';
import { ChannelDetailComponent } from './pages/channel/detail/channel-detail.component';
import { AppPermissionsComponent } from './pages/apppermission/app-permissions.component';
import { AppPermissionDetailComponent } from './pages/apppermission/detail/app-permission-detail.component';
import { AppPermissionFormComponent } from './pages/apppermission/form/app-permission-form.component';
import { AppRolesComponent } from './pages/approle/app-roles.component';
import { AppRoleDetailComponent } from './pages/approle/detail/app-role-detail.component';
import { AppRoleFormComponent } from './pages/approle/form/app-role-form.component';
import { ServiceInitializationComponent } from './pages/service-initialization/service-initialization.component';
import { TranslocoService } from '@jsverse/transloco';
import { SessionService } from './service/session.service';
import { Router } from '@angular/router';

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
      { path: 'service-initialization', component: ServiceInitializationComponent }
    ]
  }
];
