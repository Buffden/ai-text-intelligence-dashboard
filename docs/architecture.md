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

**Week 3 — LLM API Integration (Extension 3)**
- Adds multi-provider fallback: if the primary model exhausts all retries with `LlmUnavailableException`, the request is transparently routed to a configurable fallback model
- Retry logic extracted into `attemptAnalyze()` and `attemptClassify()` — public methods handle the fallback decision only, private methods handle the retry loop only
- Model override applied per-request via `OpenAiChatOptions` — no separate `ChatClient` bean required
- Parse errors do not trigger the fallback — only connectivity and availability failures do
- Provider (primary or fallback) and actual model name logged on every request

**Week 3 — LLM API Integration (Extension 4)**
- Replaces the fixed one-retry loop with exponential backoff: delay grows as `baseDelay * 2^(attempt-1)` with full jitter applied to desynchronise concurrent retries
- Respects the `Retry-After` header on 429 responses — uses the server-specified delay instead of the calculated backoff
- `ParseException` exits the retry loop immediately — parse failures are prompt problems, not transient errors, and retrying them wastes time
- Connect and read timeouts externalized to `application.yaml` — no hardcoded values anywhere in the codebase
- `LlmClientConfig` wires timeout values into Spring AI's `RestClient` so a hung LLM call cannot block a thread indefinitely
- All retry config (`maxAttempts`, `baseDelayMs`) injected from config — changing retry behaviour requires only a `application.yaml` change

**Week 3 — LLM API Integration (Extension 5)**
- Estimates cost per request using per-model input and output token prices from `application.yaml`
- Pricing is defined alongside each model in config (primary and fallback) and resolved by name at log time via `findPricing()`
- Model name passed from the call site rather than read from the API response — avoids mismatch between config names (`gpt-4o`) and versioned snapshot names returned by the API (`gpt-4o-2024-08-06`)
- Cost logged as `LLM cost — model: gpt-4o, estimated: $0.001338` with six decimal places — rounding to two would show `$0.00` for most requests

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
| Model override mechanism | 3 | Per-request `OpenAiChatOptions` | Avoids a second `ChatClient` bean; fallback model is a config value, not a wiring decision |
| Fallback trigger condition | 3 | `LlmUnavailableException` only | Parse errors are prompt failures, not provider failures — routing them to fallback would mask prompt bugs |
| Retry extraction | 3 | `attemptAnalyze` / `attemptClassify` private methods | Single responsibility — public method decides fallback, private method decides retry |
| Backoff strategy | 3 | Exponential backoff with full jitter | Linear backoff doesn't reduce load fast enough; fixed backoff causes thundering herd — jitter spreads retries across time |
| `Retry-After` handling | 3 | Header takes priority over calculated backoff | Server knows better than the client how long it needs; ignoring it risks continued 429s |
| `ParseException` in retry loop | 3 | Fast-fail — throw immediately, no retry | Bad output won't improve on retries; retrying wastes time and adds backoff delay on a guaranteed failure |
| Timeout configuration | 3 | `LlmClientConfig` wires values from `application.yaml` | Timeouts belong on the HTTP client, not in application logic; externalizing them means no code change to tune them |
| Cost tracking model name | 3 | Passed from call site (config value) | API returns versioned snapshot names; config has short names — they don't match for pricing lookup. Call site always knows the intended model name |
| Pricing config location | 3 | Nested under each model in `app.llm.models` | Price is a property of a model, not a global config — grouping them together makes it obvious which prices apply to which model |

Detailed reasoning for each decision lives in [`decisions.md`](./decisions.md).
