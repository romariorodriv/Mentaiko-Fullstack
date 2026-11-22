import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AdminIndicatorsComponent } from './admin-indicators.component';

describe('AdminIndicatorsComponent', () => {
  it('creates the component', () => {
    TestBed.configureTestingModule({ imports: [AdminIndicatorsComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    expect(TestBed.createComponent(AdminIndicatorsComponent).componentInstance).toBeTruthy();
  });
});
