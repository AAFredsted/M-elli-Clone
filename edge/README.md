# Edge

Repo for Edge component (C++ CoAP application)

## Project Components

- **CoAPServer**: Contains a wrapper library for `<coap3/coap.h>` allowing for simple setup of endpoints through start/stop/post/get functions.
- **jsonHelpers**: Helper library for state management on edge unit, which interacts with locally stored files using `<unistd.h>`.
- **debounce**: Debouncer which spawns a thread to trigger syscalls, where the timer is reset every time `void call()` is run.
- **main**: Defines endpoints using CoAPServer, applies changes to state using jsonHelpers, and runs edgeApp using debounce.

## Defined Endpoints

- **POST**: `/up`
- **POST**: `/clearweights`
- **GET**: `/CurData`

## Setup Instructions

Current setup requires pulling `gitlab.lrz.de:5005/sgc/edge/deps:$TARGETARCH` as well as `gitlab.lrz.de:5005/sgc/edgegnode:latest
` so a Docker login is required.

When this is done, the application can either be run manually or deployed together with the Thread-Router with the docker compose file found in the [/Muelli]{https://gitlab.lrz.de/sgc/muelli}  repo.

As of the implementation of a binary format for communication between SGC-units and the Edge,
the application cannot recieve JSON over CoAP.
For this reason, a python script to convert json files with SGC-data into the binary format and send a CoAP post 
a specified endpoint containing with the converted binary format has been made available in the [/Muelli]{https://gitlab.lrz.de/sgc/muelli} repo.


### Manual Setup Build with Docker Image


```shell
docker volume create edge-data
docker build --no-cache --tag edge-coap --file Dockerfile.coap .
docker run --rm   -v  /var/run/docker.sock:/var/run/docker.sock -v edge-data:/data  edge-coap:latest
```


### Manual Setup Build with Ninja
```shell
mkdir build
cd build
cmake ..
make
./coapServer #path to weights.json

```

Dummy versions of weights.json can be found in the [/edgegnode]{https://gitlab.lrz.de/sgc/edgegnode} repo on the runtimeanalysis branch.

**note: if no ipv6 address is forwardes (such as through the container setup) the application will default to ipv4.


### Sending requests
As the application requires a costum binary format for the payload, 
a python script to send requests based on json files with SGC-objects has been made available at the [/Muelli]{https://gitlab.lrz.de/sgc/muelli} repo.