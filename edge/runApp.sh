#!/bin/sh
# Ensure a Docker volume named 'weights' exists
# Command for this is: docker volume create weights
#docker volume create weights

# Run the parent container (edgecoap-builder:latest)
# Map the Docker socket for Docker-in-Docker functionality
# Map the 'weights' volume to a directory inside the container
# Ensure that ipv6 has been enabled by the docker daemon  in /etc/docker/daemon.json
#config to include:
#{
#  "ipv6": true,
#  "fixed-cidr-v6": "2001:db8:1::/64"
#}

#now, we can run the following command
#docker run -p -p [::]:5683:5683/udp  \
#    -v /var/run/docker.sock:/var/run/docker.sock \
#    -v weights:/data/weights.json \
#    edgecoap-builder:latest

# If the parent container (C++) executes another container, ensure the correct volume mapping:
docker run --network=host --rm --name pathfinder -v edge-data:/opt/app/data/ gitlab.lrz.de:5005/sgc/edgegnode:latest
