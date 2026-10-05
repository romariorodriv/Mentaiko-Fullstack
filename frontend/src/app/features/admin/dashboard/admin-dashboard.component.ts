import { Component, OnInit, signal } from '@angular/core';
import { ApiService } from '../../../core/services/api.service';
import { AdminCheckin, AdminDashboard, AdminUser, PageResponse } from '../../../shared/models/models';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  template: `
    <header class="page-head">
      <div>
        <span class="eyebrow">ADMINISTRACION</span>
        <h1>Dashboard administrativo</h1>
        <p>Vista general de solo lectura para seguimiento academico del sistema.</p>
      </div>
      <button class="btn secondary" (click)="load()" [disabled]="loading()">Actualizar</button>
    </header>

    @if (error()) {
      <div class="alert error">{{ error() }}</div>
    }

    @if (loading()) {
      <div class="loading-panel">Cargando dashboard administrativo...</div>
    } @else if (summary(); as data) {
      <section class="metric-grid">
        <article class="metric"><span>Usuarios</span><strong>{{ data.totalUsers }}</strong><small>{{ data.activeUsers }} activos</small></article>
        <article class="metric"><span>Administradores</span><strong>{{ data.totalAdmins }}</strong><small>Perfiles con rol ADMIN</small></article>
        <article class="metric"><span>Check-ins</span><strong>{{ data.totalCheckins }}</strong><small>Registros emocionales</small></article>
        <article class="metric"><span>Emociones</span><strong>{{ data.totalEmotions }}</strong><small>Catalogo completo</small></article>
        <article class="metric"><span>Microactividades</span><strong>{{ data.totalMicroActivities }}</strong><small>Catalogo completo</small></article>
        <article class="metric"><span>Recomendaciones</span><strong>{{ data.completedRecommendations }}/{{ data.totalRecommendations }}</strong><small>Completadas / generadas</small></article>
      </section>

      <section class="dashboard-grid">
        <article class="panel">
          <div class="panel-head">
            <div><span class="eyebrow">USUARIOS</span><h2>Listado paginado</h2></div>
            <small>{{ users()?.totalElements || 0 }} total</small>
          </div>
          <div class="data-list">
            @for (user of users()?.content || []; track user.id) {
              <div>
                <span class="status-dot" [class.off]="!user.active"></span>
                <b>{{ user.name }}</b>
                <span>{{ user.email }}</span>
                <span>{{ user.role }}</span>
                <span>{{ user.active ? 'Activo' : 'Inactivo' }}</span>
              </div>
            } @empty {
              <div class="empty">Sin usuarios registrados.</div>
            }
          </div>
          <div class="row-actions">
            <button class="btn secondary" (click)="loadUsers(userPage()-1)" [disabled]="userPage()===0">Anterior</button>
            <button class="btn secondary" (click)="loadUsers(userPage()+1)" [disabled]="!users() || userPage()+1 >= users()!.totalPages">Siguiente</button>
          </div>
        </article>

        <article class="panel span-2">
          <div class="panel-head">
            <div><span class="eyebrow">CHECK-INS</span><h2>Listado paginado</h2></div>
            <small>{{ checkins()?.totalElements || 0 }} total</small>
          </div>
          <div class="data-list">
            @for (checkin of checkins()?.content || []; track checkin.id) {
              <div>
                <span class="status-dot"></span>
                <b>{{ checkin.emotion.name }}</b>
                <span>{{ checkin.user.email }}</span>
                <span>Intensidad {{ checkin.intensity }}</span>
                <span>{{ checkin.context }}</span>
              </div>
            } @empty {
              <div class="empty">Sin check-ins registrados.</div>
            }
          </div>
          <div class="row-actions">
            <button class="btn secondary" (click)="loadCheckins(checkinPage()-1)" [disabled]="checkinPage()===0">Anterior</button>
            <button class="btn secondary" (click)="loadCheckins(checkinPage()+1)" [disabled]="!checkins() || checkinPage()+1 >= checkins()!.totalPages">Siguiente</button>
          </div>
        </article>
      </section>
    }
  `
})
export class AdminDashboardComponent implements OnInit {
  readonly summary = signal<AdminDashboard | null>(null);
  readonly users = signal<PageResponse<AdminUser> | null>(null);
  readonly checkins = signal<PageResponse<AdminCheckin> | null>(null);
  readonly userPage = signal(0);
  readonly checkinPage = signal(0);
  readonly loading = signal(true);
  readonly error = signal('');

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.adminDashboard().subscribe({
      next: data => {
        this.summary.set(data);
        this.loadUsers(0);
        this.loadCheckins(0);
        this.loading.set(false);
      },
      error: error => {
        this.loading.set(false);
        this.error.set(error.status === 403 ? 'No tienes permisos para ver este dashboard.' : 'No se pudo conectar con la API.');
      }
    });
  }

  loadUsers(page: number): void {
    if (page < 0) return;
    this.api.adminUsers(page, 10).subscribe({ next: data => { this.users.set(data); this.userPage.set(data.number); } });
  }

  loadCheckins(page: number): void {
    if (page < 0) return;
    this.api.adminCheckins(page, 10).subscribe({ next: data => { this.checkins.set(data); this.checkinPage.set(data.number); } });
  }
}
