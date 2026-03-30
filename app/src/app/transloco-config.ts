import { TranslocoHttpLoader } from './transloco-loader';
import { provideTransloco, TranslocoModule } from '@jsverse/transloco';
import { isDevMode } from '@angular/core';

export const translocoConfig = provideTransloco({
  config: {
    availableLangs: [
      { id: 'de', label: 'Deutsch' },
      { id: 'en', label: 'English' },
      { id: 'es', label: 'Español' },
      { id: 'fr', label: 'Français' },
      { id: 'pt', label: 'Português' },
      { id: 'nl', label: 'Nederlands' },
      { id: 'da', label: 'Dansk' },
      { id: 'sv', label: 'Svenska' },
      { id: 'no', label: 'Norsk' },
      { id: 'fi', label: 'Suomi' },
      { id: 'zh', label: '中文' },
      { id: 'ja', label: '日本語' },
      { id: 'sl', label: 'Slovenščina' },
      { id: 'cs', label: 'Čeština' },
      { id: 'pl', label: 'Polski' },
      { id: 'hr', label: 'Hrvatski' },
      { id: 'et', label: 'Eesti' },
      { id: 'lv', label: 'Latviešu' },
      { id: 'lt', label: 'Lietuvių' }
    ],
    defaultLang: 'en',
    fallbackLang: 'en',
    reRenderOnLangChange: true,
    prodMode: !isDevMode(),
  },
  loader: TranslocoHttpLoader
});

export { TranslocoModule };
