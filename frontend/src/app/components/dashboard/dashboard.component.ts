import { DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ApiService } from '../../core/services/api.service';
import { DistributionItem, WeeklyPoint } from '../../shared/models/models';
import { WeeklyGoalCardComponent } from '../goals/weekly-goal-card.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [FormsModule, RouterLink, DecimalPipe, WeeklyGoalCardComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
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
