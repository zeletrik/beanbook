# Contributing to Bean Book

Thanks for taking the time to contribute! Bean Book is a small, single-maintainer
Kotlin / Spring Boot / Vaadin project, so this guide is short and to the point.
If anything here is unclear, open an issue and ask.

## Prerequisites

- **JDK 25.** The Kotlin toolchain targets JDK 25 (`jvmToolchain(25)`), so you
  need a JDK 25 installed.
- **`JAVA_HOME`** should point at your JDK 25 installation.
- **The Gradle wrapper.** Always use `./gradlew` rather than a system Gradle;
  the wrapper pins the supported version for you. No separate Gradle install is
  needed.

## Getting started

```bash
git clone <repo-url>
cd beanbook
./gradlew bootRun
```

Then open <http://localhost:8001> in your browser.

The app uses an embedded SQLite database, so there is nothing extra to set up
for local development.

## Running tests & static analysis

```bash
./gradlew test detekt
```

Both must stay green. Please run this before opening a pull request.

UI behaviour is covered by [Karibu](https://github.com/mvysny/karibu-testing)
in-JVM tests, so you do not need a running browser to exercise the views — the
Vaadin UI is driven directly inside the JVM during the test suite.

## Code style

- Static analysis is enforced by **Detekt**, configured in
  [`detekt.yml`](detekt.yml) (built upon the default config).
- **`MaxLineLength` is 160.**
- **`autoCorrect` is on**, so Detekt will fix what it can automatically when you
  run the task; commit those fixes.
- Write **idiomatic Kotlin**.
- **Document declarations with KDoc.**

## Architecture rules

Bean Book is structured with **Spring Modulith**.

- Respect the **module boundaries** declared in each module's
  `package-info.java` (for example, `allowedDependencies` controls which other
  modules a module may depend on).
- Keep **repositories and converters in `internal` sub-packages** so they stay
  hidden from other modules.
- These rules are enforced by `ModuleBoundaryTest`, which calls
  `ApplicationModules.of(BeanbookApplication::class.java).verify()`. If you cross
  a boundary, this test fails — fix the dependency rather than the test.

## Database changes

The schema is managed with **Liquibase**, with changesets under
`src/main/resources/db/changelog/`.

- **Add a new changeset** for any schema change.
- **Never edit a changeset that has already been applied** — Liquibase tracks
  checksums, and editing an applied changeset will break migrations.

## Commit messages

This project uses **[Conventional Commits](https://www.conventionalcommits.org/)**.
The version is derived automatically by **semantic-release** from your commit
messages, so the prefix matters:

- `fix:` → patch release
- `feat:` → minor release
- `feat!:` / `BREAKING CHANGE:` → major release
- `chore:`, `docs:`, `test:`, `refactor:`, etc. for changes that should not
  trigger a release

Example: `feat: add roast-date filter to the purchase list`

## Pull requests

- Keep them **small** and focused on a single change.
- Make sure **`./gradlew test detekt` passes**.
- **Describe the change** — what it does and why — so review is quick.

Thanks again, and happy brewing!
