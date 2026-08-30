# RoastLens

RoastLens is an open-source, configurable commentary agent that generates **sharp, witty, evidence-driven** analysis while staying within legal and ethical boundaries.

It is built for local-first MVP speed, clear architecture, and future growth into SaaS, private deployment, or open-core products.

## Docker Quick Start

Docker is the quickest way to run RoastLens with durable local data:

```bash
git clone <your-repo-url>
cd RoastLens
cp .env.example .env
# Edit .env with the LLM provider and FinStream values you use.
docker compose up -d
```

Open <http://localhost:8080>. Polling defaults to **disabled**; set
`ROASTLENS_POLLING_ENABLED=true` only when you intentionally want automatic FinStream and LLM requests.

Useful commands:

```bash
docker compose ps
docker compose logs -f
docker compose down
```

`docker compose down` removes the container and network but **does not delete** the named H2 data volume.
Use `docker compose down -v` only when you explicitly want to permanently delete all persisted RoastLens content.
Do not commit `.env`; it is ignored by Git and should contain private values only on your machine.

## Why RoastLens

- **Configurable personas**: plug in style profiles without hard-coding business logic
- **Evidence-driven commentary**: output emphasizes reasons, risks, and uncertainty
- **Domain extensibility**: finance, tech, general today; easy to add more tomorrow
- **Safe but sharp tone**: witty style with built-in safety and compliance boundaries
- **Easy self-hosting**: Spring Boot + Docker + Compose

## MVP Features

- Built-in static UI centered on Content Inventory, coherent Human Review, and approved social-card output; the manual Analyze Playground remains available as a collapsed developer tool
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
- Spring Data JPA with file-backed H2 for zero-setup local persistence
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

## Container deployment

The multi-stage image builds the executable jar with Maven and runs it on a Java 17 JRE as an unprivileged
`roastlens` user. Compose mounts the `roastlens-data` named volume at `/app/data`; the container-only JDBC
configuration writes H2 to `/app/data/roastlens`, while Maven/local startup continues to use
`./data/roastlens`. Application logs remain on stdout/stderr.

The health check calls `GET /actuator/health`. Only the Actuator health endpoint is exposed, with details hidden.
It verifies the application and its local dependencies started; it does not contact FinStream or the LLM. Neither
upstream is checked during startup, so temporary upstream outages do not prevent RoastLens from starting.

### Docker networking and FinStream

`FINSTREAM_BASE_URL` accepts a hosted URL, a custom/self-hosted URL, or a Docker service URL without code
changes. Inside a container, `localhost` means the **RoastLens container itself**, not the host. For FinStream
running on the host, use `http://host.docker.internal:8081` where the Docker platform supports it. Linux setups
that do not provide that hostname may need a reachable host address or additional Docker host-gateway setup.

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
| `FINSTREAM_BASE_URL` | no | local: `http://localhost:8081`; Compose: `http://host.docker.internal:8081` | FinStream REST API base URL |
| `ROASTLENS_FINSTREAM_BASE_URL` | no | - | Backward-compatible higher-priority alias for the FinStream base URL |
| `ROASTLENS_FINSTREAM_TIMEOUT_SECONDS` | no | `5` | FinStream request timeout |
| `ROASTLENS_POLLING_ENABLED` | no | `false` | Enable automatic FinStream abnormal-event polling |
| `ROASTLENS_POLLING_INTERVAL_MS` | no | `3600000` | Fixed delay after a polling run finishes |
| `ROASTLENS_POLLING_INITIAL_DELAY_MS` | no | `60000` | Delay before the first polling run |
| `ROASTLENS_POLLING_LANGUAGE` | no | `zh-CN` | Candidate language used by the polling batch |
| `ROASTLENS_ROASTABILITY_THRESHOLD` | no | `0.6` | Inclusive score threshold for generating candidates |
| `ROASTLENS_ROAST_MAX_BATCH_SIZE` | no | `20` | Maximum unique abnormal events processed per manual request |
| `ROASTLENS_DATASOURCE_URL` | no | `jdbc:h2:file:./data/roastlens;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE` | JDBC URL for content inventory persistence |
| `ROASTLENS_DATASOURCE_USERNAME` | no | `sa` | Database username |
| `ROASTLENS_DATASOURCE_PASSWORD` | no | empty | Database password |
| `ROASTLENS_DATASOURCE_DRIVER` | no | `org.h2.Driver` | JDBC driver class (override with the URL for PostgreSQL) |

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


## Deployment modes

RoastLens is deliberately independent of where FinStream runs:

