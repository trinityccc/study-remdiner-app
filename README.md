# Study Reminder Web Application

Full-stack (Spring Boot + Supabase) study reminder app with:
- Email/password authentication via **Supabase Auth**
- Secure REST APIs in **Spring Boot**
- **Row Level Security (RLS)** on `public.study_reminders`
- Frontend: pastel “girly” UI + subtle animations

## Architecture
Frontend → Spring Boot REST API → Supabase (Auth + PostgREST/Postgres)

The frontend never uses raw DB credentials. It uses Supabase Auth and sends the user's **access token** to the backend.

## 1) Supabase setup (SQL)
Open Supabase SQL Editor and run:
- `sql/001_study_reminders.sql`

This creates:
- `public.study_reminders`
- constraints for `type`, `priority`, `status`
- RLS policies enforcing `user_id = auth.uid()`

## 2) Environment variables
This backend reads Supabase config from either:
- shell environment variables, or
- a local `backend/.env` file.

Set these as environment variables for the backend (or create `backend/.env`):
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `PORT` (optional, default `8080`)
- `FRONTEND_ORIGIN` (optional, default `http://localhost:5173,http://localhost:8080`)

Example template:
- `backend/.env.example` (copy to `backend/.env` and fill in values)

> This backend intentionally does NOT use the Supabase Service Role key.

## 3) Run the backend
From the `backend/` directory:

```powershell
mvn spring-boot:run
```

Then open:
- `http://localhost:8080/login.html`

## REST API
- `GET    /reminders`
- `POST   /reminders`
- `PUT    /reminders/{id}`
- `DELETE /reminders/{id}`

All `/reminders/*` endpoints require:
- `Authorization: Bearer <supabase_access_token>`

## Frontend pages
- `login.html` : login with Supabase Auth
- `signup.html`: signup with Supabase Auth
- `dashboard.html`: create/read/update/delete reminders

