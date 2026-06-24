# Bean Book

> Track your coffee bean purchases, roasts, and brewing notes — a mobile-first, installable PWA you can self-host.

Bean Book is a personal coffee log. Record every bag you buy — roaster, origin, process, roast level,
price, and weight — follow each bag from sealed to opened to finished, rate what you drink, keep a
wishlist of beans to try, and watch where your money and taste actually go through built-in analytics.
It is a single Spring Boot service backed by SQLite, built to run comfortably on a homelab box and to
install onto your phone's home screen like a native app.

## Features

### Bean log

- Record purchases with name, roaster, origin, process, roast level, and roast profile (espresso / filter / omni)
- Track whole-bag price and weight, purchase date, and roast date
- Follow each bag's lifecycle: **sealed → open → finished** (derived from opened/finished dates)
- Rate beans 1–5, attach brewing notes, grind settings, freeform tags, a product/roaster URL, and a bag photo
- For omni beans, capture what the bag was actually brewed as (espresso or filter)

### AI-assisted entry (optional, off by default)

- **Auto-fill from a bag photo** — upload a photo of the bag and have the fields (name, roaster, origin,
  roast level, process, roast profile, weight, roast date, tasting notes) read off it and pre-filled.
- **Auto-fill from a link** — paste a product or roaster URL and pre-fill the same fields from the page.
- You always confirm before saving: AI only fills **blank** fields, never overwrites what you typed, and
  flags suggested values with a subtle accent that clears the moment you edit them.
