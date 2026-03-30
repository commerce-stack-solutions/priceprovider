import { Injectable, inject, signal, effect, untracked } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router, NavigationEnd } from '@angular/router';
import { TranslocoService } from '@jsverse/transloco';
import { environment } from '../environments/environment';
import { Language } from '../model/language/language.model';
import { filter } from 'rxjs/operators';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class SessionService {
  private static readonly DEFAULT_AVAILABLE_LANGUAGES: Language[] = [
    { isoKey: 'en', active: true, mandatory: true },
    { isoKey: 'de', active: true, mandatory: true }
  ];

  private http = inject(HttpClient);
  private translocoService = inject(TranslocoService);
  private router = inject(Router);
  private authService = inject(AuthService);

  // Available languages loaded from API
  availableLanguages = signal<Language[]>(SessionService.DEFAULT_AVAILABLE_LANGUAGES);

  // Current language signal (default: 'en')
  language = signal<string>(this.loadLanguage());

  constructor() {
    effect(() => {
      if (this.authService.isAuthenticated()) {
        this.loadAvailableLanguages();
      } else {
        this.availableLanguages.set(SessionService.DEFAULT_AVAILABLE_LANGUAGES);
      }
    });
    
    // Initialize Transloco with the stored language
    this.translocoService.setActiveLang(this.language());
    
    // Sync Transloco when language changes
    effect(() => {
      const lang = this.language();
      if (untracked(() => this.translocoService.getActiveLang()) !== lang) {
        this.translocoService.setActiveLang(lang);
      }
    });

    // Listen to route changes to extract language from URL
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      const urlParts = event.url.split('/');
      if (urlParts.length > 1) {
        const langFromUrl = urlParts[1];
        const validLangs = ['de', 'en', 'es', 'fr', 'pt', 'nl', 'da', 'sv', 'no', 'fi', 'zh', 'ja', 'sl', 'cs', 'pl', 'hr', 'et', 'lv', 'lt'];
        if (validLangs.includes(langFromUrl) && langFromUrl !== this.language()) {
          // Don't trigger navigation, just update the language
          this.language.set(langFromUrl);
          localStorage.setItem('app-language', langFromUrl);
        }
      }
    });
  }

  private loadLanguage(): string {
    const stored = localStorage.getItem('app-language');
    return stored || 'en';
  }

  private loadAvailableLanguages(): void {
    const url = `${environment.apiBaseUrl}admin/api/languages?q=active:true`;
    this.http.get<{ items: Language[] }>(url).subscribe({
      next: (response) => {
        this.availableLanguages.set(response.items);
      },
      error: (error) => {
        console.error('Failed to load available languages:', error);
        // Fallback to default languages if API fails
        this.availableLanguages.set(SessionService.DEFAULT_AVAILABLE_LANGUAGES);
      }
    });
  }

  setLanguage(languageCode: string): void {
    const currentUrl = this.router.url;
    const urlParts = currentUrl.split('/');
    
    // Update language in session
    this.language.set(languageCode);
    localStorage.setItem('app-language', languageCode);
    
    // Navigate to the same page but with new language
    if (urlParts.length > 1) {
      urlParts[1] = languageCode;
      const newUrl = urlParts.join('/');
      this.router.navigateByUrl(newUrl);
    }
  }

  // Helper method to get current language for use in router links
  getCurrentLanguage(): string {
    return this.language();
  }
}
