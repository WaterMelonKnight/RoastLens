# RoastLens

RoastLens is an open-source, configurable commentary agent that generates **sharp, witty, evidence-driven** analysis while staying within legal and ethical boundaries.

It is built for local-first MVP speed, clear architecture, and future growth into SaaS, private deployment, or open-core products.

## Why RoastLens

- **Configurable personas**: plug in style profiles without hard-coding business logic
- **Evidence-driven commentary**: output emphasizes reasons, risks, and uncertainty
- **Domain extensibility**: finance, tech, general today; easy to add more tomorrow
- **Safe but sharp tone**: witty style with built-in safety and compliance boundaries
- **Easy self-hosting**: Spring Boot + Docker + Compose

## MVP Features

- Web page for input text + domain + persona selection
- `POST /api/analyze` structured analysis API
- Structured output fields:
  - `summary`
  - `evidencePoints` (3 items)
  - `counterPoint`
  - `confidenceNote`
  - `disclaimer`
  - `styleMeta`
- Persona templates loaded from `personas.yml`
- Domain templates loaded from `domains.yml`
- OpenAI-compatible LLM provider abstraction

## Tech Stack

- Java 17
- Spring Boot 3.x
- Maven
- WebClient (reactive HTTP client)
- YAML-based configuration
- Static HTML/CSS/JS frontend
- Docker + docker-compose

## Screenshot

> Add screenshot here (placeholder)

`docs/screenshot-placeholder.png`

## Quick Start (Local)

### 1) Clone and prepare env

```bash
git clone <your-repo-url>
cd RoastLens
cp .env.example .env
```

Fill `.env` with your provider settings, especially `ROASTLENS_LLM_API_KEY`.

### 2) Run with Maven

```bash
mvn spring-boot:run
```

Open `http://localhost:8080`.

## Baseline verification

The MVP request path is `Web UI -> POST /api/analyze -> RoastAnalysisService -> PromptBuilder -> LlmClient -> structured response`. Run the automated baseline with:

```bash
mvn test
mvn package
```

The tests start the Spring application context, exercise `/api/meta` and `/api/analyze`, and verify structured-output parsing, validation, normalization, and finance disclaimer behavior. They replace `LlmClient` with mocks and never call a real LLM API. GitHub Actions runs the test suite and then packages the executable jar for every pull request and every push to `main`.

## Quick Start (Docker)

```bash
cp .env.example .env
# edit .env
docker compose up --build
```

Open `http://localhost:8080`.

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `ROASTLENS_LLM_PROVIDER` | no | `openai-compatible` | Provider identifier (MVP currently uses OpenAI-compatible protocol) |
| `ROASTLENS_LLM_BASE_URL` | no | `https://api.openai.com` | API base URL |
| `ROASTLENS_LLM_API_KEY` | yes | - | Provider API key |
| `ROASTLENS_LLM_MODEL` | no | `gpt-4o-mini` | Model name |
| `ROASTLENS_LLM_TEMPERATURE` | no | `0.4` | Generation temperature |
| `ROASTLENS_LLM_TIMEOUT_SECONDS` | no | `45` | Request timeout |
| `ROASTLENS_LLM_USE_JSON_RESPONSE_FORMAT` | no | `false` | Whether to request JSON response format on compatible providers |
| `ROASTLENS_DEFAULT_LANGUAGE` | no | `zh-CN` | Default candidate-text language; supported values are `zh-CN` and `en-US` |
| `ROASTLENS_FINSTREAM_BASE_URL` | no | `http://localhost:8081` | FinStream REST API base URL (override if FinStream uses another port) |
| `ROASTLENS_FINSTREAM_TIMEOUT_SECONDS` | no | `5` | FinStream request timeout |
| `ROASTLENS_ROASTABILITY_THRESHOLD` | no | `0.6` | Inclusive score threshold for generating candidates |
| `ROASTLENS_ROAST_MAX_BATCH_SIZE` | no | `20` | Maximum unique abnormal events processed per manual request |

### Provider switch examples (OpenAI-compatible)

