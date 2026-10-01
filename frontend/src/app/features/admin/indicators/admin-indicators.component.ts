import { DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ApiService } from '../../../core/services/api.service';
import { AdminIndicators } from '../../../shared/models/models';

@Component({
  selector: 'app-admin-indicators',
  standalone: true,
  imports: [DecimalPipe],
  template: `
    <header class="page-head">
      <div>
        <span class="eyebrow">ADMINISTRACION · PRIVACIDAD</span>
        <h1>Indicadores generales</h1>
        <p>Tendencias agregadas sin exponer identidades ni notas privadas.</p>
      </div>
      <button class="btn secondary" (click)="load()" [disabled]="loading()">Actualizar</button>
    </header>

    @if (error()) {
      <div class="alert error">{{ error() }}</div>
    }

    @if (loading()) {
      <div class="loading-panel">Cargando indicadores anonimizados...</div>
    } @else if (data(); as indicators) {
      <section class="metric-grid">
        <article class="metric">
          <span>Usuarios totales</span>
          <strong>{{ indicators.totalUsers }}</strong>
          <small>Conteo agregado</small>
        </article>
        <article class="metric">
          <span>Usuarios activos</span>
          <strong>{{ indicators.activeUsers }}</strong>
          <small>Sin informacion identificable</small>
        </article>
        <article class="metric">
          <span>Check-ins totales</span>
          <strong>{{ indicators.totalCheckins }}</strong>
          <small>Solo conteo general</small>
        </article>
      </section>

      <section class="dashboard-grid">
        <article class="panel">
          <span class="eyebrow">RECOMENDACIONES</span>
          <h2>Finalizacion</h2>
          <div class="metric stacked">
            <span>Generadas</span>
            <strong>{{ indicators.recommendationsGenerated }}</strong>
          </div>
          <div class="metric stacked">
            <span>Completadas</span>
            <strong>{{ indicators.recommendationsCompleted }}</strong>
          </div>
          <div class="metric stacked">
            <span>Tasa</span>
            <strong>{{ indicators.completionRate | number:'1.0-1' }}<i>%</i></strong>
          </div>
        </article>

        <article class="panel span-2">
          <span class="eyebrow">DISTRIBUCION GLOBAL</span>
          <h2>Emociones registradas</h2>
          <div class="distribution">
            @for (item of indicators.emotionDistribution; track item.label) {
              <div>
                <span>{{ item.label }}</span>
                <b>{{ item.percentage | number:'1.0-0' }}%</b>
                <i><em [style.width.%]="item.percentage"></em></i>
              </div>
            } @empty {
              <div class="empty">
                <b>Sin datos</b>
                <p>Los indicadores apareceran cuando existan check-ins.</p>
              </div>
            }
          </div>
        </article>
      </section>

      <section class="panel indicator-feature">
        <div>
          <span class="eyebrow">TENDENCIA GENERAL</span>
          <h2>Emocion mas frecuente</h2>
          <strong>{{ indicators.mostFrequentEmotion }}</strong>
        </div>
        <div class="privacy-shield">
          *
          <p>La respuesta no incluye nombres, correos, IDs personales ni notas privadas.</p>
        </div>
      </section>
    }
  `
})
export class AdminIndicatorsComponent implements OnInit {
  readonly data = signal<AdminIndicators | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.indicators().subscribe({
      next: indicators => {
        this.data.set(indicators);
        this.loading.set(false);
      },
      error: error => {
        this.loading.set(false);
        this.error.set(error.status === 403
          ? 'No tienes permisos para consultar estos indicadores.'
          : 'No se pudo conectar con la API.');
      }
    });
  }
}
