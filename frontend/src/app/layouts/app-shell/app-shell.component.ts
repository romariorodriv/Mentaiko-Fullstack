import { Component, OnInit, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-shell', standalone: true, imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
  <div class="app-shell">
    <aside class="sidebar" [class.open]="menuOpen()">
      <a class="brand" routerLink="/app/dashboard" (click)="menuOpen.set(false)"><img class="brand-logo" src="/branding/mentaiko-logo.svg" alt="Mentaiko"></a>
      <nav>
        <p class="nav-caption">MI ESPACIO</p>
        <a routerLink="/app/dashboard" routerLinkActive="active" (click)="menuOpen.set(false)"><span>⌂</span> Resumen</a>
        <a routerLink="/app/checkins" routerLinkActive="active" (click)="menuOpen.set(false)"><span>♡</span> Mis check-ins</a>
        <a routerLink="/app/actividades" routerLinkActive="active" (click)="menuOpen.set(false)"><span>◉</span> Microactividades</a>
        <a routerLink="/app/perfil" routerLinkActive="active" (click)="menuOpen.set(false)"><span>○</span> Mi perfil</a>
        @if (auth.role() === 'ADMIN') {
          <p class="nav-caption">ADMINISTRACIÓN</p>
          <a routerLink="/app/admin/emociones" routerLinkActive="active"><span>◇</span> Emociones</a>
          <a routerLink="/app/admin/actividades" routerLinkActive="active"><span>▦</span> Actividades</a>
          <a routerLink="/app/admin/indicadores" routerLinkActive="active"><span>⌁</span> Indicadores</a>
        }
      </nav>
      <div class="sidebar-note"><b>¿Necesitas ayuda inmediata?</b><span>Mentaiko no reemplaza atención profesional.</span><a href="tel:105">Ver líneas de ayuda</a></div>
      <button class="logout" (click)="auth.logout()">Cerrar sesión <span>↗</span></button>
    </aside>
    <main class="main-area">
      <header class="topbar"><button class="menu-button" (click)="menuOpen.set(!menuOpen())" aria-label="Abrir menú">☰</button><span class="top-date">Un momento para ti</span><span class="avatar">{{ initials() }}</span></header>
      <div class="page-container"><router-outlet /></div>
    </main>
    @if (menuOpen()) { <button class="backdrop" (click)="menuOpen.set(false)" aria-label="Cerrar menú"></button> }
  </div>`
})
export class ShellComponent implements OnInit {
  readonly menuOpen = signal(false);
  constructor(public readonly auth: AuthService) {}
  ngOnInit(): void { if (!this.auth.profile()) this.auth.loadProfile().subscribe({error: () => undefined}); }
  initials(): string { return (this.auth.profile()?.name ?? 'M').split(' ').slice(0,2).map(x => x[0]).join('').toUpperCase(); }
}
