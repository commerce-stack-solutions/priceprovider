import { Component, Input, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SessionService } from '../../service/session.service';

@Component({
  selector: 'app-localized-stringfield-view',
  templateUrl: './localized-stringfield-view.component.html',
  styleUrls: ['./localized-stringfield-view.component.scss'],
  standalone: true,
  imports: [CommonModule]
})
export class LocalizedStringfieldViewComponent {
  @Input() values?: { [key: string]: string };
  @Input() fieldLabel: string = 'Translations';
  
  sessionService = inject(SessionService);
  
  // Compute all languages (active + inactive) sorted by session language first
  allLanguages = computed(() => {
    const languages = this.sessionService.availableLanguages();
    const sessionLang = this.sessionService.language();
    
    // Sort: session language first, then mandatory, then active, then inactive
    return languages
      .sort((a, b) => {
        if (a.isoKey === sessionLang) return -1;
        if (b.isoKey === sessionLang) return 1;
        if (a.mandatory && !b.mandatory) return -1;
        if (!a.mandatory && b.mandatory) return 1;
        if (a.active && !b.active) return -1;
        if (!a.active && b.active) return 1;
        return a.isoKey.localeCompare(b.isoKey);
      });
  });
  
  // Get only the language keys that exist in the values
  visibleLanguageKeys = computed(() => {
    if (!this.values) return [];
    
    const allKeys = this.allLanguages().map(l => l.isoKey);
    return allKeys.filter(key => this.values && this.values[key]);
  });
  
  isMandatory(lang: string): boolean {
    return this.sessionService.availableLanguages().some(l => l.isoKey === lang && l.mandatory);
  }
  
  isInactive(lang: string): boolean {
    return this.sessionService.availableLanguages().some(l => l.isoKey === lang && !l.active);
  }
}
