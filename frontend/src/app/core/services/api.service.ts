import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AdminIndicators, Checkin, CheckinRequest, DistributionItem, Emotion, MicroActivity, PageResponse, Recommendation, WeeklyPoint } from '../../shared/models/models';

@Injectable({providedIn: 'root'})
export class ApiService {
  private readonly base = environment.apiUrl;
  constructor(private readonly http: HttpClient) {}

  emotions(activeOnly = true): Observable<Emotion[]> { return this.http.get<Emotion[]>(`${this.base}/emotions`, {params: {activeOnly}}); }
  createEmotion(body: Pick<Emotion,'name'>): Observable<Emotion> { return this.http.post<Emotion>(`${this.base}/admin/emotions`, body); }
  updateEmotion(id: number, body: Partial<Emotion>): Observable<Emotion> { return this.http.put<Emotion>(`${this.base}/admin/emotions/${id}`, body); }

  checkins(filters: {from?: string; to?: string; emotionId?: number; context?: string; page?: number; size?: number} = {}): Observable<PageResponse<Checkin>> {
    let params = new HttpParams(); Object.entries(filters).forEach(([k,v]) => { if (v !== undefined && v !== '') params = params.set(k, String(v)); });
    return this.http.get<PageResponse<Checkin>>(`${this.base}/checkins`, {params});
  }
  createCheckin(body: CheckinRequest): Observable<Checkin> { return this.http.post<Checkin>(`${this.base}/checkins`, body); }
  updateCheckin(id: number, body: CheckinRequest): Observable<Checkin> { return this.http.put<Checkin>(`${this.base}/checkins/${id}`, body); }
  deleteCheckin(id: number): Observable<void> { return this.http.delete<void>(`${this.base}/checkins/${id}`); }

  activities(activeOnly = true): Observable<MicroActivity[]> { return this.http.get<MicroActivity[]>(`${this.base}/micro-activities`, {params: {activeOnly}}); }
  createActivity(body: Omit<MicroActivity,'id'>): Observable<MicroActivity> { return this.http.post<MicroActivity>(`${this.base}/admin/micro-activities`, body); }
  updateActivity(id: number, body: Partial<MicroActivity>): Observable<MicroActivity> { return this.http.put<MicroActivity>(`${this.base}/admin/micro-activities/${id}`, body); }
  recommend(checkinId: number): Observable<Recommendation> { return this.http.post<Recommendation>(`${this.base}/recommendations`, {checkinId}); }
  recommendations(): Observable<Recommendation[]> { return this.http.get<Recommendation[]>(`${this.base}/recommendations/me`); }
  completeRecommendation(id: number): Observable<Recommendation> { return this.http.post<Recommendation>(`${this.base}/recommendations/${id}/complete`, {}); }

  weekly(week: string): Observable<WeeklyPoint[]> { return this.http.get<WeeklyPoint[]>(`${this.base}/reports/weekly`, {params: {week}}); }
  distribution(from?: string, to?: string): Observable<{emotions: DistributionItem[]; contexts: DistributionItem[]}> {
    let params = new HttpParams(); if (from) params=params.set('from',from); if (to) params=params.set('to',to);
    return this.http.get<{emotions: DistributionItem[]; contexts: DistributionItem[]}>(`${this.base}/reports/distribution`, {params});
  }
  indicators(): Observable<AdminIndicators> { return this.http.get<AdminIndicators>(`${this.base}/admin/indicators`); }
}
