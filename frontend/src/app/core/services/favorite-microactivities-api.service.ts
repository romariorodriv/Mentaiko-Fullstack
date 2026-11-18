import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export interface FavoriteMicroActivity {
  id: number;
  title: string;
  description: string;
  durationMinutes: number;
  active: boolean;
  activityType: string;
  savedAt: string;
}

@Injectable({providedIn: 'root'})
export class FavoriteMicroactivitiesApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/me/favorite-micro-activities`;

  list() {
    return this.http.get<FavoriteMicroActivity[]>(this.base);
  }

  save(activityId: number) {
    return this.http.put<FavoriteMicroActivity>(`${this.base}/${activityId}`, {});
  }

  remove(activityId: number) {
    return this.http.delete<void>(`${this.base}/${activityId}`);
  }
}
