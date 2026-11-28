import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { WellnessChatService, WellnessMessage, WellnessChatResponse } from '../../core/services/wellness-chat.service';

type ChatEntry = WellnessMessage & { activity?: WellnessChatResponse['activity'] };

@Component({
  selector: 'app-wellness-chat',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './wellness-chat.component.html',
  styleUrl: './wellness-chat.component.css'
})
export class WellnessChatComponent {
  readonly messages = signal<ChatEntry[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  draft = '';

  private readonly service = inject(WellnessChatService);
  private readonly destroyRef = inject(DestroyRef);

  send(): void {
    const message = this.draft.trim();
    if (!message || message.length > 500 || this.loading()) return;
    const history = this.messages().slice(-6).map(({ role, content }) => ({ role, content }));
    this.messages.update(items => [...items, { role: 'user', content: message }]);
    this.draft = '';
    this.error.set('');
    this.loading.set(true);
    this.service.send(message, history).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.loading.set(false))
    ).subscribe({
      next: result => this.messages.update(items => [...items, {
        role: 'assistant', content: result.reply, activity: result.activity
      }]),
      error: failure => this.error.set(
        failure.status === 503
          ? 'El asistente no está disponible. Intenta nuevamente más tarde.'
          : 'No pudimos enviar tu mensaje. Intenta nuevamente.'
      )
    });
  }

  clear(): void {
    if (this.loading()) return;
    this.messages.set([]);
    this.error.set('');
    this.draft = '';
  }
}
