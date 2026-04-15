export const toAbsoluteUrl = (candidate, baseUrl) => {
  if (!candidate) return null;

  try {
    return new URL(candidate, baseUrl).href;
  } catch {
    return null;
  }
};

export const unique = (items) => [...new Set(items.filter(Boolean))];

export const isGif = (url = '') => /\.gif($|\?)/i.test(url);

export const scrollToBottom = async (page) => {
  await page.evaluate(async () => {
    await new Promise((resolve) => {
      let totalHeight = 0;
      const distance = 500;
      const timer = setInterval(() => {
        const scrollHeight = document.body.scrollHeight;
        window.scrollBy(0, distance);
        totalHeight += distance;

        if (totalHeight >= scrollHeight - window.innerHeight) {
          clearInterval(timer);
          resolve();
        }
      }, 150);
    });
  });
};
