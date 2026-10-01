import { DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiService } from '../../core/services/api.service';
import { DistributionItem, WeeklyPoint } from '../../shared/models/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [FormsModule, RouterLink, DecimalPipe],
  template: `
    <header class="page-head">
      <div>
        <span class="eyebrow">TU RESUMEN</span>
        <h1>Hola, {{ firstName() }}</h1>
        <p>Una mirada amable a tu semana, sin juicios.</p>
      </div>
      <a class="btn primary" routerLink="/app/checkins">+ Nuevo check-in</a>
    </header>

    @if (loading()) {
      <div class="loading-panel">Cargando tu resumen...</div>
    } @else if (error()) {
      <div class="alert error">{{ error() }} <button (click)="load()">Reintentar</button></div>
    } @else {
      <section class="metric-grid">
        <article class="metric">
          <span>Registros esta semana</span>
          <strong>{{ total() }}</strong>
          <small>Tu constancia construye perspectiva</small>
        </article>
        <article class="metric">
          <span>Intensidad promedio</span>
          <strong>{{ average() }}<i>/5</i></strong>
          <small>Es informacion, no una calificacion</small>
        </article>
        <article class="metric">
          <span>Emocion mas frecuente</span>
          <strong class="word">{{ topEmotion() }}</strong>
          <small>Basado en tus registros</small>
        </article>
      </section>

      <section class="dashboard-grid">
        <article class="panel span-2">
          <div class="panel-head">
            <div>
              <span class="eyebrow">EVOLUCION SEMANAL</span>
              <h2>Tu intensidad por dia</h2>
            </div>
            <input type="week" [(ngModel)]="week" (change)="load()">
          </div>
          <div class="bars">
            @for (point of weekly(); track point.label) {
              <div class="bar-item">
                <span class="bar-value">{{ point.averageIntensity | number:'1.0-1' }}</span>
                <div class="bar-track"><i [style.height.%]="point.averageIntensity * 20"></i></div>
                <small>{{ point.label }}</small>
              </div>
            }
          </div>
        </article>

        <article class="panel">
          <span class="eyebrow">DISTRIBUCION</span>
          <h2>Emociones</h2>
          <div class="distribution">
            @for (item of emotions(); track item.label) {
              <div>
                <span>{{ item.label }}</span>
                <b>{{ item.percentage | number:'1.0-0' }}%</b>
                <i><em [style.width.%]="item.percentage"></em></i>
              </div>
            } @empty {
              <div class="empty">
                <b>Sin datos</b>
                <p>Crea check-ins para ver tu distribucion emocional.</p>
              </div>
            }
          </div>
        </article>
      </section>

      <aside class="care-note">
        <span>*</span>
        <div>
          <b>Una pausa tambien cuenta</b>
          <p>Los patrones son una herramienta de reflexion, no un diagnostico. Avanza a tu ritmo.</p>
        </div>
        <a routerLink="/app/actividades">Ver microactividades -></a>
      </aside>
    }
  `
})
export class DashboardComponent implements OnInit {
  readonly loading = signal(true);
  readonly error = signal('');
  readonly weekly = signal<WeeklyPoint[]>([]);
  readonly emotions = signal<DistributionItem[]>([]);
  week = this.currentWeek();

  constructor(private readonly api: ApiService, private readonly auth: AuthService) {}

  ngOnInit(): void {
    this.load();
  }

  firstName(): string {
    return this.auth.profile()?.name?.split(' ')[0] ?? 'de nuevo';
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    forkJoin({weekly: this.api.weekly(this.week), distribution: this.api.distribution()}).subscribe({
      next: result => {
        this.weekly.set(result.weekly);
        this.emotions.set(result.distribution.emotions);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No pudimos cargar el resumen desde la API.');
      }
    });
  }

  total(): number {
    return this.weekly().reduce((sum, point) => sum + point.count, 0);
  }

  average(): string {
    const pointsWithData = this.weekly().filter(point => point.count > 0);
    return pointsWithData.length
      ? (pointsWithData.reduce((sum, point) => sum + point.averageIntensity, 0) / pointsWithData.length).toFixed(1)
      : '0';
  }

  topEmotion(): string {
    return this.emotions()[0]?.label ?? 'Sin datos';
  }

  private currentWeek(): string {
    const date = new Date();
    const target = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
    const dayNumber = target.getUTCDay() || 7;
    target.setUTCDate(target.getUTCDate() + 4 - dayNumber);
    const yearStart = new Date(Date.UTC(target.getUTCFullYear(), 0, 1));
    const weekNumber = Math.ceil((((target.getTime() - yearStart.getTime()) / 86400000) + 1) / 7);
    return `${target.getUTCFullYear()}-W${String(weekNumber).padStart(2, '0')}`;
  }
}
