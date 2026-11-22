import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { WeeklyPoint, DistributionItem } from '../../shared/models/models';

@Injectable({ providedIn: 'root' })
export class ReportsService {
  private readonly base = environment.apiUrl;
  constructor(private readonly http: HttpClient) {}

  weekly(week: string): Observable<WeeklyPoint[]> { return this.http.get<WeeklyPoint[]>(`${this.base}/reports/weekly`, { params: { week } }); }
  distribution(from?: string, to?: string): Observable<{ emotions: DistributionItem[]; contexts: DistributionItem[] }> {
    let params = new HttpParams(); if (from) params = params.set('from', from); if (to) params = params.set('to', to);
    return this.http.get<{ emotions: DistributionItem[]; contexts: DistributionItem[] }>(`${this.base}/reports/distribution`, { params });
  }
}
