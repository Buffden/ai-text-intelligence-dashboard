# AI Text Intelligence Dashboard

A full-stack AI application that accepts text input and returns structured intelligence — summarization, sentiment analysis, category classification, and entity extraction.

The goal of this project isn't to build something flashy. It's to make sure every piece of the stack connects properly: prompt → LLM API → validated structured response → frontend display. Getting this foundation right is what makes everything that comes after it easier to build and debug.

---

## Features

| Feature | Description |
| ------- | ----------- |
| Text summarization | Condenses input into a concise two-sentence summary |
| Sentiment analysis | Classifies tone as positive, negative, or neutral with a confidence score |
| Category classification | Identifies the category or topic domain of the text |
| Entity extraction | Pulls out key people, places, organizations, and concepts |
| Angular frontend | Clean UI for submitting text and viewing structured results |
| Backend API layer | Spring Boot + Spring AI service that handles all LLM communication — never called directly from the frontend |
| Multi-provider fallback | Automatically routes to a fallback model when the primary is unavailable — transparent to the client |

---

## Tech Stack

| Layer | Choice |
| ----- | ------ |
| Frontend | Angular |
| Backend | Spring Boot + Spring AI 1.1.6 |
| AI API | OpenAI / Claude |
| Language | Java (backend), TypeScript (frontend) |

---

## API Endpoints

### `POST /api/analyze`

Returns a full structured analysis of the input text:

```json
{
  "summary": "A two-sentence summary of the text.",
  "sentiment": "positive | negative | neutral",
  "confidence": 0.87,
  "key_topics": ["topic1", "topic2"],
  "word_count_estimate": 142
}
```

### `POST /api/classify`

Returns a focused category classification with chain-of-thought reasoning:

```json
{
  "reasoning": "The text discusses AI investment and automation, which maps to technology.",
  "category": "technology | politics | sports | business | health | other",
  "confidence": 0.91
}
```

Both endpoints accept the same request body:

```json
{
  "text": "Your input text here"
}
```

---

## What This Project Covers

- Writing system prompts that enforce structured JSON output reliably
- Few-shot prompting to improve classification consistency across categories
- Using a reasoning scratchpad inside JSON output for chain-of-thought classification
- Hardening prompts against injection using input delimiters and explicit role instructions
- Validating and parsing LLM responses before trusting them
- Handling API errors, rate limits, and malformed responses
- Logging token usage per request from day one
- Building a proper backend service layer — LLM is a component, not the whole app
- Multi-provider fallback — routing to a secondary model on primary failure without changing the API contract
- Config-driven provider selection — switching models requires only a `application.yaml` change, no code changes
- Exponential backoff with full jitter — retry delays grow between attempts and are randomised to prevent thundering herd
- `Retry-After` header support — on 429 responses the server-specified delay is used instead of the calculated backoff
- Connect and read timeouts — externalized to config, wired into the HTTP client so hung LLM calls fail fast instead of blocking threads indefinitely
- Parse errors fast-fail — malformed LLM responses throw immediately without consuming retry attempts or triggering fallback
- Per-request cost estimation — input and output token prices configured per model, cost logged after every successful call with six decimal places
