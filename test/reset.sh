#!/bin/bash

# Set the base CoAP server address
SERVER_IP="10.42.0.1"
SERVER_PORT="5683"

# Clear weights before sending update commands
echo "Clearing weights..."
coap-client-notls -m post coap://[$SERVER_IP]:$SERVER_PORT/ClearWeights

# Wait for a brief moment to ensure the clear operation is processed
sleep 0.5

echo "Weights Cleared..."
