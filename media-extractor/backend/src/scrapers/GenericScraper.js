import { BaseScraper } from './BaseScraper.js';
import { getPage } from '../utils/browser.js';
import { isGif, scrollToBottom, toAbsoluteUrl, unique } from '../utils/media.js';

export class GenericScraper extends BaseScraper {
  constructor() {
    super('generic', 10);
  }

  canHandle(_url) {
    return true;
  }

  async extract(url) {
    const page = await getPage();
    const result = this.baseResult();

    try {
      await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 45000 });
      await page.waitForTimeout(1500);
      await scrollToBottom(page);
      await page.waitForTimeout(750);

      const scraped = await page.evaluate(() => {
        const imgCandidates = [
          ...new Set(
            Array.from(document.querySelectorAll('img'))
              .flatMap((img) => [img.currentSrc, img.src, img.getAttribute('data-src')])
              .filter(Boolean)
          )
        ];

        const videoCandidates = [
          ...new Set(
            Array.from(document.querySelectorAll('video, source'))
              .map((node) => node.src || node.getAttribute('src'))
              .filter(Boolean)
          )
        ];

        return { imgCandidates, videoCandidates };
      });

      const allImages = scraped.imgCandidates.map((entry) => toAbsoluteUrl(entry, url));
      const allVideos = scraped.videoCandidates.map((entry) => toAbsoluteUrl(entry, url));

      result.gifs = unique([...allImages, ...allVideos].filter((item) => isGif(item)));
      result.images = unique(allImages.filter((item) => item && !isGif(item)));
      result.videos = unique(allVideos.filter((item) => item && !isGif(item)));

      if (!result.images.length && !result.videos.length && !result.gifs.length) {
        result.success = false;
        result.error = 'No media found on page';
      }

      return result;
    } catch (error) {
      return {
        ...result,
        success: false,
        error: error.message || 'Generic extraction failed'
      };
    } finally {
      await page.close();
    }
  }
}
