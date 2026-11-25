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
});
