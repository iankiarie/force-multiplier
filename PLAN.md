# ForceMultiplier Implementation Plan

## Backend (Python/FastAPI)

### Phase 1: Core Database Schema
1. Organization model
2. Role model
3. UserOrganization association (many-to-many)
4. Add organization_id to User model (via UserOrganization)
5. Update Prediction, Bet, Transaction, Award, Note models to include organization_id
6. Create initial migration (if using Alembic) or update seed script to create tables

### Phase 2: Auth & Organization Middleware
1. Update auth dependencies to get current organization from JWT or header
2. Add organization scoping to all queries (default to user's active organization)
3. Create middleware to set organization context based on user's active organization
4. Update login endpoint to return user's active organization (or allow selection)

### Phase 3: Seed Script Enhancement
1. Update seed script to create:
   - Organization: "Demo Company"
   - Organization: "ketha" (Ketha Africa)
   - Roles: "admin", "user"
   - Users:
     - admin@forcemultiplier.com (Demo Company, admin)
     - user@example.com (Demo Company, user)
     - iankiarie@ketha.africa (ketha, admin)
2. Ensure proper password hashing
3. Assign users to organizations with appropriate roles

### Phase 4: API Endpoints (Organization Scoped)
1. Auth: login, refresh, me
2. Users: CRUD (admin only), list users in organization
3. Organizations: CRUD (super admin only)
4. Predictions: CRUD, list (organization scoped)
5. Bets: place, list, resolve
6. Transactions: list (wallet), history
7. Awards: list, grant (admin), calculate Force Multiplier
8. Notes: CRUD, search, tagging

### Phase 5: Supporting Services
1. Redis setup for caching (leaderboard, dashboard)
2. Celery worker for background tasks (award calculations, sync processing)
3. Event architecture (domain events for audit logs)
4. Force Multiplier engine (monthly calculation job)
5. Badge and achievement services
6. Wallet ledger enforcement (append-only)
7. Notification service (FCM, in-app)
8. OpenTelemetry instrumentation
9. Rate limiting middleware
10. Health check endpoint

### Phase 6: Testing & Quality
1. Unit tests for all services and use cases
2. Integration tests for API endpoints
3. Database migration tests
4. Security testing (OWASP Top 10)
5. Load testing for critical endpoints
6. API contract testing (OpenAPI validation)

### Phase 7: DevOps & Documentation
1. GitHub Actions CI/CD (lint, test, build, container scan)
2. Docker Compose production configuration
3. API documentation (Swagger/OpenAPI)
4. Deployment guide (various platforms)
5. Runbooks for common operations
6. Contributing guidelines

## Android App (Kotlin/Jetpack Compose)

### Phase 1: Foundation & Auth
1. Clean Architecture setup (core, data, domain, presentation, di)
2. Hilt modules for dependency injection
3. Retrofit setup with OkHttp logger and JWT interceptor
4. Room database setup (entities, DAOs, database)
5. DataStore for settings (BASE_URL, organization selection)
6. Auth repository and use cases (login, token storage)
7. Auth ViewModel and screen (login, organization selection)
8. Splash screen and app navigation setup

### Phase 2: Core Features - Predictions
1. Prediction domain model and use cases
2. Prediction repository (local + remote)
3. Prediction ViewModel and screens (list, create, detail)
4. Bet placement flow (stake input, confirmation)
5. Prediction resolution (admin/manager only)
6. Prediction history and filtering
7. UI components: prediction card, bet button, deadline picker

### Phase 3: Wallet & Transactions
1. Wallet domain model (append-only ledger)
2. Wallet use cases (get balance, get transactions, credit, debit)
3. Wallet repository (local + remote)
4. Wallet ViewModel and screen (balance, transaction history)
5. Transaction UI components (debit/credit icons, formatting)
6. Redemption flow (future: rewards catalog)

### Phase 4: Leaderboard & Awards
1. Leaderboard domain model (snapshots, rankings)
2. Leaderboard use cases (get leaderboard, get user rank)
3. Leaderboard repository (local + remote)
4. Leaderboard ViewModel and screen (tabs: weekly/monthly/all-time)
5. Award domain model and use cases
6. Award repository and ViewModel
7. Awards screen (current month, history)
8. Force Multiplier calculation and display
9. UI components: leaderboard item, award badge, rank change animation

### Phase 5: Knowledge Vault (Notes)
1. Note domain model (with tags)
2. Note use cases (CRUD, search, filter by tag)
3. Note repository (local + remote)
4. Note ViewModel and screen (list, create, edit)
5. Tag management (create, filter, chip UI)
6. Search functionality (full-text search)
7. UI components: note card, tag chip, search bar

### Phase 6: Profile & Settings
1. Profile domain model (stats, badges, achievements)
2. Profile use cases (get profile, update profile)
3. Profile repository (local + remote)
4. Profile ViewModel and screen (stats, badges, achievements, edit)
5. Badge and achievement domain models
6. Badge/achievement repository and ViewModel
7. Badges and achievements screens (grid view, progress)
8. Settings screen (notifications, organization switching, logout, about)

### Phase 7: Offline First & Sync
1. SyncQueue Room table (entity, action, payload, retry count, status, timestamp)
2. SyncUseCase (upload pending syncs, mark as synced)
3. WorkManager for periodic sync (charger + network constraints)
4. Conflict resolution strategy (last-write-wins with timestamps)
5. Manual retry UI for failed syncs
6. Optimistic UI updates (show local changes immediately)
7. Sync status indicators (icons, banners)

### Phase 8: Polish & UX
1. Animations (list item additions, rank changes, navigation)
2. Lottie animations for loading, success, error states
3. Material Motion patterns (shared axis, container transform)
4. Accessibility (content descriptions, touch targets, contrast)
5. Dark/light theme support
6. Screen alternative layouts (tablet, foldable)
7. Error handling (network errors, empty states, loading states)
8. Unit tests for ViewModels and use cases
9. UI tests (Compose tests) for critical screens
10. Screenshot tests for regression prevention

### Phase 9: Integration & Finalization
1. End-to-end testing of critical user flows
2. Performance profiling and optimization
3. Battery and network usage optimization
4. App size optimization (resource shrinking, ProGuard)
5. Play Store assets (screenshots, feature graphic, description)
6. Release build and testing on multiple devices
7. Feedback collection and iteration

## Documentation

### 1. Architecture Decision Records (ADRs)
- Tech stack choices
- Architecture patterns (Clean Architecture, CQRS Lite, Event Sourcing lite)
- State management approach (StateFlow, UseCases)
- Offline first strategy
- Sync framework design

### 2. API Documentation
- OpenAPI/Swagger UI annotations
- Postman collection
- Example requests/responses for all endpoints
- Authentication flow
- Error response formats
- Rate limiting headers

### 3. User Guides
- Android app user guide
- Admin portal guide (future)
- Organization management guide
- Prediction creation guide
- Wallet and rewards explanation
- Force Multiplier scoring explanation
- Badge and achievement criteria

### 4. Developer Guides
- Setup guide (local development)
- Contributing guide
- Code style guide (Kotlin, Python)
- Git workflow and commit conventions
- Testing strategies (unit, integration, UI, E2E)
- Debugging and troubleshooting
- Performance tuning

### 5. Operations Guide
- Deployment instructions (Docker, Kubernetes, cloud providers)
- Environment variables reference
- Database backup and restore procedures
- Log monitoring and alerting
- Scaling guidelines
- Security best practices
- Update and migration procedures

## Timeline Estimate (with buffering)
- Backend Phase 1-2: 1 week
- Backend Phase 3-4: 1 week
- Backend Phase 5-6: 1 week
- Backend Phase 7: 1 week (ongoing)
- Android Phase 1-2: 1 week
- Android Phase 3-4: 1 week
- Android Phase 5-6: 1 week
- Android Phase 7-8: 1 week
- Android Phase 9: 1 week
- Documentation: ongoing throughout
- Buffer/integration: 1 week

Total: ~10 weeks (2.5 months) for MVP

## Success Criteria
1. All core features functional and tested
2. API endpoint coverage >90% (unit + integration)
3. Android app crash-free on target devices
4. Offline first sync works reliably
5. Security audit passes basic scans
6. Documentation complete and accurate
7. CI/CD pipeline green and automated
8. User acceptance testing passed with sample users