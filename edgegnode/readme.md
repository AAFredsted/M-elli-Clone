# Edge GNodes Documentation

## Overview

This repository contains code designed to run on **Edge (Raspberry Pi)** units. It processes incoming garbage can weights and uses **Dijkstra's algorithm** to repeatedly generate a path. This path is then sent to a remote server. The application solves the **Capacitated Vehicle Routing Problem (CVRP)**, which you can read more about [here](https://www.sciencedirect.com/science/article/abs/pii/S0377221711006692?via%3Dihub).

Please note that this repository is **separate** from the **Edge Repo** ([Link to Edge Repo](https://gitlab.lrz.de/sgc/edge)), as it does not directly handle CoAP connections from ESPs. It is solely responsible for path generation and the subsequent transmission of that data to the remote server.

### Key Features:
- **GnodeList datastructure** for compact storage of geospatial data as adjacencylist
- **Dijkstra's Algorithm** for pathfinding.
- **Capacitated Vehicle Routing Problem** solution.
- Runs on **Raspberry Pi** edge devices.
- Communication with remote server for data transfer.

## System Requirements

- **Java 21** (Java 21 SDK).
- **Gradle** (for build management).

### Dependencies:
To add additional dependencies to the project, simply reference them in the `build.gradle.kts` file. After editing the build file, run `gradle` or `./gradlew` to download the required dependencies.

## Building and Running the Project

### Build Project

To build the project, use the following command:

```bash
gradle build
./gradlew build
```

This will compile the project, and generate the necessary build files.

### Run Tests

To run tests on the project, use the following command:

```bash
gradle test
./gradlew test
```

This will execute all test cases in the project.

### Run Project

To run the project, use the following command:

```bash
 gradle run --args='-a [area] -p  [weigthsPath]'
./gradlew run --args='-a [area] -p  [weigthsPath]'
```

This will launch the application for development purposes.

### Docker Support

You can also build and run the project using Docker.

#### Build Docker Image

To build the Docker image, run:

```bash
docker build --no-cache --tag edge-first --file Dockerfile.edge .
```

#### Run Docker Image

To run the Docker image, use the following command:

```bash
docker run -v [path to weights.json]:/opt/app/data/weights.json edge-first:latest
```

Ensure to replace `[path to weights.json]` with the correct file path for your `weights.json` file.

### Running the Application Directly from JAR

To run the application directly from a JAR file, use the following command:

```bash
java -Dapp.path=[path to /app] -jar ./app/build/libs/app-all-all.jar [file | string] [start | int] [end | int]
```

Replace `[path to /app]`, `[file | string]`, `[start | int]`, and `[end | int]` with appropriate values based on your environment and configuration.

# Optional Arguments for Edge GNodes Application

The application supports several optional arguments that can be used to customize the behavior when running the program.

## Arguments Overview

| Argument          | Short Flag | Long Flag            | Description                                                                                   | Default Value                       |
|-------------------|------------|----------------------|-----------------------------------------------------------------------------------------------|-------------------------------------|
| Area Name         | `-a`       | `--area`             | The name of the area matching the first section of a JSON file in the data folder.            | Required                           |
| Weights Path      | `-p`       | `--weightsPath`      | Path to a folder where the `weights.json` file can be found to load the data.                 | Required                           |
| Endpoint URL      | `-url`     | `--endpoint`         | Optional URL for the hosted frontend. Defaults to "https://muelli.orchards.dev/".             | `https://muelli.orchards.dev/`      |
| Edge ID           | `-eID`     | `--edgeID`           | Optional ID for edge application communication, used to generate IDs for the web server.     | `GAR-1-`                           |
| Start Gnode ID    | `-sID`     | `--startGnodeID`     | Optional starting point for the pathfinding algorithm. This ID is used in the application.    | `1134`                              |
| Max Weight        | `-w`       | `--maxWeight`        | Optional upper bound for a Garbage Truck (GT) cycle.                                          | `12`                                |
| Count Garbage Trucks | `-cGT` | `--countGarbageTruck` | Optional upper bound for the number of garbage trucks to calculate cycles for.                | `5`                                 |


## Dummy Documents
The main repo contains GeoJson files and generated GNodeGraph files for the areas shown during the Sprint Presentations and the Final Demo, being Campus and Garching.
Dummy Weights files and further geojson files can be found in the /runtimeAnalysis branch, which also contains the setup for the data collection used for the runtime graphs in the final report.
