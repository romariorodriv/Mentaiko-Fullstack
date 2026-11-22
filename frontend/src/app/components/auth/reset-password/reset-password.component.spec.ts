import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ResetPasswordComponent } from './reset-password.component';

describe('ResetPasswordComponent', () => {
  it('creates the component', () => {
    TestBed.configureTestingModule({ imports: [ResetPasswordComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    expect(TestBed.createComponent(ResetPasswordComponent).componentInstance).toBeTruthy();
  });
});
