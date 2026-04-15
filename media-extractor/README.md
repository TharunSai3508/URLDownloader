# Media Extractor

Fullstack app to extract images, videos, and GIFs from a URL.

## Stack

- Backend: Node.js + Express + Playwright
- Frontend: React + Vite

## Project Structure

```
media-extractor/
  backend/
    src/
      app.js
      routes/
      engine/
      scrapers/
      utils/
  frontend/
    src/
      App.jsx
      components/
      api/
```

## Run locally

### 1) Backend

```bash
cd backend
npm install
npx playwright install chromium
npm run dev
```

Backend runs on `http://localhost:4000`.

### 2) Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:5173` and calls backend at `http://localhost:4000` by default.

To use a custom backend URL:

```bash
VITE_API_URL=http://localhost:4000 npm run dev
```
