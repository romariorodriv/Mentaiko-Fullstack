import { Component, OnInit, signal } from '@angular/core';
import { AdminService } from '../../../core/services/admin.service';
import { AdminCheckin, AdminDashboard, AdminUser, PageResponse } from '../../../shared/models/models';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css'
})
export class AdminDashboardComponent implements OnInit {
  readonly summary = signal<AdminDashboard | null>(null);
  readonly users = signal<PageResponse<AdminUser> | null>(null);
  readonly checkins = signal<PageResponse<AdminCheckin> | null>(null);
  readonly userPage = signal(0);
  readonly checkinPage = signal(0);
  readonly loading = signal(true);
  readonly error = signal('');

  constructor(private readonly adminApi: AdminService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.adminApi.adminDashboard().subscribe({
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
    this.adminApi.adminUsers(page, 10).subscribe({ next: data => { this.users.set(data); this.userPage.set(data.number); } });
  }

  loadCheckins(page: number): void {
    if (page < 0) return;
    this.adminApi.adminCheckins(page, 10).subscribe({ next: data => { this.checkins.set(data); this.checkinPage.set(data.number); } });
  }
}
