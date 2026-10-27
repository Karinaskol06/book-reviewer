# Book Reviewer

[![CI](https://github.com/Karinaskol06/book-reviewer/actions/workflows/ci.yml/badge.svg)](https://github.com/Karinaskol06/book-reviewer/actions/workflows/ci.yml)

A full-stack book social app: discover new books, write structured reviews, follow readers, browse an activity feed, join reading clubs, and get recommendations.

Built as a **portfolio project** focused on **Junior Backend** skills - Spring Boot, REST APIs, PostgreSQL/SQL, Flyway, JWT security, testing (including Testcontainers), and a clear **hexagonal (ports & adapters)** layout. A React UI is included so the API can be exercised end-to-end. 
The same codebase runs on **AWS** - EC2 + Docker Compose, **RDS** PostgreSQL, **S3** uploads via an IAM instance role, and **GitHub Actions** CD with an actuator healthcheck and automatic git rollback. 

---

## Highlights

| Product capability | Engineering behind it |
| ------------------ | --------------------- |
| Register / login | Spring Security + **JWT** (`TokenPort` → `JwtService`) |
| Books, reviews, shelves | REST API, JPA, **PostgreSQL**, validation |
| Schema & demo data | **Flyway** migrations (`validate` Hibernate mode) |
| Search & filters | **Elasticsearch** via `BookSearchPort` |
| Follow / activity feed | Domain events → `EventPublisherPort` → listeners |
| Covers & avatars | `ObjectStoragePort` — local disk or **Amazon S3** (instance role) |
| Cloud deploy | **EC2** + Docker Compose, **RDS** Postgres, **S3**, **GitHub Actions** CD |
| Automated checks | Unit tests, security slice tests, **Testcontainers** ITs, **CI + deploy** |

---

## Tech Stack

| Layer | Technologies |
| ----- | ------------ |
| **Language** | Java 21 |
| **Backend** | Spring Boot 3.2, Spring Web, Spring Security, Spring Data JPA, Validation |
| **Architecture** | Hexagonal — inbound use cases, outbound ports, infrastructure adapters |
| **Database** | PostgreSQL 16, Flyway |
| **Search** | Elasticsearch 8 |
| **Auth** | JWT (JJWT) |
| **Storage** | Local filesystem (dev) or **Amazon S3** (prod/demo on AWS) |
| **Frontend** | React 19, Vite, React Router (nginx in Docker) |
| **Build / run** | Maven Wrapper, Docker Compose |
| **CI/CD** | GitHub Actions (test → SSH deploy to EC2, healthcheck + rollback) |
| **AWS** | EC2, Elastic IP, Security Groups, IAM instance role, S3, RDS PostgreSQL |
| **Observability** | Actuator, Micrometer, Prometheus, Grafana, Alertmanager (local Compose) |
| **Testing** | JUnit 5, Mockito, Spring Security Test, Testcontainers |

---

## Table of Contents

- [Key Features](#key-features)
- [Quick Start (Docker Compose)](#quick-start-docker-compose)
- [Manual Development Setup](#manual-development-setup)
- [Architecture](#architecture)
- [Environment Variables](#environment-variables)
- [Available Scripts](#available-scripts)
- [Testing](#testing)
- [Observability](#observability)
- [Continuous Integration & Delivery](#continuous-integration--delivery)
- [Database & Migrations](#database--migrations)
- [Deployment](#deployment)
  - [AWS demo architecture](#aws-demo-architecture)
  - [AWS setup](#aws-setup)
- [Troubleshooting](#troubleshooting)
- [Project Structure](#project-structure)

---

## Key Features

- **Books** - create/update catalog entries, cover upload, duplicate checks, trending list
- **Reviews** - ratings, verdicts, pacing/mood tags, spoilers, helpful votes
- **Library** - want to read / reading / read / abandoned statuses
- **Social** - follow users, activity feed fan-out, user search
- **Profiles** - avatar, about me, social links, taste / genre spectrum
- **Reading clubs** - membership, roles, discussions, insightful votes
- **Search & recommendations** — Elasticsearch-backed search/filter; taste-aware suggestions
- **Export** - reading list PDF export

---

## Quick Start (Docker Compose)

Starts **PostgreSQL**, **Elasticsearch**, the **Spring Boot API**, the **React UI**, plus **Prometheus / Alertmanager / Grafana** for local observability.

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and Docker Compose v2
- ~4 GB RAM free (Elasticsearch is the heavy piece)

### 1. Configure environment

```bash
cp .env.example .env
```

Change `JWT_SECRET` before any shared or public deployment. For local demos the example value is enough to boot.

### 2. Build and start everything

```bash
docker compose up --build
```

First build compiles the backend inside Docker (Maven) and the frontend (npm) - expect several minutes.

### 3. Open the app

| Service | URL |
| ------- | --- |
| **Web UI** | [http://localhost:3080](http://localhost:3080) |
| **API** | [http://localhost:8081](http://localhost:8081) |
| **Grafana** | [http://localhost:3000](http://localhost:3000) (`admin` / `admin`) |
| **Prometheus** | [http://localhost:9090](http://localhost:9090) |
| **Alertmanager** | [http://localhost:9093](http://localhost:9093) |
| **Postgres** (host) | `localhost:5433` |
| **Elasticsearch** | [http://localhost:9200](http://localhost:9200) |

Nginx on the frontend proxies `/api` and uploads to the `app` container, so the browser only needs port **3080**.

Flyway runs on startup: schema baseline + seed books/users/reviews when tables are empty.

---

## Manual Development Setup

Use this when you want hot-reload on the backend/frontend while only infra runs in Docker.

### Prerequisites

- JDK **21**
- Node.js **20+** and npm
- Docker (for Postgres + Elasticsearch) *or* local installs of both

### 1. Start infrastructure only

Temporarily comment out / stop `app` and `frontend`, or run:

```bash
docker compose up postgres elasticsearch -d
```

### 2. Backend

```bash
cp .env.example .env
# For host Spring Boot against Compose Postgres:
#   DB_HOST=localhost
#   DB_PORT=5433
#   ELASTICSEARCH_HOST=localhost

./mvnw spring-boot:run
```

API: [http://localhost:8081](http://localhost:8081)

### 3. Frontend

```bash
cd bookreviewer-frontend
cp .env.example .env   # VITE_API_BASE_URL=/api
npm ci
npm run dev
```

Vite proxies `/api` to the backend. Open the URL Vite prints (usually `http://localhost:5173`).

---

## Architecture

The backend follows **ports & adapters (hexagonal)** so HTTP and frameworks sit at the edges, and application logic depends on interfaces.

```text
┌─────────────┐   inbound UseCase    ┌──────────────────┐   outbound Port   ┌────────────────────┐
│ Controllers │ ───────────────────► │ Application      │ ────────────────► │ Adapters           │
│ (web)       │                      │ services         │                   │ JWT, JPA, ES, S3,  │
└─────────────┘                      └────────┬─────────┘                   │ EventPublisher …   │
                                              │                             └────────────────────┘
                                              ▼
                                     ┌──────────────────┐
                                     │ Domain           │
                                     │ models, events   │
                                     └──────────────────┘
```

### Concrete examples in this repo

- Controllers inject **use-case interfaces** (e.g. `BookUseCase`, `AuthUseCase`), not concrete service types where wired.
- **`TokenPort`** → `JwtService` issues JWTs without leaking JJWT types into application code.
- **`ObjectStoragePort`** → local or S3 adapters; cover URLs normalized via `toStorageReference`.
- **`BookSearchPort`** → Elasticsearch adapter for search / recommendations.
- **`EventPublisherPort`** → `SpringEventPublisherAdapter`; plain domain event **records** (no Spring `ApplicationEvent` in domain).
- Listeners stay in **infrastructure** (`ActivityEventListener`, `ElasticsearchSyncListener`).
- Persistence goes through **repository ports** and JPA adapters; Hibernate `ddl-auto: validate` with **Flyway** owning schema.

This is a modular monolith with clear package boundaries: `domain`, `application`, `infrastructure`.

---

## Environment Variables

See [`.env.example`](.env.example) for the full list. Common ones:

### Required to boot

| Variable | Description | Example |
| -------- | ----------- | ------- |
| `JWT_SECRET` | HMAC secret for JWTs | long random string |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL | see `.env.example` |

### Often used

| Variable | Description | Default |
| -------- | ----------- | ------- |
| `SERVER_PORT` | Spring HTTP port | `8081` (host) / `8080` in container |
| `JWT_EXPIRATION` | Token lifetime (ms) | `86400000` (24h) |
| `ELASTICSEARCH_HOST` / `ELASTICSEARCH_PORT` | Search cluster | `localhost` / `9200` |
| `STORAGE_TYPE` | `local` or `s3` | `local` |
| `UPLOAD_DIR` | Local upload root | `./uploads-book-reviewer` |
| `STORAGE_PUBLIC_BASE_URL` | Public base URL for S3 objects | empty |
| `AWS_S3_BUCKET` / `AWS_S3_REGION` | S3 target when `STORAGE_TYPE=s3` | empty / `eu-central-1` |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | Optional; **omit on EC2** so the SDK uses the instance role | empty |
| `ELASTICSEARCH_ENABLED` | Set `false` with profile `no-es` on small AWS hosts | `true` locally |

AWS Compose env template: [`.env.aws.example`](.env.aws.example).

---

## Available Scripts

| Command | Description |
| ------- | ----------- |
| `docker compose up --build` | Full stack (Postgres, ES, API, UI) |
| `docker compose up postgres elasticsearch -d` | Infra only for local coding |
| `docker compose -f docker-compose.aws-lite.yml up -d --build` | Small EC2: Postgres in Compose, no Elasticsearch |
| `docker compose -f docker-compose.aws-rds.yml up -d --build` | EC2 app + frontend, DB on Amazon RDS |
| `./mvnw spring-boot:run` | Run API on the host |
| `./mvnw test` | Unit + slice + Testcontainers tests |
| `./mvnw -DskipTests package` | Build jar to `target/` |
| `cd bookreviewer-frontend && npm run dev` | Vite dev server |
| `cd bookreviewer-frontend && npm run build` | Production frontend build |

Windows: use `mvnw.cmd` instead of `./mvnw`.

---

## Testing

```bash
./mvnw test
```

Flyway is **disabled** in the default test `application.yaml` so unit/slice tests do not need a migrated Postgres.

| Kind | What it covers |
| ---- | -------------- |
| **Unit** | Application services with Mockito (ports mocked) |
| **Security slices** | `@WebMvcTest` + Spring Security Test for authz on controllers |
| **Testcontainers ITs** | Real **PostgreSQL** (trending, feed, filters, uniqueness) and **Elasticsearch** search |

Docker must be available for Testcontainers-based tests.

---

## Observability

Local Compose includes **Actuator/Micrometer → Prometheus → Grafana** and **Alertmanager**.

| Signal | Where |
| ------ | ----- |
| RED + JVM + Hikari + domain metrics | Grafana dashboard **Book Reviewer — Overview** |
| Alert rules | `docker/prometheus/alerts.yml` |
| Alert routing (demo UI) | `docker/alertmanager/alertmanager.yml` |

**Alerts (what fires and why):**

| Alert | Severity | Condition |
| ----- | -------- | --------- |
| `BookReviewerHighErrorRate` | critical | HTTP 5xx share **> 5%** for **2m** |
| `BookReviewerSearchFallback` | warning | ES -> Postgres search fallback rate elevated for **2m** |

How to demo fallback → firing: see [`docker/observability/ALERTS.md`](docker/observability/ALERTS.md).

---

## Continuous Integration & Delivery

GitHub Actions runs on every **push** and **pull request** to `main` (see [`.github/workflows/ci.yml`](.github/workflows/ci.yml)).

| Job | When | What it does |
| --- | ---- | ------------ |
| **Backend** | push + PR | JDK 21 + `./mvnw -B test` (Testcontainers Postgres/Elasticsearch on the runner) |
| **Frontend** | push + PR | Node 20 + `npm ci` + production `npm run build` |
| **Deploy to EC2** | push to `main` only | SSH → `git fetch` / `reset --hard` → `docker compose -f docker-compose.aws-rds.yml up -d --build` → poll `/actuator/health` → on failure **rollback** to previous commit |

Backend and frontend run in parallel; deploy waits for both and only runs on `main`.

**GitHub secrets** (deploy job): `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`.

The status badge at the top of this README reflects the latest workflow run.

---

## Database & Migrations

Migrations live in `src/main/resources/db/migration/`:

| Version | Purpose |
| ------- | ------- |
| `V1__baseline.sql` | Schema baseline |
| `V2__seed_books.sql` | Sample books (skips if data exists) |
| `V3__seed_users_and_reviews.sql` | Sample users & reviews |
| `V4__normalize_seed_cover_urls.sql` | Cover URL cleanup |

Hibernate runs with `ddl-auto: validate` - schema changes go through Flyway, not auto-DDL.

On AWS RDS the JDBC URL uses `sslmode=require` (see `docker-compose.aws-rds.yml`).

---

## Deployment

### Local / full stack

Docker Compose for the complete local demo (Postgres + Elasticsearch + API + UI + observability): see [Quick Start](#quick-start-docker-compose).




### AWS demo architecture

Portfolio / learning deploy on a single EC2 instance.

```text
GitHub Actions (main)
        │ SSH
        ▼
   EC2 (Elastic IP)
   ├── Docker: Spring Boot app  :8081
   ├── Docker: React (nginx)    :80
   ├── IAM instance role ──────► S3 bucket (covers / avatars)
   └── Security Group ─────────► RDS PostgreSQL (private SG, SSL)
```

| Piece | Role in this project |
| ----- | -------------------- |
| **EC2** | Hosts app + frontend containers; Elastic IP for a stable public address |
| **Security Groups** | SSH limited; app HTTP open for demo; RDS accepts Postgres **only from the EC2 SG** |
| **IAM instance role** | `s3:PutObject` / `GetObject` / `DeleteObject` on the avatar bucket — **no long-lived access keys** in `.env` |
| **S3** | `ObjectStoragePort` → `S3ObjectStorageAdapter`; SDK uses `DefaultCredentialsProvider` |
| **RDS PostgreSQL** | Managed DB; Flyway migrates on app boot; Compose does not run a Postgres container |
| **GitHub Actions** | Test gate → SSH deploy → healthcheck → automatic rollback |

Elasticsearch is **off** on this path (`SPRING_PROFILES_ACTIVE=docker,no-es`, `ELASTICSEARCH_ENABLED=false`) so a small instance stays within disk/RAM; search falls back to PostgreSQL via the same `BookSearchPort` abstraction.

### Compose files for AWS

| File | Use when |
| ---- | -------- |
| [`docker-compose.aws-rds.yml`](docker-compose.aws-rds.yml) | App + frontend on EC2; **RDS** for data, S3 for uploads |
| [`docker-compose.aws.yml`](docker-compose.aws.yml) | Heavier AWS-shaped stack (includes more services, needs more instance capacity) |

Copy [`.env.aws.example`](.env.aws.example) to `.env` on the instance and set `DB_*`, `JWT_SECRET`, and S3 settings. For RDS, set `DB_HOST` to the RDS endpoint.

```bash
cp .env.aws.example .env
docker compose -f docker-compose.aws-rds.yml up -d --build
curl -sf http://localhost:8081/actuator/health
```

Public UI: `http://<Elastic-IP>/` (nginx on port 80).

### AWS setup

Done as a learning deploy:

1. **IAM** - created a dedicated console user (with MFA), and an **EC2 instance role** with a least-privilege S3 policy for one avatar/cover bucket.
2. **EC2** - launched Amazon Linux, installed Docker, associated an **Elastic IP**, and opened Security Group rules for HTTP / app port for the demo. SSH was widened so **GitHub Actions** can deploy (key auth still required).
3. **S3** - created the bucket, pointed `STORAGE_TYPE=s3` / `STORAGE_PUBLIC_BASE_URL` at it, attached the instance role.
4. **RDS** - moved Postgres off the instance onto **RDS**, created the `bookreviewer` database, allowed port `5432` only from the EC2 security group, and switched Compose to `docker-compose.aws-rds.yml` with `sslmode=require`.
5. **GitHub Actions** — stored `EC2_HOST` / `EC2_USER` / `EC2_SSH_KEY` as repo secrets and wired deploy on push to `main`: SSH → pull → rebuild → `/actuator/health` poll → **rollback to the previous commit** if health fails.



---

## Troubleshooting

### `JWT_SECRET` / application fails to start

Ensure `.env` exists and `JWT_SECRET` is set. Compose passes it into the `app` service.

### Postgres connection refused from the host

Compose maps Postgres to host port **5433** (`5433:5432`). Use `DB_PORT=5433` and `DB_HOST=localhost` for host-run Spring Boot.

### Elasticsearch unhealthy / OOM

Give Docker more memory. The Compose file sets `ES_JAVA_OPTS=-Xms512m -Xmx512m`.

### Frontend loads but API calls fail

- Full Compose: use [http://localhost:3080](http://localhost:3080) so nginx can proxy `/api` to `app`.
- Vite: confirm the proxy target matches your backend port (`8081`).

### Tests fail with Testcontainers errors

Start Docker Desktop (or the Docker daemon). Re-run `./mvnw test`.

### Port already in use

Change host mappings in `docker-compose.yml` (`3080`, `8081`, `5433`, `9200`) or stop the conflicting process.

---

## Project Structure

```text
book-reviewer/
├── src/main/java/com/project/bookreviewer/
│   ├── domain/           # Models, domain events, inbound/outbound ports
│   ├── application/      # Use-case services, DTOs, mappers, config
│   ├── infrastructure/   # Web, security, JPA, ES, storage, listeners
│   └── shared/           # Small shared utilities
├── src/main/resources/
│   ├── application.yaml
│   ├── db/migration/     # Flyway
│   └── static/           # Default images / static assets
├── src/test/java/        # Unit, slice, and Testcontainers tests
├── bookreviewer-frontend/  # React + Vite UI
├── .github/workflows/    # GitHub Actions CI
├── docker-compose.yml    # Postgres, Elasticsearch, API, UI
├── Dockerfile            # Multi-stage Spring Boot image
└── .env.example
```

---

## License

Personal / portfolio project.
