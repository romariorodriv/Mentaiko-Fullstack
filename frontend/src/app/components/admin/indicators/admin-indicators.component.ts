import { DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ApiService } from '../../../core/services/api.service';
import { AdminIndicators } from '../../../shared/models/models';

@Component({
  selector: 'app-admin-indicators',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './admin-indicators.component.html',
  styleUrl: './admin-indicators.component.css'
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
