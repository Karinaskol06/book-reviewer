# Book Reviewer - Prometheus alerts

Stack: **App (Actuator/Micrometer) -> Prometheus -> Alertmanager** (+ Grafana dashboards).

| UI | URL |
|----|-----|
| Prometheus alerts | http://localhost:9090/alerts |
| Alertmanager | http://localhost:9093 |
| Grafana | http://localhost:3000 |

## What fires and why

| Alert | Severity | When | Why it matters |
|-------|----------|------|----------------|
| `BookReviewerHighErrorRate` | critical | HTTP **5xx share > 5%** for **2 minutes** | Users are failing; check logs by `correlationId`, recent changes, DB/ES |
| `BookReviewerSearchFallback` | warning | **Search fallback** rate elevated for **2 minutes** | ES path is broken; app still serves via Postgres but search quality/load suffers |

Rules live in `docker/prometheus/alerts.yml`. Alertmanager routing is in `docker/alertmanager/alertmanager.yml` (demo receiver = UI only).

## Demo: fire `BookReviewerSearchFallback`

```bash
docker compose up -d
# Stop Elasticsearch so search falls back
docker compose stop elasticsearch
# Generate search traffic for ~2+ minutes
for i in $(seq 1 40); do
  curl -s "http://localhost:8081/api/books/search?q=test&page=0&size=5" >/dev/null
  sleep 3
done
```

Then open http://localhost:9090/alerts - the warning should move **Pending -> Firing**.  
Alertmanager: http://localhost:9093/#/alerts.

Restore ES:

```bash
docker compose start elasticsearch
```

## Demo: fire `BookReviewerHighErrorRate`

Harder locally (need sustained 5xx). Options: break the DB briefly under load, or temporarily force an error path.
