import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Recommendation } from '../../shared/models/models';

@Injectable({ providedIn: 'root' })
export class RecommendationsService {
  private readonly base = environment.apiUrl;
  constructor(private readonly http: HttpClient) {}

  recommend(checkinId: number): Observable<Recommendation> { return this.http.post<Recommendation>(`${this.base}/recommendations`, {checkinId}); }
  recommendations(): Observable<Recommendation[]> { return this.http.get<Recommendation[]>(`${this.base}/recommendations/me`); }
  completeRecommendation(id: number): Observable<Recommendation> { return this.http.post<Recommendation>(`${this.base}/recommendations/${id}/complete`, {}); }
}
