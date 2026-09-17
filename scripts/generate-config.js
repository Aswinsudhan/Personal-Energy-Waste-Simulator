const fs = require('fs');
const path = require('path');

const required = [
  'VITE_API_URL',
  'FIREBASE_WEB_API_KEY',
  'FIREBASE_WEB_AUTH_DOMAIN',
  'FIREBASE_PROJECT_ID',
  'FIREBASE_WEB_STORAGE_BUCKET',
  'FIREBASE_WEB_MESSAGING_SENDER_ID',
  'FIREBASE_WEB_APP_ID'
];

const missing = required.filter((name) => !process.env[name]);
if (missing.length) {
  throw new Error(`Missing Vercel environment variables: ${missing.join(', ')}`);
}

const config = `window.ENERGY_CONFIG = ${JSON.stringify({
  apiUrl: process.env.VITE_API_URL,
  firebase: {
    apiKey: process.env.FIREBASE_WEB_API_KEY,
    authDomain: process.env.FIREBASE_WEB_AUTH_DOMAIN,
    projectId: process.env.FIREBASE_PROJECT_ID,
    storageBucket: process.env.FIREBASE_WEB_STORAGE_BUCKET,
    messagingSenderId: process.env.FIREBASE_WEB_MESSAGING_SENDER_ID,
    appId: process.env.FIREBASE_WEB_APP_ID
  }
}, null, 2)};\n`;

fs.writeFileSync(path.join(__dirname, '..', 'web', 'config.js'), config);