- **Mode A — Hosted FinStream:** Hosted FinStream → local RoastLens. Set `FINSTREAM_BASE_URL` to the hosted HTTPS URL. No official public hosted URL is hard-coded.
- **Mode B — Custom FinStream:** user-managed FinStream → local RoastLens. Set `FINSTREAM_BASE_URL` to the reachable custom URL.
- **Mode C — Future Full Local:** one Docker Compose project containing FinStream and RoastLens, using `http://finstream:8080`. **The combined compose is not implemented in this PR.** The current compose contains RoastLens only.

API keys remain environment-based. There is no Settings UI, browser secret entry, or database secret storage.

## Container manual smoke-test checklist

1. Copy `.env.example` to `.env`.
2. Configure current working LLM values.
3. Configure the FinStream URL.
4. Run `docker compose up -d`.
5. Confirm `docker compose ps` shows RoastLens healthy.
6. Open <http://localhost:8080>.
7. Confirm the existing UI loads.
8. Confirm Content Inventory works.
9. Confirm Human Review works.
10. Confirm an approved Image Card works.
11. Restart the container.
12. Confirm previously persisted H2 content remains.
13. Confirm `docker compose logs` contains no API key.
14. Confirm polling remains disabled unless explicitly enabled.

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

This integration uses the REST connector only. Automatic polling is disabled by default, and neither manual nor scheduled execution publishes content.

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

The batch performs, in order: (1) event-ID dedupe, (2) same `symbol|eventType` suppression, (3) escalation detection, (4) the bounded content-worthiness evaluation, and (5) LLM generation only for selected events. The first duplicate-key occurrence is selected. A later occurrence is selected as a new representative only if anomaly score or volume ratio rises by at least 50%, severity rises from medium to high/critical, or—only for rapid pump/drop events—the absolute five-minute return rises by at least 50%. Small repeats remain visible in the response as `SKIP` with reason “Duplicate event without meaningful escalation,” but do not invoke the evaluator or LLM. `processed` counts all examined response items, and `skipped` includes both duplicate suppression and content-worthiness skips. This filtering preserves upstream order and happens before `ROASTLENS_ROAST_MAX_BATCH_SIZE` (default 20), so duplicates do not consume useful selected-generation slots.

Scores at or above `ROASTLENS_ROASTABILITY_THRESHOLD` generate candidates; lower scores are returned as `SKIP` without invoking the LLM. A candidate-generation failure is returned as an item-level `ERROR` and later items continue, while a failure fetching the abnormal-event list remains a request-level 502.

The processing path is now `FinStream -> FinancialEvent -> processed-event check -> request novelty/content-worthiness -> candidate generation -> Content Inventory`. `sourceEventId` is a unique business key. A persisted event is returned by the batch as an execution-time `SKIP` with reason `Already processed`; its original inventory status is not changed and neither the evaluator nor LLM is called. Same-request weak novelty duplicates remain unpersisted.

Generated candidates and content-worthiness `SKIPPED` outcomes are retained. Generation errors are retained as `FAILED`, and are not automatically retried; retry policy belongs in a later PR. The single-event endpoint returns retained candidates for an existing `GENERATED` item, or an empty candidate list for a retained `SKIPPED`/`FAILED` item, without calling the LLM again. If a caller requests a language different from the retained item's language, the endpoint returns `409 Conflict` rather than returning candidates in the wrong language. Multilingual variants may be supported in a later PR.

### Scheduled abnormal-event polling

RoastLens can invoke the same abnormal-event batch pipeline on a configurable fixed delay. It is **disabled by default**, so a normal local startup makes no automatic FinStream or LLM calls. Enable it explicitly with:

```bash
ROASTLENS_POLLING_ENABLED=true
ROASTLENS_POLLING_INTERVAL_MS=3600000
ROASTLENS_POLLING_INITIAL_DELAY_MS=60000
ROASTLENS_POLLING_LANGUAGE=zh-CN
```

For a short local smoke test:

```bash
ROASTLENS_POLLING_ENABLED=true \
ROASTLENS_POLLING_INTERVAL_MS=60000 \
ROASTLENS_POLLING_INITIAL_DELAY_MS=5000 \
ROASTLENS_POLLING_LANGUAGE=zh-CN \
mvn spring-boot:run
```

The flow is `Spring scheduler -> existing abnormal-event pipeline -> processed-event check -> content-worthiness -> candidate generation -> Content Inventory`. The interval is a fixed delay measured after a run finishes. One configured language is used for each scheduler instance. The scheduler reuses Content Inventory's durable `sourceEventId` tracking, including the existing treatment of `FAILED` items; it does not add a second dedupe or retry mechanism.

