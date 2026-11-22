import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { matchingPasswords, utf8PasswordValidator } from '../password.validators';

@Component({selector: 'app-reset-password', standalone: true, imports: [ReactiveFormsModule, RouterLink], templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css'})
export class ResetPasswordComponent {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly token = this.route.snapshot.queryParamMap.get('token') ?? '';
  readonly invalidLink = signal(!/^[A-Za-z0-9_-]{43}$/.test(this.token));
  readonly loading = signal(false);
  readonly message = signal('');
  readonly error = signal('');
  readonly form = this.fb.nonNullable.group({
    newPassword: ['', [Validators.required, Validators.pattern(/\S/), Validators.minLength(8), Validators.maxLength(72), utf8PasswordValidator]],
    confirmPassword: ['', Validators.required]
  }, {validators: matchingPasswords});

  submit(): void {
    if (this.loading() || this.message() || this.invalidLink()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.error.set('');
    this.auth.resetPassword(this.token, this.form.getRawValue().newPassword).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: response => { this.message.set(response.message); this.form.reset(); },
      error: error => {
        if (error.status === 400 && !error.error?.errors) this.invalidLink.set(true);
        else this.error.set(error.status === 429 ? 'Demasiadas solicitudes. Intenta de nuevo en 15 minutos.'
          : error.status === 400 ? 'Revisa la contraseña. Debe tener al menos 8 caracteres y como máximo 72 bytes UTF-8.'
          : 'No pudimos guardar la contraseña. Revisa tu conexión e intenta de nuevo.');
      }
    });
  }
}
