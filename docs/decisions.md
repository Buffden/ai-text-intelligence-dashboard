# Architecture Decision Records

A log of key decisions made during this project and the reasoning behind them. The goal isn't to justify every choice — it's to record the thinking so future-me (and anyone reading the code) understands why things are the way they are.

---

## ADR-01: Spring AI over direct HTTP calls to LLM APIs

**Decision:** Use Spring AI 1.1.5 as the LLM integration layer rather than calling OpenAI/Claude APIs directly via RestTemplate or WebClient.

**Why:**

Calling the LLM API directly works fine for a single provider but creates tight coupling — every provider has a different request/response shape, authentication method, and error format. Spring AI abstracts all of that behind a consistent `ChatClient` interface. Switching from OpenAI to Claude, or adding Bedrock later, becomes a configuration change rather than a code rewrite.

Spring AI also handles retry configuration, streaming, and structured output enforcement out of the box. That's boilerplate that doesn't need to be written from scratch.

**Tradeoff:** Spring AI adds a dependency and a layer of abstraction. If something breaks at the Spring AI layer, debugging requires understanding both Spring AI and the underlying provider API.

---

## ADR-02: Temperature set to 0 for all analysis requests

**Decision:** All LLM calls in this project use temperature 0.

**Why:**

This project does structured extraction — summarization, classification, entity pulling. These tasks need consistent, deterministic output. A higher temperature introduces randomness that makes the output harder to validate and test. The same input should produce the same output every time.

**Tradeoff:** Temperature 0 makes the model less "creative." That's fine here — creativity is not the goal.

---

## ADR-03: JSON mode + explicit system prompt for structured output

**Decision:** Enforce structured output using both JSON mode (where the provider supports it) and an explicit system prompt that specifies the exact JSON shape.

**Why:**

JSON mode alone ensures the output is valid JSON but doesn't enforce the schema — the model might return different field names or omit fields. The system prompt adds the schema constraint on top. Together, they significantly reduce malformed responses.

The backend still validates and parses the response before using it — this is the third layer of defense.

**Tradeoff:** Slightly longer system prompt means slightly more input tokens per request. The cost is negligible and the reliability gain is worth it.

---

## ADR-04: Single retry on LLM failure

**Decision:** If the LLM call fails or returns an unparseable response, retry exactly once before returning an error to the client.

**Why:**

LLM APIs occasionally return transient errors or malformed responses that succeed on a second attempt. A single retry catches most of these without adding meaningful latency in the success case. More than one retry risks hammering the API during an actual outage and adds user-facing latency.

**Tradeoff:** One retry means some failures that would have succeeded on a third attempt get surfaced to the user. That's an acceptable tradeoff.

---

## ADR-05: Token usage logged on every request

**Decision:** Every LLM request logs input tokens, output tokens, model name, and latency.

**Why:**

AI applications have real, variable costs that scale with usage. Building the logging habit now — even on a small project — means the pattern is in place when it matters. It also makes debugging easier: when something goes wrong, having the token counts and latency in the logs narrows down whether the problem was prompt length, response length, or API latency.

**Tradeoff:** Slightly more verbose logs. Not a real tradeoff.
