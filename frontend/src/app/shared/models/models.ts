export type Role = 'USER' | 'ADMIN';

export interface UserProfile { id: number; name: string; email: string; birthDate: string; university: string; career: string; role: Role; active: boolean; createdAt: string; }
export interface AuthResponse { token: string; user: UserProfile; }
export interface RegisterRequest { name: string; email: string; password: string; birthDate: string; university: string; career: string; }
export interface Emotion { id: number; name: string; active: boolean; createdAt: string; updatedAt: string; }
export interface CheckinEmotion { id: number; name: string; }
export interface Checkin { id: number; emotion: CheckinEmotion; intensity: number; context: string; note?: string; createdAt: string; }
export interface CheckinRequest { emotionId: number; intensity: number; context: string; note?: string; }
export interface MicroActivity { id: number; title: string; description: string; durationMinutes: number; active: boolean; activityType?: string; createdAt: string; updatedAt: string; }
export interface FavoriteMicroActivity {
  id: number;
  title: string;
  description: string;
  durationMinutes: number;
  active: boolean;
  activityType: string;
  savedAt: string;
}
export interface CreateMicroActivityRequest { title: string; description: string; durationMinutes: number; }
export interface UpdateMicroActivityRequest { title?: string; description?: string; durationMinutes?: number; active?: boolean; }
export interface Recommendation { id: number; checkinId: number; activity: MicroActivity; reason: string; fallbackUsed: boolean; createdAt: string; completedAt?: string; helpfulnessRating?: number | null; feedbackComment?: string | null; preIntensity?: number | null; postIntensity?: number | null; }
export interface RecommendationFeedbackRequest { helpfulnessRating: number; feedbackComment?: string; }
export interface WeeklyPoint { label: string; count: number; averageIntensity: number; }
export interface DistributionItem { label: string; count: number; percentage: number; }
export interface AdminIndicators { totalUsers: number; activeUsers: number; totalCheckins: number; emotionDistribution: DistributionItem[]; recommendationsGenerated: number; recommendationsCompleted: number; completionRate: number; mostFrequentEmotion: string; }
export interface AdminDashboard { totalUsers: number; activeUsers: number; totalAdmins: number; totalCheckins: number; totalEmotions: number; totalMicroActivities: number; totalRecommendations: number; completedRecommendations: number; }
export interface AdminUser { id: number; name: string; email: string; birthDate: string; university: string; career: string; role: Role; active: boolean; createdAt: string; }
export interface AdminCheckin { id: number; user: { id: number; name: string; email: string }; emotion: CheckinEmotion; intensity: number; context: string; createdAt: string; }
export interface PageResponse<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number; }
export interface ApiError { status?: number; message?: string; errors?: Record<string,string>; }

export interface PeriodSummary { week: string; count: number; averageIntensity: number; predominantEmotion: string | null; }
export interface CompareResult { weekA: PeriodSummary; weekB: PeriodSummary; }
export interface AdminRule { id: number; emotionId: number; emotionName: string; emotionActive: boolean; activityId: number; activityTitle: string; activityActive: boolean; priority: number; active: boolean; createdAt: string; }
export interface CreateAdminRuleRequest { emotionId: number; activityId: number; priority: number; active?: boolean; }
export interface UpdateAdminRuleRequest { priority?: number; active?: boolean; }