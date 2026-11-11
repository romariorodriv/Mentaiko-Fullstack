import { Component, OnInit, DestroyRef, inject, signal } from '@angular/core';
import { interval, startWith, switchMap, catchError, of } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { ReminderApi } from '../../core/services/reminder-api.service';

@Component({
  selector: 'app-shell', standalone: true, imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
  <div class="app-shell">
    <aside class="sidebar" [class.open]="menuOpen()">
      <a class="brand" routerLink="/app/dashboard" (click)="menuOpen.set(false)"><img class="brand-logo" src="/branding/mentaiko-logo.svg" alt="Mentaiko"></a>
      <nav>
        <p class="nav-caption">MI ESPACIO</p>
        <a routerLink="/app/dashboard" routerLinkActive="active" (click)="menuOpen.set(false)"><span>⌂</span> Resumen</a>
        <a routerLink="/app/factores" routerLinkActive="active" (click)="menuOpen.set(false)">Factores emocionales</a>
        <a routerLink="/app/recordatorios" routerLinkActive="active" (click)="menuOpen.set(false)">Recordatorios</a>
        <a routerLink="/app/meta-semanal" routerLinkActive="active" (click)="menuOpen.set(false)">Mi meta semanal</a>
        <a routerLink="/app/calendario" routerLinkActive="active" (click)="menuOpen.set(false)"><span>▦</span> Calendario emocional</a>
        <a routerLink="/app/sueno" routerLinkActive="active" (click)="menuOpen.set(false)">Mi sueño</a>
        <a routerLink="/app/energia" routerLinkActive="active" (click)="menuOpen.set(false)">Mi energía</a>
        <a routerLink="/app/checkins" routerLinkActive="active" (click)="menuOpen.set(false)"><span>♡</span> Mis check-ins</a>
        <a routerLink="/app/actividades" routerLinkActive="active" (click)="menuOpen.set(false)"><span>◉</span> Microactividades</a>
        <a routerLink="/app/perfil" routerLinkActive="active" (click)="menuOpen.set(false)"><span>○</span> Mi perfil</a>
        @if (auth.role() === 'ADMIN') {
          <p class="nav-caption">ADMINISTRACIÓN</p>
          <a routerLink="/app/admin/dashboard" routerLinkActive="active"><span>▣</span> Dashboard</a>
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
      <div class="page-container">
        @if (reminderDue()) {
          <div class="alert" role="status">
            <p>Es hora de registrar cómo te sientes.</p>
            <a routerLink="/app/checkins" (click)="markReminderSeen()">Registrar emoción</a>
            <button type="button" (click)="markReminderSeen()">Marcar como visto</button>
          </div>
        }
        <router-outlet />
      </div>
    </main>
    @if (menuOpen()) { <button class="backdrop" (click)="menuOpen.set(false)" aria-label="Cerrar menú"></button> }
  </div>`
})
export class ShellComponent implements OnInit {
  readonly menuOpen = signal(false);
  readonly reminderDue = signal(false);
  private readonly destroyRef = inject(DestroyRef);
  constructor(public readonly auth: AuthService, private readonly reminders: ReminderApi) {}
  ngOnInit(): void {
    if (!this.auth.profile()) this.auth.loadProfile().subscribe({error: () => undefined});
    interval(60_000).pipe(
      startWith(0),
      switchMap(() => this.reminders.get().pipe(catchError(() => of(null)))),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(p => { if (p) this.reminderDue.set(p.due); });
  }
  markReminderSeen(): void {
    this.reminders.acknowledge().subscribe({
      next: p => this.reminderDue.set(p.due),
      error: () => undefined
    });
  }
  initials(): string { return (this.auth.profile()?.name ?? 'M').split(' ').slice(0,2).map(x => x[0]).join('').toUpperCase(); }
}
