import {defineConfig, devices} from '@playwright/test';

export default defineConfig({
 testDir:'./e2e', fullyParallel:false, workers:1, timeout:60000,
 use:{baseURL:process.env.PLAYWRIGHT_BASE_URL||'http://127.0.0.1:3000',
  browserName:'chromium', channel:process.env.PLAYWRIGHT_CHANNEL||'msedge',
  trace:'off', screenshot:'only-on-failure'},
 projects:[{name:'desktop',use:{viewport:{width:1366,height:900}}},
  {name:'mobile',use:{...devices['iPhone 13'],defaultBrowserType:'chromium'}}],
});
