# ForceMultiplier

A workplace gamification app — predictions, leaderboards, a points wallet, and a personal knowledge vault — built primarily as an **Android portfolio project** (Kotlin, Jetpack Compose, MVVM, Clean Architecture, Room, Hilt), backed by a small **FastAPI + PostgreSQL** API running in Docker.

> **Scope note:** ~80% of the effort here is the Android app. The backend is intentionally minimal — just enough to make the app feel "real" (network calls, auth, a remote source of truth) without becoming its own project. If you only have time for one half, build the Android app against the seeded local Room database first, and add the backend afterward.

---

## Tech Stack

**Android**
- Kotlin
- Jetpack Compose + Material 3
- MVVM + Clean Architecture (UI → ViewModel → UseCase → Repository → Room)
- Room (offline cache / local source of truth for UI)
- Hilt (dependency injection)
- Retrofit + OkHttp (networking)
- Coroutines + Flow
- Navigation Compose
- Coil (images)

**Backend**
- Python + FastAPI
- PostgreSQL
- SQLAlchemy 2
- Docker + Docker Compose
- Pytest

---

## Project Structure

```
forcemultiplier/
├── android/
│   └── app/src/main/java/com/ian/forcemultiplier/
│       ├── core/
│       │   ├── theme/              # colors, typography, shapes
│       │   ├── designsystem/       # FMCard, FMButton, FMChip, FMStatCard, etc.
│       │   ├── components/         # shared composables
│       │   └── util/
│       ├── data/
│       │   ├── local/
│       │   │   ├── dao/
│       │   │   ├── entity/
│       │   │   └── database/
│       │   ├── remote/
│       │   │   ├── api/            # Retrofit interfaces
│       │   │   └── dto/            # API response models
│       │   └── repository/         # RepositoryImpl — coordinates local + remote
│       ├── domain/
│       │   ├── model/              # User, Prediction, Bet, Award, etc.
│       │   ├── repository/         # repository interfaces
│       │   └── usecase/
│       ├── presentation/
│       │   ├── dashboard/
│       │   ├── prediction/
│       │   ├── leaderboard/
│       │   ├── wallet/
│       │   ├── vault/
│       │   ├── profile/
│       │   └── navigation/
│       └── di/                     # Hilt modules
└── backend/
    ├── docker-compose.yml
    ├── Dockerfile
    ├── requirements.txt
    ├── .env.example
    └── app/
        ├── main.py
        ├── config.py
        ├── database.py
        ├── deps.py
        ├── models/                  # SQLAlchemy ORM
        ├── schemas/                 # Pydantic
        ├── routers/                 # API endpoints
        ├── services/                # business logic
        ├── core/
        │   ├── security.py
        │   └── seed.py              # demo data generator
        └── tests/
```

---

## Core Features

| Feature | Description |
|---|---|
| **Dashboard** | Rank, points, win rate, recent predictions/notes, Force Multiplier card |
| **Predictions** | Create predictions, place stakes, resolve outcomes, view history |
| **Force Multiplier** | Monthly award — weighted score from points, accuracy, participation |
| **Leaderboard** | Monthly + all-time rankings with rank-change animations |
| **Wallet** | Append-only transaction ledger; balance = sum of transactions |
| **Mind Vault** | Personal notes with tags and search |
| **Profile** | Stats, badges, award history |

---

## Domain Logic Reference

### Bet Resolution
1. **At bet placement**: deduct `stake` immediately → create a `BET_PLACED` (debit) transaction. Wallet balance always reflects committed funds.
2. **At resolution**:
    - Winning bets → credit `stake * 2` as a `BET_WIN` transaction.
    - Losing bets → no further transaction (the debit already happened).
    - Mark all bets `settled = true`.

### Force Multiplier Score (monthly)
```
final_score = (points_score * 0.5) + (accuracy * 0.3) + (participation * 0.2)
```
- `points_score`: sum of transactions for the month, normalized 0–100 across users
- `accuracy`: wins / (wins + losses) * 100
- `participation`: user's bet count / max bet count by any user * 100

Highest score wins → `Award` record created, winner credited +5000 bonus points.

Triggered via `POST /awards/calculate?month=YYYY-MM` (admin-only, manual trigger — no cron job needed for v1).

---

## Getting Started

### 1. Backend
```bash
cd backend
cp .env.example .env
docker compose up --build
```
- API available at `http://localhost:8000`
- Interactive docs at `http://localhost:8000/docs`
- Seed demo data: `docker compose exec api python -m app.core.seed`

### 2. Android
- Open `android/` in Android Studio
- Point `BASE_URL` in the Retrofit module to your backend:
    - Emulator: `http://10.0.2.2:8000/`
    - Physical device: your machine's LAN IP
- Build and run

### 3. Offline mode (no backend)
The app can run entirely against the local Room database. Run the `DatabaseSeeder` on first launch to populate demo data — useful for early UI development before the backend is wired up.

---

## Demo Credentials

After running the seed script, you have:

### Organizations
- Demo Company
- ketha

### Roles
- admin
- user

### Users
| Email | Username | Password | Organization | Role |
|-------|----------|----------|--------------|------|
| admin@forcemultiplier.com | admin | admin | Demo Company | admin |
| user@example.com | demouser | password | Demo Company | user |
| iankiarie@ketha.africa | iankiarie | ketha123 | ketha | admin |

---

## Build Order

**Phase 1 — Backend core** (1–2 weeks)
- Docker Compose (api + postgres)
- Auth, Users, Predictions, Bets, Wallet endpoints
- Seed script
- Verify via Swagger UI before touching Android

**Phase 2 — Force Multiplier, Leaderboard, Notes** (1 week)
- Calculation service + leaderboard queries + notes CRUD
- Service-layer tests for resolution and scoring logic

**Phase 3 — Android app** (2–3 weeks)
- Design system + theme first
- Auth → Dashboard → Predictions → Leaderboard (in order, each depends on the last)
- Mind Vault last (independent, easiest to slot in)

**Phase 4 — Polish** (1 week)
- Animations on Predictions + Leaderboard only
- Screenshots, demo video, architecture diagram in README

---

## Deliberately Out of Scope (v1)

These are real, valid features — just not worth the time cost for a portfolio piece. Listed here so you can discuss them in interviews as "designed but deferred":

- Full `sync_queue` table + background sync worker (optimistic local writes + simple retry are sufficient)
- Force-Multiplier-on-Force-Multiplier meta predictions
- Separate Wallet tab (folded into Profile)
- AI-generated insights
- Push notifications, multi-tenancy, Alembic migration history

---

## License

Personal portfolio project — no license specified.