RoastLens MVP can switch providers by changing `BASE_URL + API_KEY + MODEL`.

```bash
# OpenAI
ROASTLENS_LLM_BASE_URL=https://api.openai.com
ROASTLENS_LLM_MODEL=gpt-4o-mini

# DeepSeek
ROASTLENS_LLM_BASE_URL=https://api.deepseek.com
ROASTLENS_LLM_MODEL=deepseek-chat

# Kimi / Moonshot
ROASTLENS_LLM_BASE_URL=https://api.moonshot.cn/v1
ROASTLENS_LLM_MODEL=moonshot-v1-8k

# OpenRouter
ROASTLENS_LLM_BASE_URL=https://openrouter.ai/api/v1
ROASTLENS_LLM_MODEL=openai/gpt-4o-mini
```

If a provider has weak support for JSON response format, keep:

```bash
ROASTLENS_LLM_USE_JSON_RESPONSE_FORMAT=false
```

## API

### FinStream integration

RoastLens treats FinStream as the owner of the market-event schema and uses a thin REST connector to map only the fields needed by `FinancialEventInput`. To try the integration locally:

1. Start FinStream and identify an existing FinancialEvent `eventId`.
2. Set `ROASTLENS_FINSTREAM_BASE_URL` to its REST base URL (FinStream currently defaults to `http://localhost:8081`).
3. Start RoastLens and invoke:

```bash
curl -X POST \
  "http://localhost:8080/api/v1/roasts/from-finstream/{eventId}?lang=zh-CN"
```

This integration is **manual trigger only** and **REST connector only**. It does not discover FinStream automatically. Polling, scheduling, persistence, permanent processed-event deduplication, automatic publishing, and MCP integration are not supported.

The call fetches `GET /api/v1/events/{eventId}`, maps the response at the connector boundary, and delegates candidate generation to the existing `FinancialEventRoastService`. FinStream 404 responses become API 404 responses; unavailable, timeout, upstream 5xx, empty, malformed, or incompatible responses become stable 502 responses without exposing upstream response bodies.

### Abnormal event batch generation

Trigger one bounded batch manually (there is no request body):

```bash
curl -X POST \
  "http://localhost:8080/api/v1/roasts/from-finstream/abnormal?lang=zh-CN"
```

The optional request language overrides `ROASTLENS_DEFAULT_LANGUAGE`. The default is `zh-CN`, and `en-US` is also
supported. Only candidate `text` is localized: JSON field names, `style`, `riskLevel`, symbols, and `eventType` remain
machine-readable values. Unsupported locales return HTTP 400 rather than silently falling back.

FinStream detects abnormal market events. RoastLens does not repeat that detector decision: its existing “roastability” name means **content worthiness and content novelty**—whether an already-abnormal event is worth turning into content. RoastLens consumes FinStream's current `GET /api/v1/events/abnormal` contract in FinStream order and maps only the fields it needs; extra owner fields such as `evidence` are ignored.

The deterministic content-worthiness score is clamped to `0.0..1.0`: `0.25` base; at most `0.20` for severity and `0.15` for normalized anomaly strength; `0.05` each for a known event type and the small BTC/ETH relevance signal; smooth capped contributions up to `0.10` each for absolute five-minute return and volume ratio; and `0.15` when a meaningful price move and elevated volume occur together. FinStream's unbounded `anomalyScore` is normalized over `0..4`. Unknown types remain eligible, while missing or malformed optional metrics contribute nothing. Reasons describe content value (for example, a strong combined move or limited content value), not a second market-anomaly verdict.

The batch performs, in order: (1) event-ID dedupe, (2) same `symbol|eventType` suppression, (3) escalation detection, (4) the bounded content-worthiness evaluation, and (5) LLM generation only for selected events. The first duplicate-key occurrence is selected. A later occurrence is selected as a new representative only if anomaly score or volume ratio rises by at least 50%, severity rises from medium to high/critical, or—only for rapid pump/drop events—the absolute five-minute return rises by at least 50%. Small repeats are suppressed as “Duplicate event without meaningful escalation.” This filtering preserves upstream order and happens before `ROASTLENS_ROAST_MAX_BATCH_SIZE` (default 20), so duplicates do not consume useful batch slots.

