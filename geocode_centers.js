#!/usr/bin/env node
/**
 * geocode_centers.js — Voting center → Plus Code converter
 *
 * Processes the 21.5M-line Venezuelan voter registry to extract 15,748
 * unique voting centers, geocodes them via Overpass API for parish/municipality
 * centroids, and assigns Plus Codes.
 *
 * Usage:  node geocode_centers.js
 * Output: data-centers.json
 */
'use strict';
const fs = require('fs');
const https = require('https');
const http = require('http');
const readline = require('readline');

const RE_FILE = 're_evento_2025.txt';
const OUTPUT = 'data-centers.json';
const CACHE = 'geocode-cache.json';

// ─── OLC Plus Code Encoder ────────────────────────────────────────
const _ALP = '23456789CFGHJMPQRVWX';

function plusCode(lat, lng) {
  lat = Math.max(-90.0, Math.min(90.0, lat));
  lng = Math.max(-180.0, Math.min(180.0, lng));
  if (lng === 180) lng = -180.0;
  lat += 90; lng += 180;
  let c = '';
  for (const r of [20, 1, 0.05, 0.0025, 0.000125]) {
    if (c.length >= 10) break;
    const a = Math.min(Math.floor(lat / r), 19);
    const b = Math.min(Math.floor(lng / r), 19);
    lat -= a * r; lng -= b * r;
    c += _ALP[a] + _ALP[b];
  }
  return c.slice(0, 8) + '+' + c.slice(8);
}

// ─── Name Normalization ──────────────────────────────────────────
function norm(name) {
  let s = (name || '').trim();
  for (const p of ['DTTO. ', 'EDO. ', 'MP. ', 'MCPO. ', 'PQ. ', 'CM. ', 'GRL. ']) {
    if (s.toUpperCase().startsWith(p)) { s = s.slice(p.length).trim(); break; }
  }
  for (const p of ['BLVNO ', 'BOLIVARIANO ']) {
    if (s.toUpperCase().startsWith(p)) { s = s.slice(p.length).trim(); break; }
  }
  return s.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/\s+/g, ' ').trim();
}

function normOsm(name) {
  let s = (name || '').trim().replace(/\s*\([^)]*\)\s*$/, '');
  for (const p of ['Municipio ', 'Parroquia ', 'Estado ', 'Distrito ']) {
    if (s.startsWith(p)) { s = s.slice(p.length); break; }
  }
  return s.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/\s+/g, ' ').trim();
}

// ─── Hardcoded State Centroids (fallback) ────────────────────────
const STATES = {
  'capital': [10.4880, -66.8792], 'anzoategui': [8.5916, -63.5887],
  'apure': [7.8894, -69.7500], 'aragua': [10.2310, -67.5947],
  'barinas': [8.6226, -70.2074], 'bolivar': [7.0000, -64.0000],
  'carabobo': [10.1776, -67.9921], 'cojedes': [9.6667, -68.5833],
  'delta amacuro': [9.0500, -62.0500], 'falcon': [11.1812, -69.8596],
  'guarico': [8.7500, -66.2500], 'lara': [10.0678, -69.3293],
  'merida': [8.5897, -71.1561], 'miranda': [10.2506, -66.4167],
  'monagas': [9.7500, -63.2500], 'nueva esparta': [11.0164, -63.9167],
  'portuguesa': [9.0594, -69.7500], 'sucre': [10.4532, -63.2366],
  'tachira': [7.7714, -72.2263], 'trujillo': [9.3658, -70.4270],
  'la guaira': [10.5900, -66.9300], 'vargas': [10.5900, -66.9300],
  'yaracuy': [10.3500, -69.0167], 'zulia': [10.0000, -72.0000],
  'amazonas': [3.4166, -65.8561], 'dependencias federales': [11.8000, -65.2000]
};

