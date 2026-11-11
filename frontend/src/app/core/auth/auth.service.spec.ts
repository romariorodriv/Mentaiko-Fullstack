import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';

describe('Password recovery API contracts', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]});
    auth = TestBed.inject(AuthService); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); localStorage.clear(); });

  it('posts email to the same-origin API and keeps the existing session', () => {
    localStorage.setItem('mentaiko_token', 'existing');
    auth.forgotPassword(' user@example.com ').subscribe();
    const request = http.expectOne('/api/auth/forgot-password');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({email: 'user@example.com'});
    request.flush({message: 'Si el correo está registrado...'});
    expect(auth.token()).toBe('existing');
  });

  it('posts token and newPassword without altering the password and clears the session on success', () => {
    localStorage.setItem('mentaiko_token', 'old');
    auth.resetPassword('a'.repeat(43), ' ClaveNueva123! ').subscribe();
    const request = http.expectOne('/api/auth/reset-password');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({token: 'a'.repeat(43), newPassword: ' ClaveNueva123! '});
    request.flush({message: 'Actualizada'});
    expect(auth.token()).toBeNull();
  });

  it('keeps the session when reset fails', () => {
    localStorage.setItem('mentaiko_token', 'existing');
    auth.resetPassword('a'.repeat(43), 'NuevaClave123!').subscribe({error: () => undefined});
    http.expectOne('/api/auth/reset-password').flush({message: 'Vencido'}, {status: 400, statusText: 'Bad Request'});
    expect(auth.token()).toBe('existing');
  });
});
