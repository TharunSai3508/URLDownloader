import { chromium } from 'playwright';

let browser;
let context;

export const getContext = async () => {
  if (!browser) {
    browser = await chromium.launch({ headless: true });
  }

  if (!context) {
    context = await browser.newContext({
      userAgent:
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36'
    });
  }

  return context;
};

export const getPage = async () => {
  const sharedContext = await getContext();
  return sharedContext.newPage();
};

export const closeBrowser = async () => {
  if (context) {
    await context.close();
    context = null;
  }

  if (browser) {
    await browser.close();
    browser = null;
  }
};