// ─── Step 1: Extract unique voting centers ───────────────────────
async function extractCenters() {
  process.stderr.write('⏳ Extracting centers from RE...\n');
  const centers = {};
  let n = 0;

  const rl = readline.createInterface({
    input: fs.createReadStream(RE_FILE, { encoding: 'utf-8' }),
    crlfDelay: Infinity
  });

  let first = true;
  for await (const line of rl) {
    if (first) { first = false; continue; } // skip header
    n++;
    const p = line.split('|');
    if (p.length < 9) continue;
    const code = p[6];
    if (!centers[code]) {
      centers[code] = [p[1], p[3], p[5], p[7], p[8], 0];
    }
    centers[code][5]++;
    if (n % 5000000 === 0) process.stderr.write(`  ...${n.toLocaleString()} lines\n`);
  }

  process.stderr.write(`✅ ${Object.keys(centers).length.toLocaleString()} centers from ${n.toLocaleString()} records\n`);
  return centers;
}

// ─── Step 2: Fetch parish centroids (single Overpass call) ───────
function httpPost(url, data) {
  return new Promise((resolve, reject) => {
    const mod = url.startsWith('https') ? https : http;
    const parsed = new URL(url);
    const req = mod.request({
      hostname: parsed.hostname,
      path: parsed.pathname,
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'User-Agent': 'DireccionVE/1.0' },
      timeout: 300000
    }, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => resolve(body));
    });
    req.on('error', reject);
    req.on('timeout', () => { req.destroy(); reject(new Error('timeout')); });
    req.write(data);
    req.end();
  });
}

async function fetchOverpass() {
  if (fs.existsSync(CACHE)) {
    process.stderr.write('📦 Using cached Overpass data\n');
    return JSON.parse(fs.readFileSync(CACHE, 'utf-8'));
  }

  process.stderr.write('🌐 Querying Overpass API...\n');
  const query = `[out:json][timeout:120];area["ISO3166-1"="VE"]->.ve;rel["boundary"="administrative"]["admin_level"~"^[468]$"](area.ve);out center;`;
  const body = `data=${encodeURIComponent(query)}`;

  const servers = [
    'https://overpass.kumi.systems/api/interpreter',
    'https://overpass-api.de/api/interpreter'
  ];

  let result = { elements: [] };
  for (const server of servers) {
    try {
      const resp = await httpPost(server, body);
      result = JSON.parse(resp);
      if (result.elements && result.elements.length > 0) break;
    } catch (e) {
      process.stderr.write(`  ⚠️  ${new URL(server).hostname}: ${e.message}\n`);
    }
  }

  if (!result.elements || result.elements.length === 0) {
    process.stderr.write('   Using state-level fallback only\n');
    return { elements: [] };
  }

  fs.writeFileSync(CACHE, JSON.stringify(result));
  process.stderr.write(`✅ ${result.elements.length} admin boundaries retrieved\n`);
  return result;
}

// ─── Step 3: Build geocoding index ──────────────────────────────
function buildIndex(overpass) {
  const idx = { 4: {}, 6: {}, 8: {}, fuzzy8: {} };

  for (const el of (overpass.elements || [])) {
    const tags = el.tags || {};
    const level = parseInt(tags.admin_level) || 0;
    if (![4, 6, 8].includes(level)) continue;
    const name = tags.name || tags['name:es'] || '';
    const center = el.center || {};
    const lat = center.lat, lng = center.lon;
    if (!name || lat == null || lng == null) continue;
    const key = normOsm(name);
    if (key && !idx[level][key]) idx[level][key] = [lat, lng];
  }

  // Build prefix index for fuzzy matching (parish level only)
  for (const [key, coords] of Object.entries(idx[8])) {
    const words = key.split(' ');
    for (let i = 1; i <= words.length; i++) {
      const prefix = words.slice(0, i).join(' ');
      if (!idx.fuzzy8[prefix]) idx.fuzzy8[prefix] = coords;
    }
  }

  process.stderr.write(`📍 Index: ${Object.keys(idx[4]).length} states, ${Object.keys(idx[6]).length} municipalities, ${Object.keys(idx[8]).length} parishes (${Object.keys(idx.fuzzy8).length} fuzzy keys)\n`);
  return idx;
}

// ─── Step 4: Geocode + assign Plus Codes ─────────────────────────
function km(lat1, lon1, lat2, lon2) {
  const dlat = (lat2 - lat1) * Math.PI / 180;
  const dlon = (lon2 - lon1) * Math.PI / 180;
  const a = Math.sin(dlat / 2) ** 2 + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dlon / 2) ** 2;
  return 6371 * 2 * Math.asin(Math.min(1, Math.sqrt(a)));
}

