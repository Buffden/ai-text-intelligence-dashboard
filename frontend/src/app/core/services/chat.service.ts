import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChatMessage, ChatReply, ChatRequest, ChatStreamEvent, ConversationSummary } from '../models/chat.model';

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

    sendStream(request: ChatRequest): Observable<ChatStreamEvent> {
        return new Observable<ChatStreamEvent>((observer) => {
            const controller = new AbortController();

            fetch(`${this.baseUrl}/stream`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'text/event-stream',
                },
                body: JSON.stringify(request),
                signal: controller.signal,
            })
                .then(async (response) => {
                    if (!response.ok) {
                        throw new Error(`HTTP error: ${response.status}`);
                    }
                    if (!response.body) {
                        throw new Error('ReadableStream not supported.');
                    }

                    const reader = response.body
                        .pipeThrough(new TextDecoderStream())
                        .getReader();

                    let buffer = '';

                    while (true) {
                        const { value, done } = await reader.read();
                        if (done) break;

                        buffer += value;
                        const events = buffer.split('\n\n');
                        buffer = events.pop() ?? '';

                        for (const event of events) {
                            const lines = event.split('\n');
                            const eventName = lines.find(l => l.startsWith('event:'))?.slice('event:'.length).trim();
                            const data = lines.find(l => l.startsWith('data:'))?.slice('data:'.length);

                            if (eventName === 'error') {
                                observer.error(new Error(data ?? 'Stream error'));
                                return;
                            }

                            if (eventName === 'conversation-id' && data) {
                                observer.next({ type: 'conversation-id', conversationId: data });
                                continue;
                            }

                            if (data === '[DONE]') {
                                observer.next({ type: 'done' });
                                observer.complete();
                                return;
                            }

                            if (data !== undefined) {
                                observer.next({ type: 'token', token: data });
                            }
                        }
                    }

                    observer.complete();
                })
                .catch((error) => {
                    if (error.name !== 'AbortError') {
                        observer.error(error);
                    }
                });

            return () => controller.abort();
        });
    }
}
