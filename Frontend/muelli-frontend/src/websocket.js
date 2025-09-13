// WebSocket connection setup

const wsURL = process.env.NODE_ENV === "prod" ? "wss://muelli.orchards.dev/" : "ws://localhost:3000/";

const ws = new WebSocket(wsURL);

ws.onopen = () => {
	ws.send("Socket established");
	keepAlive();
};

ws.onmessage = (event) => {
	// For now, we'll use "message" as the event type for all messages.
	// Something more clearly defined would be better.
	console.log("Received update from server:", event.data);
	const data = JSON.parse(event.data);
	if (data.message === "Data updated") {
		const event = new CustomEvent("newSubPath", {
			detail: data["subpathsGeoJson"],
		});
		window.dispatchEvent(event);
	} else if (data.message === "Meta updated") {
		const event = new CustomEvent("trashIconUpdate", {
			detail: data["trashcan"],
		});
		window.dispatchEvent(event);
	} else {
		console.error(`Message type '${data.message}' not recognised by handler.`);
	}
};

ws.onclose = () => {
	console.log("WebSocket connection closed");
	stopkeepAlive();
};

ws.onerror = (error) => {
	console.error("WebSocket error:", error);
	stopkeepAlive();
};

let keepAliveInterval;

const keepAlive = () => {
	keepAliveInterval = setInterval(() => {
		if (ws.readyState === WebSocket.OPEN) {
			console.log("ping sent to keep connection");
			ws.send("ping");
		}
	}, 30000);
};

const stopkeepAlive = () => {
	clearInterval(keepAliveInterval);
};
