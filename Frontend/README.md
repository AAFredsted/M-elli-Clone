# Muelli Frontend

This is the frontend for the Muelli project. It runs on a remote server and is connected to by the ["Edge Node"](https://gitlab.lrz.de/sgc/edge) to send shortest path data, as well as metadata about the garbage cans.
The purpose of this code is simply to visualise different paths and the state of the garbage cans in the city.


## Development Setup: 
Available at http://localhost:5173, does not include the server and websocket!
```bash
cd muelli-frontend
npm start
```

## Test and Production Build 
With server and websocket, usually at http://localhost:3000
```bash
cd muelli-frontend
npm run build:test
node server.js
```
_Note: You can remove the `:test` if you do not need to test._

## Extra Building Options
To run production build locally:
```bash
python3 -m http.server # run from /dist
node server.js # run from /muelli-frontend
```

Docker Container Setup
```bash
Build Dockerimage: docker build --no-cache  --tag muelli-frontend --file Dockerfile.frontend .
Run Dockerimage: docker run -p 3000:3000 muelli-frontend:latest    
Stop Container: get container id using 'docker ps' and run docker 'stop [id]'
```

## Testing
Tests are implemented using the Playwright test-runner, which provides a very 
user like API for E2E tests. For our case, we primarily provide simple frontend-tests,
checking the availability of endpoints and state changes to user behavior.

To run tests:
```bash
npm run test
```

## Send Data Manually (with curl)
To curl against the addcycle endpoint to trigger websocket updates:
curl -X POST -H "Content-Type: application/json" -d @[path to json] http://localhost:3000/data/addCycle


## Change underlying vector map
The Website displays the cycles by finding features in the /data/data.geojson file that match the latest cycles sent, which are stored in data_current.json.
Take care to ensure that the data.geojson file is the same as the underlying .geojson file used to generate the GNodeGraph for the website.
Otherwise, the website will not be able to find matching features and not display the cycles.
