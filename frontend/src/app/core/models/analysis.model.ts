export interface AnalysisRequest {
    text: string;
}

export interface StreamState {
    streaming: boolean;
    text: string;
    error: string | null;
}

export type ClassifyCategory = 'technology' | 'politics' | 'sports' | 'business' | 'health' | 'other';

export interface ClassifyResult {
    category: ClassifyCategory;
    confidence: number; // 0.0 – 1.0
    reasoning: string;
}