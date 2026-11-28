import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ShellComponent } from './app-shell.component';
import { AuthService } from '../../core/auth/auth.service';
import { ReminderApi } from '../../core/services/reminder-api.service';

describe('ShellComponent', () => {
  const role = signal('USER');
  const auth = {
    role,
    profile: () => ({ name: 'Estudiante' }),
    logout: vi.fn(),
    loadProfile: () => of(null)
  };

  beforeEach(async () => {
    role.set('USER');
    await TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: ReminderApi, useValue: {
          get: () => of({ due: false }), acknowledge: () => of({ due: false })
        }}
      ]
    }).compileComponents();
  });

  it('creates the authenticated layout', () => {
    expect(TestBed.createComponent(ShellComponent).componentInstance).toBeTruthy();
  });

  it('keeps every student destination while grouping frequent actions first', () => {
    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    const sidebar = fixture.nativeElement as HTMLElement;
    const paths = Array.from(sidebar.querySelectorAll<HTMLAnchorElement>('nav a'))
      .map(link => new URL(link.href).pathname);
    expect(paths).toEqual([
      '/app/dashboard',
      '/app/checkins', '/app/bienestar-chat', '/app/actividades', '/app/historial-actividades',
      '/app/calendario', '/app/meta-semanal', '/app/sueno', '/app/energia',
      '/app/factores', '/app/recordatorios',
      '/app/preferencias-actividades', '/app/perfil'
    ]);
    expect(sidebar.textContent).toContain('MI BIENESTAR');
    expect(sidebar.textContent).toContain('MI SEGUIMIENTO');
    expect(sidebar.textContent).toContain('PREFERENCIAS');
    expect(sidebar.textContent).toContain('¿Necesitas ayuda inmediata?');
    expect(sidebar.textContent).toContain('Cerrar sesión');
  });

  it('preserves administrator destinations for administrators', () => {
    role.set('ADMIN');
    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    const paths = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<HTMLAnchorElement>('nav a'))
      .map(link => new URL(link.href).pathname);
    expect(paths.filter(path => path.startsWith('/app/admin/'))).toEqual([
      '/app/admin/dashboard', '/app/admin/emociones', '/app/admin/actividades',
      '/app/admin/reglas', '/app/admin/auditoria', '/app/admin/indicadores'
    ]);
  });
});
