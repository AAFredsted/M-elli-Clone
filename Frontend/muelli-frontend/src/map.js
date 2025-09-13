import "./style.css";
import { Map, View } from "ol";
import { updateFeatureToggles } from "./mapToggle.js";
// WMTS stuff
import TileLayer from "ol/layer/Tile";
import WMTS, { optionsFromCapabilities } from "ol/source/WMTS";
import { get as getProjection } from "ol/proj";
import WMTSCapabilities from "ol/format/WMTSCapabilities";

// GeoJSON stuff
import GeoJSON from "ol/format/GeoJSON.js";
import { Vector as VectorSource } from "ol/source.js";
import { Vector as VectorLayer } from "ol/layer";
import { styleFunction } from "@/styles.js";

// Projection Stuff
import proj4 from "proj4";
import { register } from "ol/proj/proj4";

// Maplayout stuff
import Attribution from "ol/control/Attribution";
import { defaults as defaultControls } from "ol/control";

import { initTrash, resetTrash } from "./maphelpers/IconTrashcan.js";

proj4.defs("EPSG:25832", "+proj=utm +zone=32 +datum=ETRS89 +units=m +no_defs");
register(proj4);

const projection = getProjection("EPSG:25832");

const bavariaCenter = [698500, 5349150];

let subpathLayer = [];

// Creating Open Layers Map Object
const map = new Map({
	target: "map",
	controls: defaultControls({ attribution: false }),
	view: new View({
		projection: projection,
		center: bavariaCenter,
		zoom: 17,
		minZoom: 10,
		maxZoom: 40,
	}),
});

// Adding Basemap
fetch("https://geoservices.bayern.de/od/wmts/geobasis/v1/1.0.0/WMTSCapabilities.xml")
	.then((response) => response.text())
	.then((text) => {
		const parser = new WMTSCapabilities();
		const capabilities = parser.read(text);

		const options = optionsFromCapabilities(capabilities, {
			layer: "by_webkarte",
			matrixSet: "adv_utm32",
		});

		if (!options) {
			console.error("Failed to fetch map");
			return;
		}

		const wmtsSource = new WMTS(options);

		const wmtsLayer = new TileLayer({
			source: wmtsSource,
		});

		map.addLayer(wmtsLayer);

		const attribution = new Attribution({
			collapsible: false,
			label: "Map data &copy; GeoBasis-DE / BayernAtlas",
		});
		map.addControl(attribution);
	})
	.catch((error) => {
		console.error("WMTS layer not loaded correctly: ", error);
	});


// Function to add GeoJSON to the map
function addGeoJsonToMap(geoJsonObject, isSubPath) {
    if (subpathLayer.length > 0) {
        subpathLayer.forEach((layer) => map.removeLayer(layer));
        subpathLayer = [];
    }

    const vectorSource = new VectorSource({
        features: new GeoJSON().readFeatures(geoJsonObject, {
            dataProjection: "EPSG:4326",
            featureProjection: "EPSG:25832",
        }),
    });

    if (isSubPath) {
		let iteration = 0;

        // Add each route as a separate VectorLayer
        vectorSource.getFeatures().forEach((feature) => {
			const offset = iteration % 2 === 0 ? -iteration : iteration + 1;
			iteration++;
            const routeLayer = new VectorLayer({
                source: new VectorSource({
                    features: [feature],
                }),
            });
			routeLayer.setStyle(
				(feature, resolution) => {
					const style = styleFunction(feature);

					const geometry = feature.getGeometry().clone();
					geometry.translate(offset * 2 * resolution, offset * 2 * resolution);
					style.setGeometry(geometry);

					return style;
				}
			)
            map.addLayer(routeLayer);
            subpathLayer.push(routeLayer);

            // Add toggle functionality for the route
            const id = feature.get('subcycleIndex');
            document.addEventListener(`toggle${id}`, (event) => {
                routeLayer.setVisible(event.detail);
            });
        });
    } else {
        // Add the entire GeoJSON as a single VectorLayer
        const vectorLayer = new VectorLayer({
            source: vectorSource,
            style: styleFunction,
        });

        map.addLayer(vectorLayer);
        subpathLayer.push(vectorLayer);
		document.addEventListener(`toggleBase`, (event) => {
			vectorLayer.setVisible(event.detail);
		});
    }

    updateFeatureToggles(vectorSource.getFeatures());
}

// Try fetching and displaying subpath data if available
fetch("/data/subpaths.geojson")
	.then((response) => response.json())
	.then((subpathGeoJson) => {
		if (subpathGeoJson && subpathGeoJson.features && subpathGeoJson.features.length > 0) {
			console.log("Subpath found:", subpathGeoJson);
			// Display subpath data if it exists
			addGeoJsonToMap(subpathGeoJson, true);
		} else {
			console.log("No subpath data found, displaying full big.geojson");

			// If no subpath exists, display the full big.geojson
			fetch("/data/data.geojson")
				.then((response) => response.json())
				.then((geojsonObject) => {
					console.log("Full geojson loaded");
					addGeoJsonToMap(geojsonObject, false);
				})
				.catch((error) => {
					console.error("Error loading full GeoJSON data:", error);
				});
		}
	})
	.catch((error) => {
		console.error("Error loading subpath data:", error);

		// If fetching the subpath fails, fall back to displaying full big.geojson
		fetch("/data/data.geojson")
			.then((response) => response.json())
			.then((geojsonObject) => {
				console.log("Error loading subpath, displaying full geojson");
				addGeoJsonToMap(geojsonObject, false);
			})
			.catch((error) => {
				console.error("Error loading full GeoJSON data:", error);
			});
	});

initTrash(map);

window.addEventListener("newSubPath", (event) => {
	addGeoJsonToMap(event.detail, true);
});

window.addEventListener("trashIconUpdate", () => {
	resetTrash(map);
});
