import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface WellnessMessage {
  role: 'user' | 'assistant';
  content: string;
}
export interface WellnessChatResponse {
  reply: string;
  activity: { id: number; title: string; description: string; durationMinutes: number } | null;
}

@Injectable({ providedIn: 'root' })
export class WellnessChatService {
  constructor(private readonly http: HttpClient) {}

  send(message: string, history: WellnessMessage[]): Observable<WellnessChatResponse> {
    return this.http.post<WellnessChatResponse>(`${environment.apiUrl}/wellness/chat`, {
      message,
      history: history.slice(-6).map(item => ({
        role: item.role,
        content: item.content.slice(0, 500)
      }))
    });
  }
}
