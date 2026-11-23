import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AdminCheckin, AdminDashboard, AdminIndicators, AdminUser, PageResponse } from '../../shared/models/models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly base = environment.apiUrl;
  constructor(private readonly http: HttpClient) {}

  indicators(): Observable<AdminIndicators> { return this.http.get<AdminIndicators>(`${this.base}/admin/indicators`); }
  adminDashboard(): Observable<AdminDashboard> { return this.http.get<AdminDashboard>(`${this.base}/admin/dashboard`); }
  adminUsers(page = 0, size = 10): Observable<PageResponse<AdminUser>> {
    return this.http.get<PageResponse<AdminUser>>(`${this.base}/admin/users`, { params: { page, size } });
  }
  adminCheckins(page = 0, size = 10): Observable<PageResponse<AdminCheckin>> {
    return this.http.get<PageResponse<AdminCheckin>>(`${this.base}/admin/checkins`, { params: { page, size } });
  }
}
