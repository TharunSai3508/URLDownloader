import express from 'express';
import cors from 'cors';
import extractRouter from './routes/extract.js';
import { closeBrowser } from './utils/browser.js';

const app = express();
const PORT = process.env.PORT || 4000;

app.use(cors());
app.use(express.json({ limit: '1mb' }));

app.get('/health', (_req, res) => {
  res.json({ status: 'ok' });
});

app.use('/extract', extractRouter);

app.use((err, _req, res, _next) => {
  console.error('Unhandled error:', err);
  res.status(500).json({
    images: [],
    videos: [],
    gifs: [],
    source: 'unknown',
    success: false,
    error: 'Internal server error'
  });
});

const server = app.listen(PORT, () => {
  console.log(`Media Extractor backend running on port ${PORT}`);
});

const shutdown = async () => {
  console.log('Shutting down...');
  server.close(async () => {
    await closeBrowser();
    process.exit(0);
  });
};

process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
