import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, RegisterRequest, Role, UserProfile } from '../../shared/models/models';

@Injectable({providedIn: 'root'})
export class AuthService {
  private readonly key = 'mentaiko_token';
  private readonly profileState = signal<UserProfile | null>(null);
  readonly profile = this.profileState.asReadonly();
  readonly isAuthenticated = computed(() => !!this.token());

  constructor(private readonly http: HttpClient, private readonly router: Router) {}

  token(): string | null { return localStorage.getItem(this.key); }
  hasUsableToken(): boolean {
    const token = this.token();
    if (!token) return false;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return !payload.exp || payload.exp * 1000 > Date.now();
    } catch {
      this.clearSession();
      return false;
    }
  }
  role(): Role | null {
    const token = this.token();
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const role = payload.role ?? payload.roles?.[0] ?? payload.authorities?.[0];
      return String(role).replace('ROLE_', '') as Role;
    } catch { this.clearSession(); return null; }
  }
  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, {email, password}).pipe(tap(r => {
      localStorage.setItem(this.key, r.token);
      if (r.user) this.profileState.set(r.user);
    }));
  }
  register(body: RegisterRequest): Observable<UserProfile> { return this.http.post<UserProfile>(`${environment.apiUrl}/auth/register`, body); }
  loadProfile(): Observable<UserProfile> { return this.http.get<UserProfile>(`${environment.apiUrl}/users/me`).pipe(tap(p => this.profileState.set(p))); }
  updateProfile(body: Pick<UserProfile,'name'|'university'|'career'>): Observable<UserProfile> { return this.http.put<UserProfile>(`${environment.apiUrl}/users/me`, body).pipe(tap(p => this.profileState.set(p))); }
  logout(): void { localStorage.removeItem(this.key); this.profileState.set(null); void this.router.navigate(['/login']); }
  clearSession(): void { localStorage.removeItem(this.key); this.profileState.set(null); }
}
