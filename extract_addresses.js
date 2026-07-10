#!/usr/bin/env node
/**
 * Extract all addresses from Venezuela OSM PBF → data-houses.json
 * Two-pass: first find ways needing resolution + their nodeRefs,
 * then collect only needed node coords. Memory efficient.
 */
'use strict';
const fs = require('fs');
const osmread = require('osm-read');

const FILE = 'venezuela.osm.pbf';
const addresses = [];
const pendingWays = [];
const neededNodes = new Set();

console.log('Pass 1: finding address nodes and ways...');
osmread.parse({
  filePath: FILE,
  node: function (node) {
    const tags = node.tags || {};
    if (tags['addr:street'] && tags['addr:housenumber']) {
      addresses.push({
        street: tags['addr:street'],
        number: tags['addr:housenumber'],
        city: tags['addr:city'] || '',
        zip: tags['addr:postcode'] || '',
        lat: node.lat,
        lng: node.lon
      });
    }
  },
  way: function (way) {
    const tags = way.tags || {};
    if (!tags['addr:street'] || !tags['addr:housenumber']) return;
    const refs = way.nodeRefs || [];
    pendingWays.push({
      nodeRefs: refs,
      street: tags['addr:street'],
      number: tags['addr:housenumber'],
      city: tags['addr:city'] || '',
      zip: tags['addr:postcode'] || ''
    });
    for (const ref of refs) neededNodes.add(ref);
  },
  endDocument: function () {
    console.log(`  Address nodes: ${addresses.length}`);
    console.log(`  Address ways: ${pendingWays.length} (need ${neededNodes.size} node coords)`);

    if (pendingWays.length === 0) {
      finish();
      return;
    }

    // Pass 2: collect only needed node coordinates
    console.log('Pass 2: resolving way node coordinates...');
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
            addresses.push({
              street: w.street, number: w.number,
              city: w.city, zip: w.zip,
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
  console.log('Total addresses extracted:', addresses.length);

  const seen = new Set();
  const unique = [];
  for (const a of addresses) {
    const key = `${a.zip}|${a.city}|${a.street}|${a.number}`;
    if (!seen.has(key)) {
      seen.add(key);
      unique.push(a);
    }
  }

  console.log('After dedup:', unique.length);

  const output = unique.map(a => [
    a.zip, a.city, a.street, a.number,
    Math.round(a.lng * 1e7) / 1e7,
    Math.round(a.lat * 1e7) / 1e7
  ]);

  fs.writeFileSync('data-houses.json', JSON.stringify(output));
  console.log(`Saved data-houses.json (${output.length} entries)`);
}
