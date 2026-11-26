import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { ShellComponent } from './layouts/app-shell/app-shell.component';

export const routes: Routes = [
  {path: 'olvide-contrasena', loadComponent: () => import('./components/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)},
  {path: 'restablecer-contrasena', loadComponent: () => import('./components/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent)},
  {path: '', pathMatch: 'full', loadComponent: () => import('./components/landing/landing.component').then(m => m.LandingComponent)},
  {path: 'login', loadComponent: () => import('./components/auth/login/login.component').then(m => m.LoginComponent)},
  {path: 'registro', loadComponent: () => import('./components/auth/register/register.component').then(m => m.RegisterComponent)},
  {path: 'app', component: ShellComponent, canActivate: [authGuard], children: [
    {path: 'dashboard', loadComponent: () => import('./components/dashboard/dashboard.component').then(m => m.DashboardComponent)},
    {path: 'preferencias-actividades', loadComponent: () => import('./components/activity-preferences/activity-preferences.component').then(m => m.ActivityPreferencesComponent)},
    {path: 'factores', loadComponent: () => import('./components/factors/emotional-factors.component').then(m => m.EmotionalFactorsComponent)},
    {path: 'recordatorios', loadComponent: () => import('./components/reminders/reminders.component').then(m => m.RemindersComponent)},
    {path: 'meta-semanal', loadComponent: () => import('./components/goals/weekly-goal.component').then(m => m.WeeklyGoalComponent)},
    {path: 'calendario', loadComponent: () => import('./components/calendar/emotional-calendar.component').then(m => m.EmotionalCalendarComponent)},
    {path: 'sueno', loadComponent: () => import('./components/sleep/sleep.component').then(m => m.SleepComponent)},
    {path: 'energia', loadComponent: () => import('./components/energy/energy.component').then(m => m.EnergyComponent)},
    {path: 'checkins', loadComponent: () => import('./components/checkins/checkins.component').then(m => m.CheckinsComponent)},
    {path: 'actividades', loadComponent: () => import('./components/activities/activities.component').then(m => m.ActivitiesComponent)},
    {path: 'historial-actividades', loadComponent: () => import('./components/activity-history/activity-history.component').then(m => m.ActivityHistoryComponent)},
    {path: 'perfil', loadComponent: () => import('./components/profile/profile.component').then(m => m.ProfileComponent)},
    {path: 'admin/dashboard', canActivate: [adminGuard], loadComponent: () => import('./components/admin/dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent)},
    {path: 'admin/emociones', canActivate: [adminGuard], loadComponent: () => import('./components/admin/emotions/admin-emotions.component').then(m => m.AdminEmotionsComponent)},
    {path: 'admin/actividades', canActivate: [adminGuard], loadComponent: () => import('./components/admin/activities/admin-activities.component').then(m => m.AdminActivitiesComponent)},
    {path: 'admin/reglas', canActivate: [adminGuard], loadComponent: () => import('./components/admin/rules/admin-rules.component').then(m => m.AdminRulesComponent)},
    {path: 'admin/auditoria', canActivate: [adminGuard], loadComponent: () => import('./components/admin/audit/admin-audit.component').then(m => m.AdminAuditComponent)},
    {path: 'admin/indicadores', canActivate: [adminGuard], loadComponent: () => import('./components/admin/indicators/admin-indicators.component').then(m => m.AdminIndicatorsComponent)},
    {path: '', pathMatch: 'full', redirectTo: 'dashboard'}
  ]},
  {path: '**', redirectTo: 'login'}
];
