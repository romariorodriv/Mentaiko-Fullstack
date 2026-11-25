import { DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { ReportsService } from '../../core/services/reports.service';
import { CompareResult, DistributionItem, WeeklyPoint } from '../../shared/models/models';
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
  readonly compare = signal<CompareResult | null>(null);
  readonly compareLoading = signal(false);
  readonly compareError = signal('');
  readonly lowDataThreshold = 3;
  week = this.currentWeek();
  weekA = this.shiftWeek(this.currentWeek(), -1);
  weekB = this.currentWeek();

  constructor(private readonly reports: ReportsService, private readonly auth: AuthService) {}

  ngOnInit(): void {
    this.load();
    this.loadCompare();
  }

  firstName(): string {
    return this.auth.profile()?.name?.split(' ')[0] ?? 'de nuevo';
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    forkJoin({weekly: this.reports.weekly(this.week), distribution: this.reports.distribution()}).subscribe({
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

  loadCompare(): void {
    if (!this.weekA || !this.weekB) {
      this.compare.set(null);
      this.compareError.set('Selecciona dos semanas para comparar.');
      return;
    }
    this.compareLoading.set(true);
    this.compareError.set('');
    this.reports.compare(this.weekA, this.weekB).subscribe({
      next: result => {
        this.compare.set(result);
        this.compareLoading.set(false);
      },
      error: () => {
        this.compareLoading.set(false);
        this.compareError.set('No pudimos cargar la comparativa.');
      }
    });
  }

  isLowData(count: number): boolean {
    return count < this.lowDataThreshold;
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

  private shiftWeek(week: string, delta: number): string {
    const [yearText, weekText] = week.split('-W');
    // El 4 de enero siempre cae en la semana ISO 1 de su año.
    const jan4 = new Date(Date.UTC(Number(yearText), 0, 4));
    const jan4Day = jan4.getUTCDay() || 7;
    const monday = new Date(jan4);
    monday.setUTCDate(jan4.getUTCDate() - jan4Day + 1 + (Number(weekText) - 1 + delta) * 7);
    const target = new Date(monday);
    target.setUTCDate(monday.getUTCDate() + 3); // jueves de esa semana define el año ISO
    const yearStart = new Date(Date.UTC(target.getUTCFullYear(), 0, 1));
    const weekNumber = Math.ceil((((target.getTime() - yearStart.getTime()) / 86400000) + 1) / 7);
    return `${target.getUTCFullYear()}-W${String(weekNumber).padStart(2, '0')}`;
  }
}
