import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Recommendation, AlternativeRecommendationRequest, RecommendationFeedbackRequest } from '../../shared/models/models';

@Injectable({ providedIn: 'root' })
export class RecommendationsService {
  private readonly base = environment.apiUrl;
  constructor(private readonly http: HttpClient) {}

  recommend(checkinId: number, maxDurationMinutes?: number): Observable<Recommendation> {
    return this.http.post<Recommendation>(`${this.base}/recommendations`, {
      checkinId,
      ...(maxDurationMinutes === undefined ? {} : {maxDurationMinutes})
    });
  }
  recommendations(): Observable<Recommendation[]> { return this.http.get<Recommendation[]>(`${this.base}/recommendations/me`); }
  history(from?: string, to?: string): Observable<Recommendation[]> {
    const params: Record<string, string> = {};
    if (from) params['from'] = from;
    if (to) params['to'] = to;
    return this.http.get<Recommendation[]>(`${this.base}/recommendations/me/history`, { params });
  }
  completeRecommendation(id: number, postIntensity?: number): Observable<Recommendation> { return this.http.post<Recommendation>(`${this.base}/recommendations/${id}/complete`, postIntensity === undefined ? {} : { postIntensity }); }
  rateRecommendation(id: number, body: RecommendationFeedbackRequest): Observable<Recommendation> { return this.http.post<Recommendation>(`${this.base}/recommendations/${id}/feedback`, body); }
  requestAlternative(id: number, body: AlternativeRecommendationRequest): Observable<Recommendation> {
    return this.http.post<Recommendation>(`${this.base}/recommendations/${id}/alternative`, body);
  }
}
