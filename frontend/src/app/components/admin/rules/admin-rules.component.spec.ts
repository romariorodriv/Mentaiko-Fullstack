import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { routes } from '../../../app.routes';
import { adminGuard } from '../../../core/guards/admin.guard';
import { AdminRule } from '../../../shared/models/models';
import { AdminRulesComponent } from './admin-rules.component';

describe('AdminRulesComponent', () => {
  const rule: AdminRule = {
    id: 7, emotionId: 1, emotionName: 'Ansiedad', emotionActive: true,
    activityId: 3, activityTitle: 'Respiración guiada', activityActive: true,
    priority: 2, active: true, createdAt: '2026-11-20T10:00:00'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [AdminRulesComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
  });

  function setup(rules: AdminRule[] = [rule]) {
    const fixture = TestBed.createComponent(AdminRulesComponent);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    http.expectOne(r => r.method === 'GET' && r.url === '/api/admin/rules').flush(rules);
    http.expectOne(r => r.url === '/api/emotions' && r.params.get('activeOnly') === 'true')
      .flush([{ id: 1, name: 'Ansiedad', active: true, createdAt: '', updatedAt: '' }]);
    http.expectOne(r => r.url === '/api/micro-activities' && r.params.get('activeOnly') === 'true')
      .flush([{ id: 3, title: 'Respiración guiada', description: '', durationMinutes: 5, active: true, createdAt: '', updatedAt: '' },
              { id: 4, title: 'Caminar', description: '', durationMinutes: 5, active: true, createdAt: '', updatedAt: '' }]);
    fixture.detectChanges();
    return { fixture, http, component: fixture.componentInstance, element: fixture.nativeElement as HTMLElement };
  }

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('is registered at /app/admin/reglas behind adminGuard', () => {
    const route = routes.find(r => r.path === 'app')?.children?.find(r => r.path === 'admin/reglas');
    expect(route).toBeDefined();
    expect(route?.canActivate).toContain(adminGuard);
  });

  it('shows the rules table and the active catalog options', () => {
    const { element } = setup();
    const row = element.querySelector('tbody tr');
    expect(row?.textContent).toContain('Ansiedad');
    expect(row?.textContent).toContain('Respiración guiada');
    expect(row?.textContent).toContain('Activa');
    expect(element.querySelectorAll('select')[1].querySelectorAll('option').length).toBe(3);
  });

  it('creates a rule with POST', () => {
    const { fixture, http, component, element } = setup([]);
    component.form.setValue({ emotionId: 1, activityId: 4, priority: 3 });
    fixture.detectChanges();
    element.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));

    const request = http.expectOne(r => r.method === 'POST' && r.url === '/api/admin/rules');
    expect(request.request.body).toEqual({ emotionId: 1, activityId: 4, priority: 3 });
    request.flush({ ...rule, id: 8, activityId: 4, activityTitle: 'Caminar', priority: 3 });
    http.expectOne(r => r.method === 'GET' && r.url === '/api/admin/rules').flush([]);
    fixture.detectChanges();

    expect(element.textContent).toContain('Regla "Ansiedad → Caminar" creada.');
  });

  it('shows the backend message when the pair is duplicated', () => {
    const { fixture, http, component, element } = setup();
    component.form.setValue({ emotionId: 1, activityId: 3, priority: 1 });
    component.submit();
    http.expectOne(r => r.method === 'POST' && r.url === '/api/admin/rules')
      .flush({ status: 409, message: 'Ya existe una regla para esa emocion y microactividad' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();

    expect(element.querySelector('.alert.error')?.textContent).toContain('Ya existe una regla para esa emocion y microactividad');
  });

  it('edits only the priority with PUT', () => {
    const { fixture, http, component } = setup();
    component.startEdit(rule);
    fixture.detectChanges();
    expect(component.form.controls.emotionId.disabled).toBe(true);
    component.form.controls.priority.setValue(1);
    component.submit();

    const request = http.expectOne(r => r.method === 'PUT' && r.url === '/api/admin/rules/7');
    expect(request.request.body).toEqual({ priority: 1 });
    request.flush({ ...rule, priority: 1 });
    http.expectOne(r => r.method === 'GET' && r.url === '/api/admin/rules').flush([{ ...rule, priority: 1 }]);

    expect(component.editing()).toBeNull();
    expect(component.rules()[0].priority).toBe(1);
  });

  it('does not submit a priority lower than 1', () => {
    const { http, component } = setup();
    component.form.setValue({ emotionId: 1, activityId: 4, priority: 0 });
    component.submit();
    http.expectNone(r => r.method === 'POST');
    expect(component.form.controls.priority.invalid).toBe(true);
  });

  it('toggles the active switch with PUT', () => {
    const { fixture, http, component, element } = setup();
    element.querySelector<HTMLButtonElement>('button.switch')!.click();

    const request = http.expectOne(r => r.method === 'PUT' && r.url === '/api/admin/rules/7');
    expect(request.request.body).toEqual({ active: false });
    request.flush({ ...rule, active: false });
    fixture.detectChanges();

    expect(component.rules()[0].active).toBe(false);
    expect(element.querySelector('tbody tr')?.textContent).toContain('Inactiva');
  });
});
