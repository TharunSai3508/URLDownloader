import { Router } from 'express';
import { ScraperEngine } from '../engine/ScraperEngine.js';

const router = Router();
const engine = new ScraperEngine();

router.post('/', async (req, res) => {
  const { url } = req.body;

  if (!url || typeof url !== 'string') {
    return res.status(400).json({
      images: [],
      videos: [],
      gifs: [],
      source: 'unknown',
      success: false,
      error: 'A valid URL is required'
    });
  }

  try {
    const result = await engine.extract(url);
    return res.status(result.success ? 200 : 422).json(result);
  } catch (error) {
    console.error('Extraction route error:', error);
    return res.status(500).json({
      images: [],
      videos: [],
      gifs: [],
      source: 'unknown',
      success: false,
      error: error.message || 'Extraction failed'
    });
  }
});

export default router;
