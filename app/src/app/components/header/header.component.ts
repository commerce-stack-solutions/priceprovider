import { ChangeDetectionStrategy, Component, EventEmitter, OnInit, Output, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SessionService } from '../../service/session.service';
import { AuthService } from '../../service/auth.service';
import { TranslocoModule } from '@jsverse/transloco';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss'],
  imports: [CommonModule, FormsModule, TranslocoModule],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HeaderComponent implements OnInit {
  private static readonly DARK_THEME = 'dark';
  private static readonly LIGHT_THEME = 'light';
  private static readonly THEME_STORAGE_KEY = 'app-theme';

  @Output() sidebarToggle = new EventEmitter<void>();
  theme = signal(HeaderComponent.LIGHT_THEME);
  sessionService = inject(SessionService);
  authService = inject(AuthService);
  showSessionConfig = signal(false);
  selectedLanguage: string = this.sessionService.language();

  ngOnInit(): void {
    // Load theme from localStorage
    try {
      const savedTheme = localStorage.getItem(HeaderComponent.THEME_STORAGE_KEY);
      if (savedTheme === HeaderComponent.DARK_THEME || savedTheme === HeaderComponent.LIGHT_THEME) {
        this.theme.set(savedTheme);
      }
    } catch (error) {
      console.warn('Failed to load theme from localStorage:', error);
    }
    document.body.setAttribute('data-bs-theme', this.theme());
    this.selectedLanguage = this.sessionService.language();
  }

  toggleTheme(): void {
    this.theme.update(t => 
      t === HeaderComponent.DARK_THEME ? HeaderComponent.LIGHT_THEME : HeaderComponent.DARK_THEME
    );
    const newTheme = this.theme();
    document.body.setAttribute('data-bs-theme', newTheme);
    // Persist theme preference
    try {
      localStorage.setItem(HeaderComponent.THEME_STORAGE_KEY, newTheme);
    } catch (error) {
      console.warn('Failed to save theme to localStorage:', error);
    }
  }

  toggleSessionConfig(): void {
    this.showSessionConfig.update(show => {
      if (!show) {
        // Opening panel - reset selected language to current
        this.selectedLanguage = this.sessionService.language();
      }
      return !show;
    });
  }

  applySessionConfig(): void {
    this.sessionService.setLanguage(this.selectedLanguage);
    this.showSessionConfig.set(false);
  }

  login(): void {
    this.authService.login();
  }

  logout(): void {
    this.authService.logout();
  }
}
