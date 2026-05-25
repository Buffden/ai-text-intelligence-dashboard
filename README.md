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

---

## Tech Stack

| Layer | Choice |
| ----- | ------ |
| Frontend | Angular |
| Backend | Spring Boot + Spring AI 1.1.5 |
| AI API | OpenAI / Claude |
| Language | Java (backend), TypeScript (frontend) |

---

## API Response Shape

Every text analysis request returns:

```json
{
  "summary": "A two-sentence summary of the text",
  "sentiment": "positive | negative | neutral",
  "confidence": 0.87,
  "key_topics": ["topic1", "topic2"],
  "word_count_estimate": 142
}
```

---

## What This Project Covers

- Writing system prompts that enforce structured JSON output reliably
- Validating and parsing LLM responses before trusting them
- Handling API errors, rate limits, and malformed responses
- Logging token usage per request from day one
- Building a proper backend service layer — LLM is a component, not the whole app

