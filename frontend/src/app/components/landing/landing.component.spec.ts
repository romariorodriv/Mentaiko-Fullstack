import { TestBed } from '@angular/core/testing';
import { LandingComponent } from './landing.component';

describe('LandingComponent', () => {
  it('renders the public landing page', () => {
    TestBed.configureTestingModule({ imports: [LandingComponent] });
    const fixture = TestBed.createComponent(LandingComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('main')).toBeTruthy();
  });
});
