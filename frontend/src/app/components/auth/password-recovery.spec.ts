import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../../app.routes';
import { ForgotPasswordComponent } from './forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './reset-password/reset-password.component';

describe('Password recovery public screens', () => {
  let harness: RouterTestingHarness;
  let http: HttpTestingController;
  const token = 'a'.repeat(43);
  beforeEach(async () => {
    localStorage.clear();
    TestBed.configureTestingModule({providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()]});
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });
  afterEach(() => { http.verify(); localStorage.clear(); });

  it('shows the recovery link on login', async () => {
    await harness.navigateByUrl('/login');
    expect(harness.routeNativeElement?.querySelector('a[href="/olvide-contrasena"]')?.textContent).toContain('¿Olvidaste tu contraseña?');
  });

  it('validates email before sending', async () => {
    const component = await harness.navigateByUrl('/olvide-contrasena', ForgotPasswordComponent);
    component.form.setValue({email: 'bad'}); component.submit(); harness.detectChanges();
    expect(component.form.invalid).toBe(true);
    expect(harness.routeNativeElement?.textContent).toContain('correo válido');
    http.expectNone('/api/auth/forgot-password');
  });

  it('submits through the form, prevents duplicates and shows the generic success', async () => {
    const component = await harness.navigateByUrl('/olvide-contrasena', ForgotPasswordComponent);
    const input = harness.routeNativeElement!.querySelector('input')!;
    input.value = 'user@example.com'; input.dispatchEvent(new Event('input')); harness.detectChanges();
    harness.routeNativeElement!.querySelector('form')!.dispatchEvent(new Event('submit', {bubbles: true, cancelable: true}));
    component.submit(); harness.detectChanges();
    expect(component.loading()).toBe(true);
    expect(harness.routeNativeElement!.querySelector('button')!.disabled).toBe(true);
    http.expectOne('/api/auth/forgot-password').flush({message: 'Si el correo está registrado, recibirás instrucciones.'});
    harness.detectChanges();
    expect(component.loading()).toBe(false);
    expect(harness.routeNativeElement?.textContent).toContain('Si el correo está registrado');
    component.submit(); http.expectNone('/api/auth/forgot-password');
    expect(harness.routeNativeElement?.querySelector('a[href="/login"]')).not.toBeNull();
  });

  it.each([0, 429, 500])('allows retry after request error %s', async status => {
    const component = await harness.navigateByUrl('/olvide-contrasena', ForgotPasswordComponent);
    component.form.setValue({email: 'user@example.com'}); component.submit();
    http.expectOne('/api/auth/forgot-password').flush({}, {status, statusText: 'Error'});
    expect(component.loading()).toBe(false); expect(component.error()).not.toBe('');
    component.submit(); http.expectOne('/api/auth/forgot-password').flush({message: 'Solicitud recibida'});
  });

  it.each(['', '?token=bad', '?token=' + 'a'.repeat(257)])('handles missing or malformed token %s', async query => {
    const component = await harness.navigateByUrl('/restablecer-contrasena' + query, ResetPasswordComponent);
    expect(component.invalidLink()).toBe(true); component.submit();
    http.expectNone('/api/auth/reset-password');
    expect(harness.routeNativeElement?.querySelector('a[href="/olvide-contrasena"]')).not.toBeNull();
  });

  it('requires matching passwords and limits UTF-8 bytes', async () => {
    const component = await harness.navigateByUrl('/restablecer-contrasena?token=' + token, ResetPasswordComponent);
    for (const value of ['short', ' '.repeat(8), 'é'.repeat(37)]) {
      component.form.setValue({newPassword: value, confirmPassword: value});
      component.submit(); expect(component.form.invalid).toBe(true);
    }
    component.form.setValue({newPassword: 'NuevaClave123!', confirmPassword: 'Diferente123!'});
    expect(component.form.hasError('passwordMismatch')).toBe(true);
    component.submit(); http.expectNone('/api/auth/reset-password');
    component.form.setValue({newPassword: 'é'.repeat(36), confirmPassword: 'é'.repeat(36)});
    expect(component.form.valid).toBe(true);
  });

  it('submits the token from the public URL once and shows success with login navigation', async () => {
    const component = await harness.navigateByUrl('/restablecer-contrasena?token=' + token, ResetPasswordComponent);
    component.form.setValue({newPassword: 'NuevaClave123!', confirmPassword: 'NuevaClave123!'});
    component.submit(); component.submit(); harness.detectChanges();
    expect(component.loading()).toBe(true);
    const request = http.expectOne('/api/auth/reset-password');
    expect(request.request.body).toEqual({token, newPassword: 'NuevaClave123!'});
    request.flush({message: 'Contraseña actualizada'}); harness.detectChanges();
    expect(component.loading()).toBe(false);
    expect(harness.routeNativeElement?.textContent).toContain('Contraseña actualizada');
    expect(harness.routeNativeElement?.querySelector('a[href="/login"]')).not.toBeNull();
    component.submit(); http.expectNone('/api/auth/reset-password');
  });

  it('handles the shared invalid, used or expired token response', async () => {
    const component = await harness.navigateByUrl('/restablecer-contrasena?token=' + token, ResetPasswordComponent);
    component.form.setValue({newPassword: 'NuevaClave123!', confirmPassword: 'NuevaClave123!'}); component.submit();
    http.expectOne('/api/auth/reset-password').flush({message: 'El enlace es invalido o ha vencido'}, {status: 400, statusText: 'Bad Request'});
    harness.detectChanges(); expect(component.invalidLink()).toBe(true);
    expect(harness.routeNativeElement?.textContent).toContain('ya fue usado o ha vencido');
    component.submit(); http.expectNone('/api/auth/reset-password');
  });

  it.each([0, 429, 500])('allows reset retry after transient error %s', async status => {
    const component = await harness.navigateByUrl('/restablecer-contrasena?token=' + token, ResetPasswordComponent);
    component.form.setValue({newPassword: 'NuevaClave123!', confirmPassword: 'NuevaClave123!'}); component.submit();
    http.expectOne('/api/auth/reset-password').flush({}, {status, statusText: 'Error'});
    expect(component.invalidLink()).toBe(false); expect(component.loading()).toBe(false);
    expect(component.error()).not.toBe('');
    component.submit(); http.expectOne('/api/auth/reset-password').flush({message: 'Actualizada'});
  });

  it('shows field validation errors without classifying the token as invalid', async () => {
    const component = await harness.navigateByUrl('/restablecer-contrasena?token=' + token, ResetPasswordComponent);
    component.form.setValue({newPassword: 'NuevaClave123!', confirmPassword: 'NuevaClave123!'}); component.submit();
    http.expectOne('/api/auth/reset-password').flush({errors: {newPassword: 'No válida'}}, {status: 400, statusText: 'Bad Request'});
    expect(component.invalidLink()).toBe(false); expect(component.error()).toContain('Revisa la contraseña');
  });
});
