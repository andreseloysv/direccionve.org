#!/usr/bin/env node
/**
 * Extract all named streets from Venezuela OSM PBF → data-streets.json
 * Two-pass: collect needed nodeRefs, then resolve coordinates.
 */
'use strict';
const fs = require('fs');
const osmread = require('osm-read');

const FILE = 'venezuela.osm.pbf';
const pendingWays = [];
const neededNodes = new Set();

console.log('Pass 1: finding street ways...');
osmread.parse({
  filePath: FILE,
  way: function (way) {
    const tags = way.tags || {};
    if (!tags.highway || !tags.name) return;
    const refs = way.nodeRefs || [];
    pendingWays.push({ name: tags.name, city: tags['addr:city'] || '', nodeRefs: refs });
    for (const ref of refs) neededNodes.add(ref);
  },
  endDocument: function () {
    console.log(`  Street ways: ${pendingWays.length} (need ${neededNodes.size} node coords)`);

    console.log('Pass 2: resolving node coordinates...');
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

        const streets = [];
        for (const w of pendingWays) {
          let sumLat = 0, sumLng = 0, count = 0;
          for (const ref of w.nodeRefs) {
            const coord = nodeCoords[ref];
            if (coord) { sumLat += coord[0]; sumLng += coord[1]; count++; }
          }
          if (count > 0) {
            streets.push({ name: w.name, city: w.city, lat: sumLat / count, lng: sumLng / count });
          }
        }

        console.log('Total named streets:', streets.length);

        // Deduplicate by (name, city) averaging coordinates
        const seen = {};
        for (const s of streets) {
          const key = `${s.name}|${s.city}`;
          if (!seen[key]) {
            seen[key] = { ...s, count: 1 };
          } else {
            const prev = seen[key];
            prev.lat = (prev.lat * prev.count + s.lat) / (prev.count + 1);
            prev.lng = (prev.lng * prev.count + s.lng) / (prev.count + 1);
            prev.count++;
          }
        }

        const unique = Object.values(seen);
        console.log('Unique named streets:', unique.length);

        const output = unique.map(s => [
          s.city, s.name,
          Math.round(s.lng * 1e7) / 1e7,
          Math.round(s.lat * 1e7) / 1e7
        ]);

        fs.writeFileSync('data-streets.json', JSON.stringify(output));
        console.log(`Saved data-streets.json (${output.length} entries)`);
      }
    });
  }
});
