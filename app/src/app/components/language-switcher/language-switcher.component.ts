import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslocoService, TranslocoModule } from '@jsverse/transloco';

@Component({
  selector: 'app-language-switcher',
  templateUrl: './language-switcher.component.html',
  styleUrls: ['./language-switcher.component.scss'],
  imports: [CommonModule, TranslocoModule]
})
export class LanguageSwitcherComponent {
  private translocoService = inject(TranslocoService);
  
  isOpen = signal(false);
  
  get availableLanguages() {
    return this.translocoService.getAvailableLangs() as Array<{ id: string; label: string }>;
  }
  
  get currentLang() {
    return this.translocoService.getActiveLang();
  }
  
  get currentLangLabel() {
    const lang = this.availableLanguages.find(l => l.id === this.currentLang);
    return lang ? lang.label : this.currentLang;
  }
  
  toggleDropdown() {
    this.isOpen.update(value => !value);
  }
  
  selectLanguage(langId: string) {
    this.translocoService.setActiveLang(langId);
    this.isOpen.set(false);
  }
  
  closeDropdown() {
    this.isOpen.set(false);
  }
}
