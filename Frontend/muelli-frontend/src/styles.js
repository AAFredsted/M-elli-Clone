import { Fill, Stroke, Style, Icon } from "ol/style.js";

// Define styles for different geometry types
const styles = {
	LineString: (feature) => new Style({
		stroke: new Stroke({
			color: feature.get("color") || "black",
			width: 3,
		}),
	}),
	MultiLineString: (feature) => new Style({
		stroke: new Stroke({
			color: feature.get("color") || "green",
			width: 2,
		}),
	}),
	MultiPolygon: (feature) => new Style({
		stroke: new Stroke({
			color: feature.get("color") || "yellow",
			width: 1,
		}),
		fill: new Fill({
			color: "rgba(255, 255, 0, 0.1)",
		}),
	}),
	Polygon: (feature) => new Style({
		stroke: new Stroke({
			color: feature.get("color") || "blue",
			lineDash: [4],
			width: 3,
		}),
		fill: new Fill({
			color: "rgba(0, 0, 255, 0.1)",
		}),
	}),
	// Add a custom style for trashcan features
	Trashcan: new Style({
		image: new Icon({
			src: "@/icons/trashcan.svg", // Path to trashcan icon
			scale: 0.6,
		}),
	}),
};

// The style function that applies the appropriate style based on the feature's geometry type
const styleFunction = (feature) => {
	// Check if the feature has a geometry type of 'Point' (for trashcan icons)
	if (feature.getGeometry().getType() === "Point") {
		return styles["Trashcan"]; // Return the trashcan style
	}

	// Otherwise, return the style for the feature's geometry type (LineString, Polygon, etc.)
	const geometryType = feature.getGeometry().getType();
	return styles[geometryType] ? styles[geometryType](feature) : null;
};

export { styleFunction };