- **Off by default** and completely hidden unless enabled. Opt in with an OpenAI key (see
  [*Configuration*](#ai-assisted-entry-opt-in)); a privacy-friendly local-model path (Ollama) is prepared
  for a future release.

### Analytics

- Total and average spend across your collection
- Spend over time, zero-filled month by month so the timeline never collapses gaps
- Spend and count broken down by bean, roaster, origin, brew method, and roast profile
- Highlights: most common origin, most expensive bean, most expensive roaster
- Consumption pace (days from opened to finished) and a projected monthly cost

### Wishlist

- Keep a list of beans to try, with roaster, origin, notes, and a link

### Backup & data

- Export your full library (purchases + wishlist) to JSON
- Import JSON backups, with tolerant parsing: legacy fields are migrated, duplicate ids are collapsed,
  and unparseable records are skipped and counted rather than failing the whole import

### Progressive Web App

- Mobile-first UI with light and dark themes
- Installable to the home screen via a web app manifest (standalone display, app shortcut to "Add Purchase")
- Service worker provides an offline fallback so the app shell still loads without a connection
- Configurable currency symbol (defaults to `€`)

## Tech stack

| Layer         | Technology                                          |
|---------------|-----------------------------------------------------|
| Language      | Kotlin 2.4.0 (JVM toolchain 25)                     |
| Framework     | Spring Boot 4.1.0                                   |
| Modularity    | Spring Modulith 2.1.0                               |
| UI            | Vaadin Flow 25.1.8 (Lumo theme)                     |
| Persistence   | Spring Data JDBC + SQLite (xerial 3.53.2.0)         |
| Migrations    | Liquibase                                           |
| JSON          | Jackson (`tools.jackson` kotlin module)             |
| Build         | Gradle (Kotlin DSL), version catalog                |
| AI (optional) | Koog 1.0 (JetBrains) + OpenAI                       |
| Static checks | Detekt 2.0.0-alpha.3                                |
| Testing       | JUnit 5, Karibu Testing 2.7.0, Spring Modulith test |

## Architecture

Bean Book is a [Spring Modulith](https://spring.io/projects/spring-modulith) application. Each
top-level package under `eu.zeletrik.beanbook` is an application module with a clear public surface:

- **`beans`** — the core domain: `BeanPurchase`, its enums (`RoastLevel`, `RoastProfile`,
  `Process`, `BrewTarget`, `BagState`), and `BeanPurchaseService`.
- **`analytics`** — read-only aggregation over purchases (spend, pace, brew-method breakdowns).
- **`wishlist`** — beans you plan to buy: `WishlistItem` and `WishlistService`.
- **`backup`** — JSON `ExportService` and `ImportService` over the bean and wishlist modules.
- **`preferences`** — user preferences such as the currency symbol.
- **`ai`** — optional, feature-flagged AI-assisted entry: extracts bean fields from a bag photo or a
  product URL to pre-fill the Add form. Built on [Koog](https://github.com/JetBrains/koog) with OpenAI;
  created only when the feature is enabled.
- **`ui`** — the Vaadin views and components (the presentation layer).

**Internal-package convention:** types that are implementation details live in an `internal`
sub-package (e.g. `beans.internal`, `wishlist.internal` hold repositories and converters). Spring
Modulith treats `internal` packages as private to their module — other modules depend on the
module's public API (its top-level types), never on its internals. This keeps module boundaries
explicit and verifiable.

## Getting started

### Prerequisites

- **JDK 25** (the Gradle Kotlin toolchain targets 25; toolchain auto-download is disabled, so a JDK 25
  must be available via `JAVA_HOME`)
- No separate database to install — SQLite is embedded and the schema is applied by Liquibase on startup.

### Run in development

```bash
./gradlew bootRun
```

The app starts on [http://localhost:8001](http://localhost:8001). Defaults to write its database to `./beanbook.db` in
the working directory.

### Run tests and static analysis

```bash
./gradlew test       # JUnit 5 + Karibu UI tests + Spring Modulith verification
./gradlew detekt     # Kotlin static analysis (autocorrect enabled)
```

### Production build

```bash
./gradlew clean build
```

This runs the Vaadin production frontend build and produces an executable JAR under `build/libs/`.

## Configuration

Configuration follows Spring Boot conventions, so any property can be overridden with an environment
variable.

| Variable                   | Purpose                                           | Default         |
|----------------------------|---------------------------------------------------|-----------------|
| `DATASOURCE_PATH`          | Path where the datasource can be found            | `./beanbook.db`          |
| `BEANBOOK_AI_ENABLED`      | Turn on AI-assisted entry (photo / URL auto-fill) | `false`                  |
| `BEANBOOK_AI_PROVIDER`     | AI backend: `OPENAI` (cloud) or `OLLAMA` (local)  | `OPENAI`                 |
| `OPENAI_API_KEY`           | OpenAI API key; required when provider is `OPENAI` | _(unset)_               |
| `BEANBOOK_AI_OPENAI_MODEL` | OpenAI model: `GPT4o`, `GPT4oMini`, or `GPT5`      | `GPT4o`                  |
| `OLLAMA_BASE_URL`          | Ollama server URL; used when provider is `OLLAMA`  | `http://localhost:11434` |
| `BEANBOOK_AI_OLLAMA_MODEL` | Ollama model: `GRANITE_VISION` or `LLAMA_3_2`      | `GRANITE_VISION`         |

The server listens on port **8001**.

**Currency preference.** The currency symbol shown throughout the UI is a user preference stored in the
database (not an environment variable). It defaults to `€` and can be changed in the app's settings.

### AI-assisted entry (opt-in)

Off by default. Set `BEANBOOK_AI_ENABLED=true`, then pick a provider with `BEANBOOK_AI_PROVIDER`:

- **`OPENAI` (cloud, default)** — provide `OPENAI_API_KEY` (read from the environment only — never baked
  into the image or committed). Optionally set `BEANBOOK_AI_OPENAI_MODEL` (`GPT4o` default, `GPT4oMini`
  for lower cost, `GPT5` for more capability).
- **`OLLAMA` (local, on-box)** — no key needed; point `OLLAMA_BASE_URL` at your Ollama server
  (`http://localhost:11434` by default) and choose `BEANBOOK_AI_OLLAMA_MODEL`. The photo path needs a
  **multimodal** model: `GRANITE_VISION` (default, `granite3.2-vision`) handles photos and URLs;
  `LLAMA_3_2` is text-only (URL path). Pull the model in Ollama first (e.g. `ollama pull granite3.2-vision`).

- **Privacy** — with `OPENAI`, the bag photo or fetched page content is sent to OpenAI's API. With
  `OLLAMA`, everything stays **on your box**. With the feature off (the default), nothing leaves the box.
- **Cost** — OpenAI: roughly a few cents per photo extraction on GPT‑4o (less on `GPT4oMini`), billed to
  your own key; URL cost varies with page size. Ollama: **zero marginal cost** (your hardware).
- **You stay in control** — AI only pre-fills the Add form for you to review and save; it never writes a
  purchase on its own, and never overwrites a field you've already filled in.

## Docker / homelab deployment

The image is built from the production JAR (multi-stage, Amazon Corretto 25 Alpine, runs as a non-root
user). Build the JAR first, then the image — the build passes the project version as `APP_VERSION`:

```bash
# 1. Build the production JAR
./gradlew clean build -Pvaadin.productionMode

# 2. Build the image, tagging it with the project version
docker build \
  --build-arg APP_VERSION=$(grep '^version' gradle.properties | sed 's/version = //') \
  -t beanbook:latest .
```

Then start it with Docker Compose:

```bash
docker compose up -d
```

The provided `compose.yaml` maps port `8001`, sets `SERVER_ADDRESS=0.0.0.0` so the container is
reachable, points `SPRING_DATASOURCE_URL` at `/data/beanbook.db` (WAL mode), and persists the database
on a named `beanbook-data` volume.

## Project structure

```
src/main/kotlin/eu/zeletrik/beanbook
├── BeanbookApplication.kt        # Spring Boot entry point
├── AppShellConfiguration.kt      # PWA shell: manifest, service worker, theme
├── ai/                           # optional AI-assisted entry (photo / URL → form)
│   └── internal/                 # Koog wiring, page fetch, HTML reduce (module-private)
├── analytics/                    # spend & consumption aggregation
├── backup/                       # JSON export / import
├── beans/                        # core domain
│   └── internal/                 # repositories, converters (module-private)
├── preferences/                  # currency & user preferences
├── ui/                           # Vaadin views and components
└── wishlist/                     # beans to try
    └── internal/                 # repository (module-private)

src/main/resources
├── application.yml               # server, datasource, Liquibase config
├── db/changelog/                 # Liquibase migrations
└── static/                       # manifest.webmanifest, sw.js, offline.html, icons
```

## Releases

Releases are automated with [semantic-release](https://semantic-release.gitbook.io/) on the `main`
branch, driven by [Conventional Commits](https://www.conventionalcommits.org/). Commit messages
determine the next version: `fix:` triggers a patch, `feat:` a minor, and a breaking change a major.
On release, the version in `gradle.properties` is bumped, the change is committed, and a GitHub release
with generated notes is published.

## License

Released under the [MIT License](LICENSE.md).
