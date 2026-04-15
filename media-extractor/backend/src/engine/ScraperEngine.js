import { GenericScraper } from '../scrapers/GenericScraper.js';
import { YouTubeScraper } from '../scrapers/YouTubeScraper.js';
import { InstagramScraper } from '../scrapers/InstagramScraper.js';

export class ScraperEngine {
  constructor() {
    this.scrapers = [];
    this.register(new InstagramScraper());
    this.register(new YouTubeScraper());
    this.register(new GenericScraper());
  }

  register(scraper) {
    this.scrapers.push(scraper);
    this.scrapers.sort((a, b) => b.priority - a.priority);
  }

  getScraper(url) {
    return this.scrapers.find((scraper) => scraper.canHandle(url));
  }

  async extract(url) {
    const scraper = this.getScraper(url);

    if (!scraper) {
      return {
        images: [],
        videos: [],
        gifs: [],
        source: 'none',
        success: false,
        error: 'No scraper found for URL'
      };
    }

    try {
      return await scraper.extract(url);
    } catch (error) {
      return {
        images: [],
        videos: [],
        gifs: [],
        source: scraper.name,
        success: false,
        error: error.message || 'Unexpected scraping failure'
      };
    }
  }
}
