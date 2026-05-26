# Architecture

This document evolves alongside the project. Each week adds new capabilities — sections are marked with the week they were introduced so the progression is clear.

---

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
- Calls the backend API endpoints
- Handles loading state and error display
- No direct LLM calls — ever

### Backend (Spring Boot + Spring AI)

**Week 1 — Foundation**
- Exposes `POST /api/analyze` — full text analysis returning summary, sentiment, topics, and word count
- Validates the incoming request
- Builds the prompt using a system prompt template
- Calls the LLM via Spring AI's `ChatClient`
- Parses and validates the structured JSON response
- Logs token usage and latency for every request
- Returns the structured result or a clean error response

**Week 2 — Prompt Engineering**
- Exposes `POST /api/classify` — focused category classification using few-shot prompting
- Uses a separate system prompt (`classify-system.st`) with chain-of-thought reasoning
- Hardens `/api/analyze` against prompt injection — user input is wrapped in `<text>` delimiters and the model is explicitly instructed to treat it as data, not instructions

### LLM Provider (OpenAI / Claude)

- Called exclusively by the backend
- Configured via Spring AI's provider abstraction — switching providers requires only a config change, not code changes
- Uses structured output / JSON mode to enforce response format

---

## API Contract

All endpoints accept the same request body:

```json
{
  "text": "The input text to analyze"
}
```

All endpoints return the same error shape on failure:

```json
{
  "error": "A human-readable description of what went wrong",
  "code": "LLM_UNAVAILABLE | INVALID_INPUT | PARSE_ERROR"
}
```

### `POST /api/analyze` — Week 1

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

### `POST /api/classify` — Week 2

**Response (success):**

```json
{
  "reasoning": "The text discusses AI investment and automation, which maps to technology.",
  "category": "technology | politics | sports | business | health | other",
  "confidence": 0.91
}
```

---

## Prompt Design

### Week 1 — Zero-shot structured output (`analyze-system.st`)

A single system prompt instructs the model to return a specific JSON shape. The user's text is passed as the user message. Temperature is set to 0 for consistent, deterministic output.

### Week 2 — Few-shot classification with chain-of-thought (`classify-system.st`)

A separate system prompt for `/api/classify` embeds 10 labelled examples across all 6 categories. The `reasoning` field is placed before `category` in both the spec and examples so the model writes its thinking before committing to a label — a chain-of-thought pattern that improves classification consistency on ambiguous inputs.

### Week 2 — Prompt injection hardening (`analyze-system.st`)

User input on `/api/analyze` is wrapped in `<text>...</text>` delimiters before being passed to the model. The system prompt explicitly tells the model that content inside those tags is data to analyze, not instructions to follow. Output schema validation acts as the second line of defense — any injection that bypasses the prompt still has to produce valid JSON matching the `AnalysisResponse` contract.

---

## Key Design Decisions

| Decision | Week | Choice | Reason |
| -------- | ---- | ------ | ------ |
| LLM abstraction | 1 | Spring AI ChatClient | Keeps provider-switching to config only, no code changes |
| Output format | 1 | JSON mode + explicit system prompt | Double enforcement — JSON mode where supported, prompt as fallback |
| Temperature | 1 | 0 | Structured extraction needs consistency, not creativity |
| Error handling | 1 | Validate → retry once → return clean error | Simple, predictable, easy to test |
| Token logging | 1 | On every request | Building the observability habit early |
| Classification prompting | 2 | Few-shot with 10 labelled examples | Zero-shot degrades on ambiguous inputs; examples show the model where category boundaries sit |
| Reasoning field ordering | 2 | `reasoning` before `category` in output | Forces genuine chain-of-thought — model commits its thinking before the label, not after |
| Prompt file per endpoint | 2 | Separate `classify-system.st` | Each prompt has one output contract; merging them makes both harder to reason about and test |
| Injection hardening | 2 | `<text>` delimiters + explicit data instruction | Tells the model where user input starts and ends, and that it is data — not a command |

Detailed reasoning for each decision lives in [`decisions.md`](./decisions.md).
