import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AdminEmotionsComponent } from './admin-emotions.component';

describe('AdminEmotionsComponent', () => {
  it('creates the component', () => {
    TestBed.configureTestingModule({ imports: [AdminEmotionsComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    expect(TestBed.createComponent(AdminEmotionsComponent).componentInstance).toBeTruthy();
  });
});
