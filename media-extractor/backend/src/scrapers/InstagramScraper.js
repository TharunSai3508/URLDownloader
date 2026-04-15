import { BaseScraper } from './BaseScraper.js';
import { getPage } from '../utils/browser.js';
import { isGif, unique } from '../utils/media.js';

export class InstagramScraper extends BaseScraper {
  constructor() {
    super('instagram', 100);
  }

  canHandle(url) {
    return /instagram\.com/i.test(url);
  }

  async extract(url) {
    const page = await getPage();
    const result = this.baseResult();

    try {
      await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 45000 });
      await page.waitForTimeout(2000);

      const media = await page.evaluate(() => {
        const ogImage = document
          .querySelector('meta[property="og:image"]')
          ?.getAttribute('content');
        const ogVideo = document
          .querySelector('meta[property="og:video"]')
          ?.getAttribute('content');

        const images = Array.from(document.querySelectorAll('img'))
          .flatMap((img) => [img.currentSrc, img.src])
          .filter(Boolean);

        const videos = Array.from(document.querySelectorAll('video, source'))
          .map((node) => node.src || node.getAttribute('src'))
          .filter(Boolean);

        return {
          images: [ogImage, ...images].filter(Boolean),
          videos: [ogVideo, ...videos].filter(Boolean)
        };
      });

      const allImages = unique(media.images);
      const allVideos = unique(media.videos);

      result.gifs = unique([...allImages, ...allVideos].filter((item) => isGif(item)));
      result.images = allImages.filter((item) => !isGif(item));
      result.videos = allVideos.filter((item) => !isGif(item));

      if (!result.images.length && !result.videos.length && !result.gifs.length) {
        result.success = false;
        result.error = 'No Instagram media found (private or restricted page likely)';
      }

      return result;
    } catch (error) {
      return {
        ...result,
        success: false,
        error: error.message || 'Instagram extraction failed'
      };
    } finally {
      await page.close();
    }
  }
}
