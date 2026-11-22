import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { EmotionalFactorsComponent } from './emotional-factors.component';

describe('EmotionalFactorsComponent', () => {
  it('creates the component', () => {
    TestBed.configureTestingModule({ imports: [EmotionalFactorsComponent], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    expect(TestBed.createComponent(EmotionalFactorsComponent).componentInstance).toBeTruthy();
  });
});
