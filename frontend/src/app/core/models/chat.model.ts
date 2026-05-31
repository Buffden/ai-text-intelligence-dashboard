export type ChatRole = 'user' | 'assistant';

export interface ChatMessage {
    role: ChatRole;
    content: string;
}

export interface ChatRequest {
    conversationId?: string;
    message: string;
}

export interface ChatReply {
    conversationId: string;
    reply: string;
}

export interface ConversationSummary {
    id: string;
    title: string;
    createdAt: string;   // ISO-8601 Instant from backend
    messageCount: number;
}

export type ChatStreamEvent =
    | { type: 'conversation-id'; conversationId: string }
    | { type: 'token'; token: string }
    | { type: 'done' };