function near(lat, lng, ref, maxKm) {
  maxKm = maxKm || 300;
  if (!ref) return true;
  return km(lat, lng, ref[0], ref[1]) < maxKm;
}

function geocodeCenters(centers, idx) {
  process.stderr.write('🔗 Matching centers to coordinates...\n');
  const stats = { parish: 0, parish_fuzzy: 0, muni: 0, state_osm: 0, state_hc: 0, fallback: 0 };
  const results = [];

  for (const [code, [estado, muni, parish, nombre, direccion, voters]] of Object.entries(centers)) {
    let lat = null, lng = null;

    const sk = norm(estado);
    const stateRef = idx[4][sk] || STATES[sk] || null;

    // Try exact parish match (admin_level=8)
    let k = norm(parish);
    if (k && idx[8][k]) {
      const [plat, plng] = idx[8][k];
      if (near(plat, plng, stateRef)) { lat = plat; lng = plng; stats.parish++; }
    }
    // Try fuzzy parish match
    if (lat == null && k && idx.fuzzy8[k]) {
      const [plat, plng] = idx.fuzzy8[k];
      if (near(plat, plng, stateRef)) { lat = plat; lng = plng; stats.parish_fuzzy++; }
    }
    // Try municipality (admin_level=6)
    if (lat == null) {
      k = norm(muni);
      if (k && idx[6][k]) {
        const [plat, plng] = idx[6][k];
        if (near(plat, plng, stateRef)) { lat = plat; lng = plng; stats.muni++; }
      } else if (k && idx.fuzzy8[k]) {
        const [plat, plng] = idx.fuzzy8[k];
        if (near(plat, plng, stateRef)) { lat = plat; lng = plng; stats.muni++; }
      }
    }
    // Try state from Overpass (admin_level=4)
    if (lat == null) {
      k = norm(estado);
      if (k && idx[4][k]) { [lat, lng] = idx[4][k]; stats.state_osm++; }
    }
    // Hardcoded state centroids
    if (lat == null) {
      k = norm(estado);
      if (STATES[k]) { [lat, lng] = STATES[k]; stats.state_hc++; }
      else { lat = 8.0; lng = -66.0; stats.fallback++; }
    }

    results.push([code, nombre, estado, muni, parish, voters, Math.round(lat * 1e6) / 1e6, Math.round(lng * 1e6) / 1e6, plusCode(lat, lng)]);
  }

  results.sort((a, b) => b[5] - a[5]); // sort by voters desc

  const total = Object.values(stats).reduce((a, b) => a + b, 0);
  process.stderr.write(`✅ Geocoded ${total.toLocaleString()}: ${stats.parish + stats.parish_fuzzy} parish (${stats.parish} exact + ${stats.parish_fuzzy} fuzzy), ${stats.muni} muni, ${stats.state_osm + stats.state_hc} state, ${stats.fallback} fallback\n`);
  return results;
}

// ─── Step 5: Write output ───────────────────────────────────────
function writeOutput(results) {
  const json = JSON.stringify(results);
  fs.writeFileSync(OUTPUT, json);
  const totalVoters = results.reduce((sum, r) => sum + r[5], 0);
  process.stderr.write(`💾 ${OUTPUT}: ${results.length.toLocaleString()} centers, ${Math.round(json.length / 1024)}KB\n`);
  process.stderr.write(`📊 ${totalVoters.toLocaleString()} voters total\n`);
  process.stderr.write(`\n   Top 5 centers:\n`);
  for (const r of results.slice(0, 5)) {
    process.stderr.write(`   ${r[1].slice(0, 45).padEnd(45)} ${String(r[5]).padStart(7)} voters  ${r[8]}\n`);
  }
}

// ─── Main ────────────────────────────────────────────────────────
async function main() {
  const centers = await extractCenters();
  const overpass = await fetchOverpass();
  const idx = buildIndex(overpass);
  const results = geocodeCenters(centers, idx);
  writeOutput(results);
  process.stderr.write('\n✨ Done!\n');
}

main();
