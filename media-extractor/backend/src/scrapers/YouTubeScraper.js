import { BaseScraper } from './BaseScraper.js';
import { getPage } from '../utils/browser.js';
import { unique } from '../utils/media.js';

export class YouTubeScraper extends BaseScraper {
  constructor() {
    super('youtube', 90);
  }

  canHandle(url) {
    return /(?:youtube\.com|youtu\.be)/i.test(url);
  }

  async extract(url) {
    const page = await getPage();
    const result = this.baseResult();

    try {
      await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 45000 });
      await page.waitForTimeout(1500);

      const media = await page.evaluate(() => {
        const ogImage = document
          .querySelector('meta[property="og:image"]')
          ?.getAttribute('content');
        const twitterImage = document
          .querySelector('meta[name="twitter:image"]')
          ?.getAttribute('content');
        const poster = document.querySelector('video')?.getAttribute('poster');

        const candidateVideos = [
          document.querySelector('link[rel="canonical"]')?.getAttribute('href'),
          window.location.href
        ].filter(Boolean);

        return {
          images: [ogImage, twitterImage, poster].filter(Boolean),
          videos: candidateVideos
        };
      });

      result.images = unique(media.images);
      result.videos = unique(media.videos);
      result.gifs = [];

      if (!result.images.length && !result.videos.length) {
        result.success = false;
        result.error = 'No YouTube media metadata found';
      }

      return result;
    } catch (error) {
      return {
        ...result,
        success: false,
        error: error.message || 'YouTube extraction failed'
      };
    } finally {
      await page.close();
    }
  }
}
