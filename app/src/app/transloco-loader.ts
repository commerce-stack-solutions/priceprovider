import { inject, Injectable } from '@angular/core';
import { Translation, TranslocoLoader } from '@jsverse/transloco';
import { HttpClient } from '@angular/common/http';
import { forkJoin, Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class TranslocoHttpLoader implements TranslocoLoader {
  private http = inject(HttpClient);

  getTranslation(lang: string): Observable<Translation> {
    // Load all translation files for the language and merge them
    return forkJoin({
      common: this.http.get<Translation>(`/assets/i18n/${lang}/common.json`),
      pages: this.http.get<Translation>(`/assets/i18n/${lang}/pages.json`),
      components: this.http.get<Translation>(`/assets/i18n/${lang}/components.json`)
    }).pipe(
      map(({ common, pages, components }) => ({
        common,
        pages,
        components
      }))
    );
  }
}
