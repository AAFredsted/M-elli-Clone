import express from "express";
import path from "path";
import fs from "fs";
import { WebSocketServer, WebSocket } from "ws";
import { constructPath } from "./helperfunctions/constructPath.js";

const dataPath = path.resolve("dist", "data");
if (!fs.existsSync(dataPath)) fs.mkdirSync(dataPath, { recursive: true });

const app = express();
app.use(express.json());
const PORT = 3000;

app.use(express.static(path.resolve("dist")));

const server = app.listen(PORT, () => {
	console.log(`Server is running and serving the static files on http://localhost:${PORT}`);
});


const wsServer = new WebSocketServer({ noServer: true });

server.on('upgrade', (request, socket, head) => {
  const pathname = new URL(request.url, `http://${request.headers.host}`).pathname;
  console.log(`Upgrade request for ${pathname}`);
  
  if (pathname === '/') {
    wsServer.handleUpgrade(request, socket, head, (ws) => {
      wsServer.emit('connection', ws, request);
    });
  } else {
    socket.destroy();
  }
});

wsServer.on('connection', (socket) => {
  console.log('WebSocket connection established');

  socket.on('message', (message) => {
    if (message === 'ping') {
      console.log('Received ping from client');
      return;
    }
    console.log('Received message:', message);
  });

  socket.on('close', () => {
    console.log('WebSocket connection closed');
  });
});


/**
 * Send an update to all connected clients.
 * @param type {string} Type of event (handled by the client)
 * @param data {object} Data to send to the clients. Default is an empty object.
 */
function sendUpdateToClients(type, data = {}) {
	wsServer.clients.forEach((client) => {
		if (client.readyState === WebSocket.OPEN) {
			client.send(JSON.stringify({ message: type, ...data }));
		}
	});
}

app.post("/data/addCycle", (req, res) => {
	const newFileName = `data_current.json`;
	const newFilePath = path.join(dataPath, newFileName);
	const bigGeoJsonPath = path.join(dataPath, "data.geojson");
	const subPathsFilePath = path.join(dataPath, "subpaths.geojson");
	const receivedData = req.body;

	if (!receivedData || typeof receivedData !== "object" || !Array.isArray(receivedData.subcycles)) {
		return res.status(400).json({ error: "Invalid JSON data" });
	}

	if (!fs.existsSync(dataPath)) {
		fs.mkdirSync(dataPath, { recursive: true });
	}

	fs.writeFile(newFilePath, JSON.stringify(receivedData, null, 2), (err) => {
		if (err) {
			console.error("Error saving received JSON:", err);
			return res.status(500).json({ error: "Failed to save the file" });
		}

		console.log(`File saved as ${newFileName}`);

		const subcycles = receivedData.subcycles;
		const result = constructPath(subcycles, bigGeoJsonPath, subPathsFilePath);

		if (!result.success) {
			console.error("Error generating subpaths:", result.error);
			return res.status(500).json({ error: result.error });
		}

		res.status(200).json({
			message: "File saved and subpaths generated successfully",
			savedFile: newFileName,
			subpathsFile: path.basename(result.filePath),
		});

		const file = fs.readFileSync(subPathsFilePath, "utf8");
		sendUpdateToClients("Data updated", { subpathsGeoJson: JSON.parse(file) });
	});
});

app.post("/data/metaMultiple", (req, res) => {
	// POST = Adds or updates multiple bins.
	const received = req.body;
	if (!received || !Array.isArray(received)) return res.status(400).json({ error: "Invalid JSON data" });

	const metaFilePath = path.join(dataPath, "metadata.json");
	if (!fs.existsSync(metaFilePath)) {
		fs.writeFileSync(metaFilePath, JSON.stringify(received, null, 2));
		return res.status(200).json({ message: "Added metadata successfully (new file created)." });
	}

	const file = fs.readFileSync(metaFilePath, "utf8");
	let metadata = JSON.parse(file);

	if (!Array.isArray(metadata)) return res.status(500).json({ error: "Invalid metadata file" });

	let successfulOperations = 0;
	received.forEach((meta) => {
		if (!meta.id || !meta.latitude || !meta.longitude) return;

		const existingIdx = metadata.findIndex((data) => data.id === meta.id);
		if (existingIdx >= 0) metadata[existingIdx] = meta;
		else metadata.push(meta);

		successfulOperations++;
	});

	fs.writeFileSync(metaFilePath, JSON.stringify(metadata, null, 2));
	const message = successfulOperations === received.length ? `Added ${successfulOperations} metadata successfully.` : `Added ${successfulOperations} metadata successfully (with ${received.length - successfulOperations} errors).`;
	res.status(200).json({ message: message });
	sendUpdateToClients("Meta updated", {});
});
app.post("/data/meta", (req, res) => {
	// POST = New bin!

	// Check receiving data
	const received = req.body;
	if (!received || typeof received !== "object") return res.status(400).json({ error: "Invalid JSON data" });
	if (!received.id || !received.latitude || !received.longitude) return res.status(400).json({ error: "Missing required fields" });

	// Check if file exists. If so, read it and update the data. If not, create it.
	const metaFilePath = path.join(dataPath, "metadata.json");
	if (!fs.existsSync(metaFilePath)) {
		fs.writeFileSync(metaFilePath, JSON.stringify([received], null, 2));
		return res.status(200).json({ message: "Added metadata successfully (new file created)." });
	}

	const file = fs.readFileSync(metaFilePath, "utf8");
	const metadata = JSON.parse(file);
	if (!Array.isArray(metadata)) return res.status(500).json({ error: "Invalid metadata file" });

	if (metadata.some((data) => data.id === received.id))
		// Any ID matches
		return res.status(400).json({ error: "ID already exists!" });

	metadata.push(received);
	fs.writeFileSync(metaFilePath, JSON.stringify(metadata, null, 2));
	res.status(200).json({ message: "Added metadata successfully." });
	sendUpdateToClients("Meta updated", {});
});
app.put("/data/meta", (req, res) => {
	// UPDATE, bin moved or whatever

	// Check receiving data
	const received = req.body;
	if (!received || typeof received !== "object") return res.status(400).json({ error: "Invalid JSON data" });
	if (!received.id || !received.latitude || !received.longitude) return res.status(400).json({ error: "Missing required fields" });

	// Check file exists. In this case, if it doesn't exist we throw an error (can't update a non-existing file).
	const metaFilePath = path.join(dataPath, "metadata.json");
	let file;
	try { file = fs.readFileSync(metaFilePath, "utf8"); }
	catch (err) { return res.status(500).json({ error: "Metadata file does not exist" }); }

	const metadata = JSON.parse(file);
	if (!Array.isArray(metadata)) return res.status(500).json({ error: "Invalid metadata file" });
	if (!metadata.some((data) => data.id === received.id))
		// No ID matches
		return res.status(400).json({ error: "ID does not exist!" });

	// Update the data
	const updated = metadata.map((data) => data.id === received.id ? received : data);
	fs.writeFileSync(metaFilePath, JSON.stringify(updated, null, 2));
	res.status(200).json({ message: "Updated metadata successfully." });
	sendUpdateToClients("Meta updated");
});
