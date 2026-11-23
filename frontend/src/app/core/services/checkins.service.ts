import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Checkin, CheckinRequest, PageResponse } from '../../shared/models/models';

@Injectable({ providedIn: 'root' })
export class CheckinsService {
  private readonly base = environment.apiUrl;
  constructor(private readonly http: HttpClient) {}

  checkins(filters: { from?: string; to?: string; emotionId?: number; context?: string; page?: number; size?: number } = {}): Observable<PageResponse<Checkin>> {
    let params = new HttpParams(); Object.entries(filters).forEach(([k, v]) => { if (v !== undefined && v !== '') params = params.set(k, String(v)); });
    return this.http.get<PageResponse<Checkin>>(`${this.base}/checkins`, { params });
  }
  createCheckin(body: CheckinRequest): Observable<Checkin> { return this.http.post<Checkin>(`${this.base}/checkins`, body); }
  updateCheckin(id: number, body: CheckinRequest): Observable<Checkin> { return this.http.put<Checkin>(`${this.base}/checkins/${id}`, body); }
  deleteCheckin(id: number): Observable<void> { return this.http.delete<void>(`${this.base}/checkins/${id}`); }
}
