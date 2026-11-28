import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { WellnessChatComponent } from './wellness-chat.component';
import { WellnessChatService } from '../../core/services/wellness-chat.service';
import { provideRouter } from '@angular/router';

describe('WellnessChatComponent', () => {
  let fixture: ComponentFixture<WellnessChatComponent>;
  let component: WellnessChatComponent;
  let send: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    send = vi.fn().mockReturnValue(of({ reply: 'Puedes hacer una pausa.', activity: null }));
    await TestBed.configureTestingModule({
      imports: [WellnessChatComponent],
      providers: [provideRouter([]), { provide: WellnessChatService, useValue: { send } }]
    }).compileComponents();
    fixture = TestBed.createComponent(WellnessChatComponent);
    component = fixture.componentInstance;
  });

  it('sends the message and displays the response', () => {
    component.draft = 'Estoy estresado';
    component.send();
    expect(send).toHaveBeenCalledWith('Estoy estresado', []);
    expect(component.messages().map(m => m.role)).toEqual(['user', 'assistant']);
    expect(component.messages()[1].content).toBe('Puedes hacer una pausa.');
  });

  it('shows an error without clearing the user message when the provider is unavailable', () => {
    send.mockReturnValue(throwError(() => ({ status: 503 })));
    component.draft = 'Hola';
    component.send();
    expect(component.error()).toContain('no está disponible');
    expect(component.messages()).toHaveLength(1);
  });

  it('rejects blank messages', () => {
    component.draft = '   ';
    component.send();
    expect(send).not.toHaveBeenCalled();
  });
});
