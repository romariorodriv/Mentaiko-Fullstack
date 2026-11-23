import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';

@Component({selector: 'app-forgot-password', standalone: true, imports: [ReactiveFormsModule, RouterLink], templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css'})
export class ForgotPasswordComponent {
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  readonly loading = signal(false);
  readonly message = signal('');
  readonly error = signal('');
  readonly form = this.fb.nonNullable.group({email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]]});

  submit(): void {
    if (this.loading() || this.message()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.error.set('');
    this.auth.forgotPassword(this.form.getRawValue().email).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: response => this.message.set(response.message),
      error: error => this.error.set(error.status === 429
        ? 'Demasiadas solicitudes. Intenta de nuevo en 15 minutos.'
        : 'No pudimos procesar la solicitud. Revisa tu conexión e intenta de nuevo.')
    });
  }
}
