import { fromLonLat } from "ol/proj";
import Overlay from "ol/Overlay";

// Coordinates in EPSG:4326
const start =  [11.6459778, 48.2642061];
let overlays = [];

const popup = document.getElementById("popup");

export const resetTrash = (map) => {
	if (overlays) overlays.forEach((overlay) => map.removeOverlay(overlay));
	addTrash(map);
};

export const initTrash = (map) => {
	let closer; // Variable for setTimeout.
	popup.dataset.selected = "null";

	// Create an overlay to anchor the popup to the map.
	const popupOverlay = new Overlay({
		element: popup,
	});
	map.addOverlay(popupOverlay);

	const observer = new MutationObserver((mutations) => {
		mutations.forEach(() => {
			if (popup.dataset.selected === "null") return popupOverlay.setPosition(undefined);

			const selected = JSON.parse(popup.dataset.selected);
			const transformedCoord = fromLonLat([selected.longitude, selected.latitude], "EPSG:25832");
			popupOverlay.setPosition(transformedCoord);

			if (closer) clearTimeout(closer);

			popup.innerHTML = `
				<h2>${selected.id}</h2>
				Location: ${selected.latitude}, ${selected.longitude}<br>
				Last Weight: ${selected.lastWeight.toFixed(2)}<br>
				Last Update: ${selected.lastUpdate}<br>
			`;
			popup.dataset.showing = "true";
			popup.style.opacity = "1";
		});
	});
	observer.observe(popup, { attributeFilter: ["data-selected"] });

	map.addEventListener("click", () => {
		if (popup.dataset.showing === "true") {
			// Hide popup if showing
			popup.style.opacity = "0";
			popup.dataset.showing = "false";
			closer = setTimeout(() => popupOverlay.setPosition(undefined), 350);
		}
	});

	addTrash(map);
};

//convert to epsg:25832
const addTrash = async (map) => {
	const response = await fetch("/data/metadata.json");
	const metadata = await response.json();

	metadata.forEach((can) => {
		const transformedCoord = fromLonLat([can.longitude, can.latitude], "EPSG:25832");

		const trashcanElement = document.createElement("div");
		trashcanElement.className = "trashcan-icon";
		trashcanElement.style.backgroundImage = `url(/icons/trashcan.svg)`;
		trashcanElement.style.backgroundSize = "contain";
		trashcanElement.style.width = "32px";
		trashcanElement.style.height = "32px";

		// I wish I could just use click here, but clicks are also drags, and we don't want to snap back on dragging the map.
		let startX = 0;
		let startY = 0;
		const permittedDelta = 5;

		trashcanElement.addEventListener("mousedown", (ev) => {
			startX = ev.clientX;
			startY = ev.clientY;
		});
		trashcanElement.addEventListener("mouseup", (ev) => {
			const endX = ev.clientX;
			const endY = ev.clientY;

			if (Math.abs(endX - startX) > permittedDelta || Math.abs(endY - startY) > permittedDelta) return;

			map.getView().centerOn(transformedCoord, map.getSize(), [map.getSize()[0] / 2, map.getSize()[1] / 2]);
			popup.dataset.selected = JSON.stringify(can);
		});

		// Create an overlay
		const trashcanOverlay = new Overlay({
			position: transformedCoord,
			positioning: "center-center",
			element: trashcanElement,
			stopEvent: false,
		});
		map.addOverlay(trashcanOverlay);
		overlays.push(trashcanOverlay);

		// Update the popup if the selected trashcan is updated
		const selected = JSON.parse(popup.dataset.selected);
		if (selected && can.id === selected.id) popup.dataset.selected = JSON.stringify(can);
	});

	const garbagetruckcoord = fromLonLat(start, "EPSG:25832");

	// Create a DOM element for the trashcan
	const trashcanElement = document.createElement("div");
	trashcanElement.className = "garbagetruck-icon";
	trashcanElement.style.backgroundImage = `url(/icons/garbagetruck.svg)`;
	trashcanElement.style.backgroundSize = "contain";
	trashcanElement.style.width = "32px";
	trashcanElement.style.height = "32px";

	// Create an overlay
	const trashcanOverlay = new Overlay({
		position: garbagetruckcoord,
		positioning: "center-center",
		element: trashcanElement,
		stopEvent: false,
	});

	map.addOverlay(trashcanOverlay);
};
