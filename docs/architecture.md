# Architecture

## System Overview

A straightforward three-layer system. The user interacts with an Angular frontend, which talks to a Spring Boot backend, which calls the LLM API. Nothing clever — the goal is a clean, correct implementation of this pattern.

```
┌─────────────────────┐         ┌──────────────────────────┐         ┌─────────────────┐
│                     │  HTTP   │                          │  HTTPS  │                 │
│   Angular Frontend  │────────▶│  Spring Boot + Spring AI │────────▶│  OpenAI / Claude│
│                     │◀────────│                          │◀────────│                 │
└─────────────────────┘  JSON   └──────────────────────────┘  JSON   └─────────────────┘
```

---

## Component Breakdown

### Frontend (Angular)

- Single page with a text input and results display
- Calls the backend `/api/analyze` endpoint
- Handles loading state and error display
- No direct LLM calls — ever

### Backend (Spring Boot + Spring AI)

- Exposes a single REST endpoint: `POST /api/analyze`
- Validates the incoming request
- Builds the prompt using a system prompt template
- Calls the LLM via Spring AI's `ChatClient`
- Parses and validates the structured JSON response
- Logs token usage and latency for every request
- Returns the structured result or a clean error response

### LLM Provider (OpenAI / Claude)

- Called exclusively by the backend
- Configured via Spring AI's provider abstraction — switching providers requires only a config change, not code changes
- Uses structured output / JSON mode to enforce response format

---

## API Contract

### `POST /api/analyze`

**Request:**

```json
{
  "text": "The input text to analyze"
}
```

**Response (success):**

```json
{
  "summary": "A two-sentence summary of the text",
  "sentiment": "positive | negative | neutral",
  "confidence": 0.87,
  "key_topics": ["topic1", "topic2"],
  "word_count_estimate": 142
}
```

**Response (error):**

```json
{
  "error": "A human-readable description of what went wrong",
  "code": "LLM_UNAVAILABLE | INVALID_INPUT | PARSE_ERROR"
}
```

---

## Prompt Design

The backend sends a single request to the LLM with a structured system prompt that instructs it to return a specific JSON format. The user's text is passed as the user message.

```
System:
You are a text analysis assistant. Analyze the provided text and return a JSON object with the following fields:
- summary: a two-sentence summary
- sentiment: "positive", "negative", or "neutral"
- confidence: a float between 0 and 1 representing sentiment confidence
- key_topics: an array of key topics or concepts
- word_count_estimate: an estimated word count

Return only valid JSON. No explanation, no markdown, no extra text.

User:
{input_text}
```

Temperature is set to 0 for consistent, deterministic output.

---

## Key Design Decisions

| Decision | Choice | Reason |
| -------- | ------ | ------ |
| LLM abstraction | Spring AI ChatClient | Keeps provider-switching to config only, no code changes |
| Output format | JSON mode + explicit system prompt | Double enforcement — JSON mode where supported, prompt as fallback |
| Temperature | 0 | Structured extraction needs consistency, not creativity |
| Error handling | Validate → retry once → return clean error | Simple, predictable, easy to test |
| Token logging | On every request | Building the observability habit early |

Detailed reasoning for each decision lives in [`decisions.md`](./decisions.md).
