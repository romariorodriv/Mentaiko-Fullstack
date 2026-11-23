import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Emotion } from '../../shared/models/models';

@Injectable({ providedIn: 'root' })
export class EmotionsService {
  private readonly base = environment.apiUrl;
  constructor(private readonly http: HttpClient) {}

  emotions(activeOnly = true): Observable<Emotion[]> { return this.http.get<Emotion[]>(`${this.base}/emotions`, { params: { activeOnly } }); }
  createEmotion(body: Pick<Emotion, 'name'>): Observable<Emotion> { return this.http.post<Emotion>(`${this.base}/admin/emotions`, body); }
  updateEmotion(id: number, body: Partial<Emotion>): Observable<Emotion> { return this.http.put<Emotion>(`${this.base}/admin/emotions/${id}`, body); }
}
