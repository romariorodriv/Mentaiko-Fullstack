import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { CheckinsComponent } from './checkins.component';

describe('CheckinsComponent', () => {
  it('creates the component', () => {
    TestBed.configureTestingModule({ imports: [CheckinsComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    expect(TestBed.createComponent(CheckinsComponent).componentInstance).toBeTruthy();
  });
});
