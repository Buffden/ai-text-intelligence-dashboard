export interface AnalysisRequest {
    text: string;
}

export interface StreamState {
    streaming: boolean;
    text: string;
    error: string | null;
}