import { AbstractControl, ValidationErrors } from '@angular/forms';

export function utf8PasswordValidator(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && new TextEncoder().encode(control.value).length > 72
    ? {utf8Length: true} : null;
}

export function matchingPasswords(control: AbstractControl): ValidationErrors | null {
  return control.get('newPassword')?.value === control.get('confirmPassword')?.value
    ? null : {passwordMismatch: true};
}
