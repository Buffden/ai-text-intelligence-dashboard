import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChatMessage, ChatReply, ChatRequest, ConversationSummary } from '../models/chat.model';

@Injectable({
    providedIn: 'root',
})
export class ChatService {

    private readonly baseUrl = 'http://localhost:8080/api/chat';

    constructor(private http: HttpClient) { }

    send(request: ChatRequest): Observable<ChatReply> {
        return this.http.post<ChatReply>(this.baseUrl, request);
    }

    getHistory(conversationId: string): Observable<ChatMessage[]> {
        return this.http.get<ChatMessage[]>(`${this.baseUrl}/${conversationId}/history`);
    }

    getConversations(): Observable<ConversationSummary[]> {
        return this.http.get<ConversationSummary[]>(`${this.baseUrl}/conversations`);
    }
}
