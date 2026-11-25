import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { environment } from '../../../environments/environment';
import { ActivityPreferencesComponent } from './activity-preferences.component';

describe('ActivityPreferencesComponent', () => {
  const url = `${environment.apiUrl}/me/activity-preferences`;
  const available = ['RESPIRACION','MOVIMIENTO','RELAJACION','ESCRITURA','ATENCION_PLENA','DESCANSO_OCULAR','GENERAL'];

  function setup() {
    TestBed.configureTestingModule({ imports: [ActivityPreferencesComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    const fixture = TestBed.createComponent(ActivityPreferencesComponent);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne({ method: 'GET', url }).flush({ selected: ['RESPIRACION'], available });
    fixture.detectChanges();
    return { fixture, http, element: fixture.nativeElement as HTMLElement };
  }

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('creates the component', () => {
    TestBed.configureTestingModule({ imports: [ActivityPreferencesComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    expect(TestBed.createComponent(ActivityPreferencesComponent).componentInstance).toBeTruthy();
  });

  it('loads saved preferences and shows the Jira-aligned labels', () => {
    const { element } = setup();
    const labels = Array.from(element.querySelectorAll('label')).map(label => label.textContent?.trim());
    expect(labels).toContain('Reflexión escrita');
    expect(labels).toContain('Movimiento y estiramiento');
    expect(labels).toContain('Descanso ocular');
    const checked = Array.from(element.querySelectorAll<HTMLInputElement>('input[type=checkbox]')).filter(input => input.checked);
    expect(checked.length).toBe(1);
  });

  it('saves the selected types with PUT and shows a confirmation', () => {
    const { fixture, http, element } = setup();
    fixture.componentInstance.toggle('DESCANSO_OCULAR', true);
    element.querySelector<HTMLButtonElement>('button')!.click();

    const request = http.expectOne({ method: 'PUT', url });
    expect(request.request.body).toEqual({ selected: ['RESPIRACION', 'DESCANSO_OCULAR'] });
    request.flush({ selected: ['DESCANSO_OCULAR', 'RESPIRACION'], available });
    fixture.detectChanges();

    expect(fixture.componentInstance.selected()).toEqual(['DESCANSO_OCULAR', 'RESPIRACION']);
    expect(element.textContent).toContain('Preferencias actualizadas.');
  });
});
