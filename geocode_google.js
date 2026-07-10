#!/usr/bin/env node
/**
 * Re-geocode voting centers using Google Geocoding API.
 * Usage: node geocode_google.js <GOOGLE_API_KEY>
 */
'use strict';
const fs = require('fs');
const https = require('https');

const API_KEY = process.argv[2] || '';
if (!API_KEY) {
  console.log('Usage: node geocode_google.js <GOOGLE_API_KEY>');
  process.exit(1);
}

const CACHE_FILE = 'geocode-cache-google.json';
const CENTERS_FILE = 'data-centers.json';

// Open Location Code encoder (inline)
const _A = '23456789CFGHJMPQRVWX';
const _R = [20, 1, 0.05, 0.0025, 0.000125];

function encodePlusCode(lat, lng, length) {
  length = length || 10;
  lat = Math.min(90, Math.max(-90, lat));
  while (lng < -180) lng += 360;
  while (lng >= 180) lng -= 360;
  if (lat === 90) lat -= 0.9 * _R[Math.min(length, 10) >> 1];
  let code = '', aLat = lat + 90, aLng = lng + 180, i = 0;
  while (i < length) {
    if (i < 10) {
      const p = i >> 1, r = _R[p];
      const dLat = Math.min(Math.floor(aLat / r), 19);
      const dLng = Math.min(Math.floor(aLng / r), 19);
      aLat -= dLat * r; aLng -= dLng * r;
      code += _A[dLat] + _A[dLng]; i += 2;
      if (i === 8) code += '+';
    } else {
      let rLat = _R[4], rLng = _R[4];
      for (let g = 10; g < i; g++) { rLat /= 5; rLng /= 4; }
      const gLatR = rLat / 5, gLngR = rLng / 4;
      const row = Math.min(Math.floor(aLat / gLatR), 4);
      const col = Math.min(Math.floor(aLng / gLngR), 3);
      aLat -= row * gLatR; aLng -= col * gLngR;
      code += _A[row * 4 + col]; i += 1;
    }
  }
  if (!code.includes('+')) code += '+';
  return code;
}

function geocode(query) {
  return new Promise((resolve) => {
    const params = new URLSearchParams({
      address: query,
      key: API_KEY,
      components: 'country:VE'
    });
    const url = `https://maps.googleapis.com/maps/api/geocode/json?${params}`;

    https.get(url, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        try {
          const data = JSON.parse(body);
          if (data.status === 'OVER_QUERY_LIMIT') {
            console.log('\n  Rate limited, waiting 10s...');
            setTimeout(() => geocode(query).then(resolve), 10000);
            return;
          }
          if (data.status === 'OK' && data.results && data.results.length > 0) {
            const loc = data.results[0].geometry.location;
            resolve([loc.lat, loc.lng]);
          } else {
            resolve([null, null]);
          }
        } catch (e) {
          resolve([null, null]);
        }
      });
    }).on('error', (e) => {
      console.log(`\n  HTTP error: ${e.message}`);
      setTimeout(() => resolve([null, null]), 5000);
    });
  });
}

function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}

async function main() {
  // Load cache
  let cache = {};
  try {
    cache = JSON.parse(fs.readFileSync(CACHE_FILE, 'utf-8'));
  } catch (e) {}

  // Load centers
  const centers = JSON.parse(fs.readFileSync(CENTERS_FILE, 'utf-8'));
  // Format: [code, name, state, muni, parish, voters, lat, lng, plusCode]

  const total = centers.length;
  let updated = 0, failed = 0, cached = 0;

  for (let i = 0; i < centers.length; i++) {
    const c = centers[i];
    const [name, state, muni, parish] = [c[1], c[2], c[3], c[4]];
    const cacheKey = `${name}|${muni}|${state}`;

    let lat, lng;

    if (cache[cacheKey]) {
      [lat, lng] = cache[cacheKey];
      cached++;
    } else {
      const parts = [name];
      if (parish) parts.push(parish.replace('PQ. ', '').replace('CM. ', ''));
      if (muni) parts.push(muni.replace('MP. ', ''));
      if (state) parts.push(state.replace('EDO. ', ''));
      parts.push('Venezuela');
      const query = parts.join(', ');

      [lat, lng] = await geocode(query);
      cache[cacheKey] = [lat, lng];
      await sleep(50); // ~20 req/s
    }

    if (lat != null && lng != null) {
      c[6] = Math.round(lat * 1e6) / 1e6;
      c[7] = Math.round(lng * 1e6) / 1e6;
      c[8] = encodePlusCode(lat, lng, 10);
      updated++;
    } else {
      failed++;
    }

    if ((i + 1) % 100 === 0 || i === total - 1) {
      fs.writeFileSync(CACHE_FILE, JSON.stringify(cache));
      process.stdout.write(`\r  ${i + 1}/${total}  updated=${updated}  cached=${cached}  failed=${failed}`);
    }
  }

  console.log();
  fs.writeFileSync(CENTERS_FILE, JSON.stringify(centers));
  console.log(`Done. ${updated} geocoded, ${failed} failed, saved to ${CENTERS_FILE}`);
}

main();