An in-process guard prevents overlapping runs in one JVM, and top-level failures are logged and released for the next interval. This is not a distributed scheduling guarantee: multiple application instances can each poll. There is no automatic publishing, review workflow, scheduler UI, or frontend auto-refresh; Content Inventory refresh remains manual.

### Content Inventory

Read-only inventory endpoints are available:

```bash
curl "http://localhost:8080/api/v1/content?limit=20"
curl "http://localhost:8080/api/v1/content/{contentItemId}"
curl "http://localhost:8080/api/v1/content/by-event/{sourceEventId}"
```

The built-in UI at `http://localhost:8080` makes the responsive Content Inventory dashboard the primary workspace. It loads recent persisted items, supports client-side status/language/symbol filtering, and groups candidate comparison, selection, final-text editing, approval, and rejection into one Human Review workspace. The existing Analyze Playground remains available in a collapsed **Developer tools** section.

The current default and supported runtime database is file-backed H2 at `./data/roastlens`, so content survives application restarts and requires no Docker database. Hibernate schema update is used for this MVP. The schema and entity design are intended to remain PostgreSQL-compatible, but the PostgreSQL JDBC driver and deployment support are not included yet and will be added later. The inventory contains `ContentItem` source metadata, score, language, `GENERATED`/`SKIPPED`/`FAILED` status, timestamps, and atomically persisted `ContentCandidate` rows.

There is still no automatic publishing, AI image generation, video generation, or distributed scheduler lock.

### POST `/api/v1/roasts`

Submit a standardized `FinancialEvent` manually and receive 3–5 content candidates. This API does not persist results, schedule work, publish content, or change the manual analyze Web UI.

For `zh-CN`, generation is optimized for short-form social financial commentary: compact setup-and-punchline
writing, contrast, deadpan or lightly sarcastic humor, and natural Chinese phrasing rather than financial-report prose.
The humor remains grounded only in supplied event facts; an unknown cause stays unknown and may be the subject of the
joke, but is never invented. Each `RoastCandidate` is reviewable draft content only—not automatically published content.

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

## Human review workflow

Persisted content now follows this deliberately small workflow:

`FinancialEvent → generation → Content Inventory → human review → APPROVED / REJECTED`

Generation outcome (`GENERATED`, `SKIPPED`, or `FAILED`) remains separate from review status. New generated items begin as `PENDING`; skipped and failed items have no review status and cannot be reviewed. For compatibility with H2 databases created by earlier versions, all review columns are nullable and generated rows whose stored review status is null are presented as `PENDING`.

A reviewer selects a generated candidate in Content Inventory, optionally edits its text, and approves it. Generated `ContentCandidate.text` is immutable historical output; the final human text is stored separately as `ContentItem.reviewedText`. A reviewer can also reject generated content with an optional reason. The latest decision wins, so generated items may be approved after rejection or re-approved without introducing revision/audit history.

Review endpoints:

- `POST /api/v1/content/{id}/approve` with `{"candidateId":"...","reviewedText":"optional edited text"}`. The candidate must belong to the item. Missing or blank edited text uses the original candidate text.
- `POST /api/v1/content/{id}/reject` with an optional `{"reason":"..."}` body.

Both return the updated Content Inventory response. Review writes are transactional. This workflow does **not** publish content automatically and adds no publishing integrations, image generation, or video generation.

## Approved image cards

The current end-to-end content workflow is:

`FinStream → FinancialEvent → RoastLens generation → Content Inventory → Human Review → Approved Output → Image Card`

An image card is available only when a content item has generation status `GENERATED`, review status `APPROVED`, and nonblank persisted `reviewedText`. The canonical 1200 × 1200 SVG uses the approved edited text—not an unapproved candidate—and is rendered deterministically on demand:

```bash
curl "http://localhost:8080/api/v1/content/{contentItemId}/card.svg" > roastlens-card.svg
```

Pending, rejected, skipped, and failed content returns `409 Conflict`; an unknown item returns `404 Not Found`. Card bytes are not stored in the database or filesystem. The renderer uses escaped SVG text and deterministic wrapping, calls no LLM or image-generation service, and requires no desktop graphics environment.

After approval, **Approved Output** immediately shows the persisted approved text and same-origin SVG preview. **Download SVG** fetches the canonical card, while **Download PNG** performs lightweight in-browser SVG-to-canvas conversion. Refreshing or re-approving derives the preview from the latest persisted content; rejecting removes it while retaining the original candidates. Automatic social publishing is not implemented.
