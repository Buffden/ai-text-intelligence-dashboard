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
