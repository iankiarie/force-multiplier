# ForceMultiplier Setup Guide

## Prerequisites

- Docker and Docker Compose
- Git
- (Optional) Python 3.11+ for running the API directly
- (Optional) Postgres client for direct database access

## Quick Start

### 1. Clone the repository
```bash
git clone <repository-url>
cd ForceMultiplier
```

### 2. Backend Setup
```bash
cd backend
# Copy environment example and adjust as needed
cp .env.example .env
# Edit .env to set your PostgreSQL password and secret key
# Example:
# DATABASE_URL=postgresql://forcemultiplier:your_password@localhost:5432/forcemultiplier
# SECRET_KEY=a_strong_secret_key_here
```

### 3. Start the services
```bash
docker compose up -d
# Wait for PostgreSQL to be ready (about 10-15 seconds)
# You can check with: docker compose logs -f postgres
```

### 4. Initialize the database
```bash
# Run the seed script to create tables and demo data
docker compose exec api python -m app.core.seed
# Expected output:
# Created organization: Demo Company
# Created organization: ketha
# Created role: admin
# Created role: user
# Created admin user for Demo Company
# Assigned admin user to Demo Company
# Created demo user for Demo Company
# Assigned demo user to Demo Company
# Created admin user for ketha organization
# Assigned admin user to ketha organization
```

### 5. Verify the API is running
```bash
curl http://localhost:8001/
# Should return: {"message":"Welcome to the ForceMultiplier API!"}
# Interactive API docs: http://localhost:8001/docs
```

### 6. Test authentication
```bash
# Login as the ketha admin user we just created
curl -X POST "http://localhost:8001/auth/token" \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "username=iankiarie&password=ketha123"
# Response will contain an access token

# Use the token to access protected endpoints
TOKEN="<the_token_from_above>"
curl -H "Authorization: Bearer $TOKEN" http://localhost:8001/auth/users/me
```

### 7. Android App Setup
1. Open the `android/` folder in Android Studio
2. Wait for Gradle sync to complete
3. Create a `Constants.kt` file in `app/src/main/java/com/ian/forcemultiplier/util/` with:
   ```kotlin
   package com.ian.forcemultiplier.util

   object Constants {
       // For Android Emulator
       const val BASE_URL = "http://10.0.2.2:8001/"
       // For physical device on same LAN:
       // const val BASE_URL = "http://<YOUR_COMPUTER_IP>:8001/"
   }
   ```
4. Build and run the app on an emulator or device

## Troubleshooting

### Containers not starting
- Check Docker logs: `docker compose logs api`
- Ensure port 8001 is free on your host (port 8000 is used by Portainer)
- Try rebuilding: `docker compose build api`

### Database connection errors
- Verify PostgreSQL is healthy: `docker compose ps`
- Check the DATABASE_URL in .env matches the service name and credentials
- The docker-compose.yml uses:
  - POSTGRES_USER=forcemultiplier
  - POSTGRES_PASSWORD=forcemultiplier
  - POSTGRES_DB=forcemultiplier
- Internal URL (for API): `postgresql://forcemultiplier:forcemultiplier@postgres:5432/forcemultiplier`
- External URL (for host client): `postgresql://forcemultiplier:forcemultiplier@localhost:5434/forcemultiplier`

### Seed script fails
- Make sure the API container is running: `docker compose ps`
- Try running the seed with more verbose output: `docker compose exec api python -m app.core.seed 2>&1`

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

## Next Steps for Development

Refer to `PLAN.md` for the incremental implementation roadmap.

## License

Personal portfolio project — no license specified.