Scores at or above `ROASTLENS_ROASTABILITY_THRESHOLD` generate candidates; lower scores are returned as `SKIP` without invoking the LLM. A candidate-generation failure is returned as an item-level `ERROR` and later items continue, while a failure fetching the abnormal-event list remains a request-level 502.

Duplicate suppression is deliberately scoped to one request. This endpoint has no cross-request cooldown, persistent processed tracking, scheduler, database, content inventory, or publishing.

### POST `/api/v1/roasts`

Submit a standardized `FinancialEvent` manually and receive 3–5 content candidates. This API does not persist results, schedule work, publish content, or change the manual analyze Web UI.

Request:

```json
{
  "id": "event-id",
  "source": "BINANCE",
  "symbol": "BTCUSDT",
  "eventType": "RAPID_DROP",
  "eventTime": "2026-08-20T10:30:00Z",
  "detectedAt": "2026-08-20T10:30:03Z",
  "severity": 0.9,
  "anomalyScore": 1.8,
  "summary": "BTC dropped rapidly with abnormal volume",
  "metrics": {
    "return5m": -5.8,
    "volumeRatio": 7.3
  }
}
```

Response:

```json
{
  "eventId": "event-id",
  "candidates": [
    {
      "text": "BTC just used five minutes to remind the market that digital gold can still free-fall.",
      "style": "dry",
      "riskLevel": "low"
    },
    {
      "text": "The long-term narrative briefly met the short-term elevator shaft.",
      "style": "deadpan",
      "riskLevel": "low"
    },
    {
      "text": "Market confidence remains available, subject to sudden five-minute maintenance.",
      "style": "sarcastic",
      "riskLevel": "medium"
    }
  ]
}
```

### POST `/api/analyze`

Request:

```json
{
  "text": "Company X says it will triple revenue in 12 months while cutting burn by 80%.",
  "domain": "finance",
  "persona": "sharp_analyst"
}
```

Response:

```json
{
  "summary": "Bold claim, thin proof: this reads more like pitch-deck optimism than operational reality.",
  "evidencePoints": [
    "Revenue tripling and massive burn reduction in parallel is operationally rare.",
    "No unit economics path or demand evidence is presented.",
    "Timeline risk is high given execution and market uncertainty."
  ],
  "counterPoint": "If the firm has undisclosed contracted demand, the upside case could be underappreciated.",
  "confidenceNote": "Confidence is moderate because key data (pipeline quality, margins, cash runway) is missing.",
  "disclaimer": "This content is for informational and educational purposes only and does not constitute investment advice.",
  "styleMeta": {
    "persona": "sharp_analyst",
    "domain": "finance",
    "tone": "Direct, witty, data-first",
    "analysisFocus": "Business model quality, valuation narrative, catalyst credibility, risk asymmetry"
  }
}
```

### GET `/api/meta`

Returns available domains and personas.

## Project Structure

```text
src/main/java/com/roastlens
├── controller      # API controllers
├── service         # Analysis orchestration
├── model           # DTOs and config models
├── llm             # LLM abstraction + providers
├── persona         # Persona config registry
├── domain          # Domain config registry
├── prompt          # Prompt assembly
├── safety          # Safety policy and boundaries
└── config          # Spring config/properties
```

## Safety and Disclaimer

RoastLens is designed for sharp commentary, not abuse.

- No hateful, discriminatory, violent, or illegal content
- No direct investment instruction in finance mode
- Mandatory informational disclaimer in finance outputs

## Roadmap

- Multi-provider adapters (Anthropic / Gemini / OpenRouter)
- JSON Schema-based strict output validation and retry strategy
- Domain packs (business, workplace, current affairs)
- Prompt template versioning and governance
- Optional persistence and audit trail
- SaaS-ready multi-tenant auth/billing layer

## License

MIT (recommended for open-source adoption; add `LICENSE` file as needed)
