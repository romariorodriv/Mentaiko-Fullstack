import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { utf8PasswordValidator } from '../password.validators';
import { AuthService } from '../../../core/auth/auth.service';

@Component({selector:'app-register',standalone:true,imports:[ReactiveFormsModule,RouterLink],templateUrl: './register.component.html',
  styleUrl: './register.component.css'})
export class RegisterComponent{
 private readonly fb=inject(FormBuilder);
 readonly loading=signal(false);readonly error=signal('');readonly maxBirthDate=this.yesterday();readonly form=this.fb.nonNullable.group({name:['',[Validators.required,Validators.minLength(2),Validators.maxLength(100)]],birthDate:['',[Validators.required,this.pastDateValidator.bind(this)]],university:['',[Validators.required,Validators.maxLength(150)]],career:['',[Validators.required,Validators.maxLength(150)]],email:['',[Validators.required,Validators.email,Validators.maxLength(150)]],password:['',[Validators.required,Validators.minLength(8),Validators.maxLength(72),utf8PasswordValidator]],confirmPassword:['',Validators.required]});
 constructor(private readonly auth:AuthService,private readonly router:Router){}
 submit():void{const v=this.form.getRawValue();if(this.form.invalid){this.form.markAllAsTouched();return;}if(v.password!==v.confirmPassword){this.error.set('Las contrasenas no coinciden.');return;}this.loading.set(true);this.error.set('');const{name,email,password,birthDate,university,career}=v;this.auth.register({name,email,password,birthDate,university,career}).subscribe({next:()=>void this.router.navigate(['/login']),error:e=>{this.loading.set(false);this.error.set(e.status===409?'Ese correo ya esta registrado.':e.error?.message??'Revisa los datos o la conexion con el servidor.');}});}
 showError(name:string):boolean{const control=this.form.get(name);return !!control&&(control.touched||control.dirty)&&control.invalid;}
 private yesterday():string{const d=new Date();d.setDate(d.getDate()-1);return d.toISOString().slice(0,10);}
 private pastDateValidator(control:AbstractControl){if(!control.value)return null;return control.value<=this.yesterday()?null:{pastDate:true};}
}
