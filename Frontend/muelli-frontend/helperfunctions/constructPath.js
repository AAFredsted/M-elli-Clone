import fs from 'fs';
import * as turf from '@turf/turf'; // Import turf.js for geometry operations

const colorIndex = [
  '#FF5733', '#33FF57', '#3357FF', '#FF33A1', '#FF8C33',
  '#33FFF5', '#8C33FF', '#FFD733', '#33FF8C', '#FF3333'
];

// Helper function to find the common streetIds between two points
const findCommonStreetIds = (startNode, endNode) => {
  const startStreetIds = new Set(startNode.streetIds);
  const endStreetIds = new Set(endNode.streetIds);
  const commonStreetIds = [];
  for (const id of startStreetIds) {
    if (endStreetIds.has(id)) {
      commonStreetIds.push(id);
    }
  }
  return commonStreetIds;
};

// Helper function to parse a POINT string into coordinates
const parsePoint = (pointString) => {
  const coords = pointString.replace('POINT (', '').replace(')', '').split(' ');
  return [parseFloat(coords[0]), parseFloat(coords[1])];
};

const constructPath = (subcycles, originData, newGeoJsonPath) => {
  const originGeoJson = JSON.parse(fs.readFileSync(originData, 'utf-8'));

  const cycleFeatures = [];
  const lineStringCache = new Map();

  subcycles.forEach((subCycle, subcycleIndex) => {
    const lineStrings = [];

    for (let i = 0; i < subCycle.length - 1; i++) {
      const startNode = subCycle[i];
      const endNode = subCycle[i + 1];

      const commonStreetIds = findCommonStreetIds(startNode, endNode);
      if (!commonStreetIds || commonStreetIds.length === 0) {
        console.warn(`No common streetId between points ${i} and ${i + 1}`);
        continue;
      }

      let longestLineString = null;
      let maxLength = 0;

      for (const commonStreetId of commonStreetIds) {
        const cacheKey = `${commonStreetId}`;
        let lineString = lineStringCache.get(cacheKey);

        if (!lineString) {
          const feature = originGeoJson.features.find(f => f.properties['@id'] === commonStreetId);
          if (!feature) {
            console.warn(`Street ID ${commonStreetId} not found in originGeoJson`);
            continue;
          }
          lineString = feature.geometry;
          if (lineString.type !== 'LineString') {
            console.warn(`Feature with ID ${commonStreetId} is not a LineString`);
            continue;
          }
          lineStringCache.set(cacheKey, lineString);
        }

        const startCoords = parsePoint(startNode.Point);
        const endCoords = parsePoint(endNode.Point);

        const snappedStart = turf.nearestPointOnLine(lineString, turf.point(startCoords));
        const snappedEnd = turf.nearestPointOnLine(lineString, turf.point(endCoords));

        const slicedLine = turf.lineSlice(turf.point(snappedStart.geometry.coordinates), turf.point(snappedEnd.geometry.coordinates), lineString);

        if (slicedLine && slicedLine.geometry.coordinates.length > 0) {
          const length = turf.length(slicedLine);
          if (length > maxLength) {
            maxLength = length;
            longestLineString = slicedLine;
          }
        } else {
          console.warn(`Sliced line is empty between points ${i} and ${i + 1}`);
        }
      }

      if (longestLineString) {
        lineStrings.push(longestLineString.geometry.coordinates);
      }
    }

    if (lineStrings.length > 0) {
      const multiLineString = turf.multiLineString(lineStrings, {
        subcycleIndex: subcycleIndex,
        description: `Subcycle ${subcycleIndex + 1}`,
        color: colorIndex[subcycleIndex % colorIndex.length]
      });
      cycleFeatures.push(multiLineString);
    }
  });

  const newGeoJson = {
    type: 'FeatureCollection',
    features: cycleFeatures,
  };

  fs.writeFileSync(newGeoJsonPath, JSON.stringify(newGeoJson, null, 2), 'utf-8');

  console.log(`Generated subpaths.geojson at ${newGeoJsonPath}`);
  return { success: true, filePath: newGeoJsonPath };
};

export { constructPath };