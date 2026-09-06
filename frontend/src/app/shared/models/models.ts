export type Role = 'USER' | 'ADMIN';

export interface UserProfile { id: number; name: string; email: string; birthDate: string; university: string; career: string; role: Role; active: boolean; createdAt: string; }
export interface AuthResponse { token: string; user: UserProfile; }
export interface RegisterRequest { name: string; email: string; password: string; birthDate: string; university: string; career: string; }
export interface Emotion { id: number; name: string; active: boolean; }
export interface Checkin { id: number; emotion: Emotion; intensity: number; context: string; note?: string; createdAt: string; }
export interface CheckinRequest { emotionId: number; intensity: number; context: string; note?: string; }
export interface MicroActivity { id: number; title: string; description: string; durationMinutes: number; active: boolean; }
export interface Recommendation { id: number; checkinId: number; activity: MicroActivity; reason: string; fallbackUsed: boolean; createdAt: string; completedAt?: string; }
export interface WeeklyPoint { label: string; count: number; averageIntensity: number; }
export interface DistributionItem { label: string; count: number; percentage: number; }
export interface AdminIndicators { activeUsers: number; totalCheckins: number; completionRate: number; mostFrequentEmotion: string; }
export interface PageResponse<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number; }
export interface ApiError { status?: number; message?: string; errors?: Record<string,string>; }
