export class BaseScraper {
  constructor(name, priority = 0) {
    this.name = name;
    this.priority = priority;
  }

  canHandle(_url) {
    throw new Error('canHandle must be implemented');
  }

  async extract(_url) {
    throw new Error('extract must be implemented');
  }

  baseResult() {
    return {
      images: [],
      videos: [],
      gifs: [],
      source: this.name,
      success: true,
      error: null
    };
  }
}
