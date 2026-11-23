import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { EnergyComponent } from './energy.component';

describe('EnergyComponent', () => {
  it('creates the component', () => {
    TestBed.configureTestingModule({ imports: [EnergyComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    expect(TestBed.createComponent(EnergyComponent).componentInstance).toBeTruthy();
  });
});
