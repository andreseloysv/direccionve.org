#!/usr/bin/env node
/**
 * Extract ALL addressable features from Venezuela OSM PBF.
 * Includes: addr:* tagged items, named POIs, named buildings, places, shops, amenities, etc.
 * Two-pass: first find features + collect nodeRefs for ways, then resolve.
 */
'use strict';
const fs = require('fs');
const osmread = require('osm-read');

const FILE = 'venezuela.osm.pbf';
const features = [];
const pendingWays = [];
const neededNodes = new Set();

function categorize(tags) {
  if (tags.amenity) return tags.amenity;
  if (tags.shop) return 'shop:' + tags.shop;
  if (tags.tourism) return 'tourism:' + tags.tourism;
  if (tags.office) return 'office:' + tags.office;
  if (tags.leisure) return 'leisure:' + tags.leisure;
  if (tags.building && tags.building !== 'yes') return 'building:' + tags.building;
  if (tags.place) return 'place:' + tags.place;
  if (tags.highway) return null; // skip
  return '';
}

console.log('Pass 1: finding features...');
osmread.parse({
  filePath: FILE,
  node: function (node) {
    const tags = node.tags || {};
    const name = tags.name || '';
    const street = tags['addr:street'] || '';
    if (!name && !street) return;
    const category = categorize(tags);
    if (category === null) return;

    features.push({
      name, street,
      number: tags['addr:housenumber'] || '',
      city: tags['addr:city'] || '',
      zip: tags['addr:postcode'] || '',
      category, lat: node.lat, lng: node.lon
    });
  },
  way: function (way) {
    const tags = way.tags || {};
    if (tags.highway && !tags.name) return;
    if (tags.highway && !tags['addr:street'] && !tags.amenity && !tags.shop) return;
    const name = tags.name || '';
    const street = tags['addr:street'] || '';
    if (!name && !street) return;
    const category = categorize(tags);
    if (category === null) return;

    const refs = way.nodeRefs || [];
    pendingWays.push({
      nodeRefs: refs, name, street,
      number: tags['addr:housenumber'] || '',
      city: tags['addr:city'] || '',
      zip: tags['addr:postcode'] || '',
      category
    });
    for (const ref of refs) neededNodes.add(ref);
  },
  endDocument: function () {
    console.log(`  Feature nodes: ${features.length}, ways: ${pendingWays.length} (need ${neededNodes.size} coords)`);

    if (pendingWays.length === 0) { finish(); return; }

    console.log('Pass 2: resolving way coordinates...');
    const nodeCoords = {};
    osmread.parse({
      filePath: FILE,
      node: function (node) {
        if (neededNodes.has(node.id)) {
          nodeCoords[node.id] = [node.lat, node.lon];
        }
      },
      endDocument: function () {
        console.log(`  Resolved ${Object.keys(nodeCoords).length} node coords`);

        for (const w of pendingWays) {
          let sumLat = 0, sumLng = 0, count = 0;
          for (const ref of w.nodeRefs) {
            const coord = nodeCoords[ref];
            if (coord) { sumLat += coord[0]; sumLng += coord[1]; count++; }
          }
          if (count > 0) {
            features.push({
              name: w.name, street: w.street, number: w.number,
              city: w.city, zip: w.zip, category: w.category,
              lat: sumLat / count, lng: sumLng / count
            });
          }
        }
        finish();
      }
    });
  }
});

function finish() {
  console.log('Total features extracted:', features.length);

  const seen = new Set();
  const unique = [];
  for (const f of features) {
    const key = `${f.name}|${f.street}|${f.number}|${Math.round(f.lat * 1e4)}|${Math.round(f.lng * 1e4)}`;
    if (!seen.has(key)) {
      seen.add(key);
      unique.push(f);
    }
  }

  console.log('After dedup:', unique.length);

  const cats = {};
  for (const f of unique) {
    const c = f.category || 'address';
    cats[c] = (cats[c] || 0) + 1;
  }
  Object.entries(cats)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 20)
    .forEach(([c, n]) => console.log(`  ${c}: ${n}`));

  const output = unique.map(f => [
    f.name || 0, f.street || 0, f.number || 0,
    f.city || 0, f.zip || 0, f.category || 0,
    Math.round(f.lng * 1e5) / 1e5,
    Math.round(f.lat * 1e5) / 1e5
  ]);

  const json = JSON.stringify(output);
  fs.writeFileSync('data-all-addresses.json', json);
  console.log(`\nSaved data-all-addresses.json (${output.length} entries, ${Math.round(json.length / 1024)} KB)`);
}
