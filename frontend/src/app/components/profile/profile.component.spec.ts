import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ProfileComponent } from './profile.component';

describe('ProfileComponent', () => {
  it('creates the component', () => {
    TestBed.configureTestingModule({ imports: [ProfileComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    expect(TestBed.createComponent(ProfileComponent).componentInstance).toBeTruthy();
  });

  it('links to the activity preferences screen', () => {
    TestBed.configureTestingModule({ imports: [ProfileComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    const fixture = TestBed.createComponent(ProfileComponent);
    fixture.detectChanges();
    TestBed.inject(HttpTestingController).match(() => true)
      .forEach(request => request.flush({ name: 'Ana Pérez', email: 'ana@example.com', university: 'UPC', career: 'Software', role: 'USER' }));
    fixture.detectChanges();

    const link = (fixture.nativeElement as HTMLElement).querySelector<HTMLAnchorElement>('a[href="/app/preferencias-actividades"]');
    expect(link).not.toBeNull();
    expect(link!.textContent).toContain('Preferencias de actividades');
  });

  it('requires password confirmation and calls the self-deletion endpoint', () => {
    TestBed.configureTestingModule({ imports: [ProfileComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([{path: 'login', component: ProfileComponent}])] });
    const fixture = TestBed.createComponent(ProfileComponent);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne(request => request.url.endsWith('/users/me') && request.method === 'GET')
      .flush({ name: 'QA', email: 'qa@example.invalid', university: 'UPC', career: 'Software', role: 'USER' });
    fixture.detectChanges();

    const first = [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')]
      .find(b => b.textContent?.includes('Quiero eliminar mi cuenta'))!;
    first.click();
    fixture.detectChanges();
    const pwd = (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>('#delete-password')!;
    pwd.value = 'Password-qa-2026!';
    pwd.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    const confirm = [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')]
      .find(b => b.textContent?.includes('Eliminar definitivamente'))!;
    confirm.click();
    const request = http.expectOne(req => req.method === 'DELETE' && req.url.endsWith('/users/me'));
    expect(request.request.body).toEqual({password: 'Password-qa-2026!'});
    request.flush(null, {status: 204, statusText: 'No Content'});
  });
});
