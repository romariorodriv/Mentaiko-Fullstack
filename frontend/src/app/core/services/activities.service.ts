import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MicroActivity, CreateMicroActivityRequest, UpdateMicroActivityRequest } from '../../shared/models/models';

@Injectable({ providedIn: 'root' })
export class ActivitiesService {
  private readonly base = environment.apiUrl;
  constructor(private readonly http: HttpClient) {}

  activities(activeOnly = true): Observable<MicroActivity[]> { return this.http.get<MicroActivity[]>(`${this.base}/micro-activities`, {params: {activeOnly}}); }
  createActivity(body: CreateMicroActivityRequest): Observable<MicroActivity> { return this.http.post<MicroActivity>(`${this.base}/admin/micro-activities`, body); }
  updateActivity(id: number, body: UpdateMicroActivityRequest): Observable<MicroActivity> { return this.http.put<MicroActivity>(`${this.base}/admin/micro-activities/${id}`, body); }
  setActivityType(id: number, activityType: string): Observable<{id:number;activityType:string}> {
    return this.http.patch<{id:number;activityType:string}>(`${this.base}/admin/micro-activities/${id}/type`,{activityType});
  }
}
