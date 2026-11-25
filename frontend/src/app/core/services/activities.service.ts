import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MicroActivity, FavoriteMicroActivity, ConfirmFocusSessionRequest, FocusSession, GuidedMicroActivity, CreateMicroActivityRequest, UpdateMicroActivityRequest } from '../../shared/models/models';

@Injectable({ providedIn: 'root' })
export class ActivitiesService {
  private readonly base = environment.apiUrl;
  constructor(private readonly http: HttpClient) {}

  activities(activeOnly = true, maxDuration?: number): Observable<MicroActivity[]> {
    return this.http.get<MicroActivity[]>(`${this.base}/micro-activities`, {
      params: {...{activeOnly}, ...(maxDuration === undefined ? {} : {maxDuration})}
    });
  }
  favorites(): Observable<FavoriteMicroActivity[]> {
    return this.http.get<FavoriteMicroActivity[]>(`${this.base}/micro-activities/favorites`);
  }
  addFavorite(id: number): Observable<FavoriteMicroActivity> {
    return this.http.post<FavoriteMicroActivity>(`${this.base}/micro-activities/${id}/favorite`, {});
  }
  removeFavorite(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/micro-activities/${id}/favorite`);
  }
  activityGuide(id: number): Observable<GuidedMicroActivity> {
    return this.http.get<GuidedMicroActivity>(`${this.base}/micro-activities/${id}`);
  }

  confirmFocusSession(body: ConfirmFocusSessionRequest): Observable<FocusSession> {
    return this.http.post<FocusSession>(`${this.base}/focus-sessions`, body);
  }
  createActivity(body: CreateMicroActivityRequest): Observable<MicroActivity> { return this.http.post<MicroActivity>(`${this.base}/admin/micro-activities`, body); }
  updateActivity(id: number, body: UpdateMicroActivityRequest): Observable<MicroActivity> { return this.http.put<MicroActivity>(`${this.base}/admin/micro-activities/${id}`, body); }
  setActivityType(id: number, activityType: string): Observable<{id:number;activityType:string}> {
    return this.http.patch<{id:number;activityType:string}>(`${this.base}/admin/micro-activities/${id}/type`,{activityType});
  }
}
