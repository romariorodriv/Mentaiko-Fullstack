import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AdminRule, CreateAdminRuleRequest, UpdateAdminRuleRequest } from '../../shared/models/models';

@Injectable({ providedIn: 'root' })
export class AdminRulesService {
  private readonly base = `${environment.apiUrl}/admin/rules`;
  constructor(private readonly http: HttpClient) {}

  rules(): Observable<AdminRule[]> { return this.http.get<AdminRule[]>(this.base); }
  createRule(body: CreateAdminRuleRequest): Observable<AdminRule> { return this.http.post<AdminRule>(this.base, body); }
  updateRule(id: number, body: UpdateAdminRuleRequest): Observable<AdminRule> { return this.http.put<AdminRule>(`${this.base}/${id}`, body); }
}
