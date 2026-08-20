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

### POST `/api/v1/roasts`

Submit a standardized `FinancialEvent` manually and receive 3–5 content candidates. This API currently does **not** fetch events from FinStream REST, persist results, schedule work, publish content, or change the manual analyze Web UI.

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
