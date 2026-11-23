import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ShellComponent } from './app-shell.component';

describe('ShellComponent', () => {
  it('creates the authenticated layout', () => {
    TestBed.configureTestingModule({ imports: [ShellComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    expect(TestBed.createComponent(ShellComponent).componentInstance).toBeTruthy();
  });
});
