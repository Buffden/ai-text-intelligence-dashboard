# Requirements

## Functional Requirements

### Core Analysis

| ID | Requirement |
| -- | ----------- |
| F-01 | The system accepts a plain text input from the user via the frontend |
| F-02 | The system returns a summarization of the input text in two sentences |
| F-03 | The system returns a sentiment classification — positive, negative, or neutral — with a confidence score |
| F-04 | The system returns a category or topic domain for the input text |
| F-05 | The system extracts key entities from the text — people, places, organizations, and concepts |
| F-06 | All analysis results are returned as a single structured JSON response |

### API

| ID | Requirement |
| -- | ----------- |
| F-07 | The backend exposes a REST endpoint that accepts text and returns the structured analysis |
| F-08 | The LLM is called exclusively from the backend — never from the frontend |
| F-09 | The API validates the request before sending it to the LLM |
| F-10 | The API validates and parses the LLM response before returning it to the client |

### Frontend

| ID | Requirement |
| -- | ----------- |
| F-11 | The Angular frontend provides a text input area for the user to submit content |
| F-12 | The frontend displays each analysis result — summary, sentiment, topics, entities — in a readable format |
| F-13 | The frontend shows a loading state while the analysis is in progress |
| F-14 | The frontend displays a clear error message if the request fails |

---

## Non-Functional Requirements

### Reliability

| ID | Requirement |
| -- | ----------- |
| NF-01 | The backend retries the LLM call once on failure before returning an error |
| NF-02 | The backend handles LLM API rate limit errors gracefully with an appropriate error response |
| NF-03 | The backend handles malformed or unparseable LLM responses without crashing |

### Observability

| ID | Requirement |
| -- | ----------- |
| NF-04 | Every LLM request logs the input token count, output token count, and model used |
| NF-05 | Every LLM request logs the latency from request sent to response received |
| NF-06 | Errors from the LLM API are logged with enough context to debug without reproducing |

### Security

| ID | Requirement |
| -- | ----------- |
| NF-07 | The LLM API key is never hardcoded — loaded from environment variables only |
| NF-08 | The API validates that input text is not empty and does not exceed a reasonable length limit |

### Performance

| ID | Requirement |
| -- | ----------- |
| NF-09 | The backend responds within 10 seconds for typical inputs under 500 words |
| NF-10 | The frontend does not block the UI thread while waiting for the analysis response |

---

## Out of Scope

- User authentication or accounts
- Storing analysis history
- Streaming responses
- Multi-language support
- File upload (PDF, DOCX)

These are intentionally deferred. This project is about getting the core LLM application stack right, not feature completeness.
