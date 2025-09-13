#!/bin/bash

# Set the base CoAP server address
SERVER_IP="10.42.0.1"
SERVER_PORT="5683"

# Clear weights before sending update commands
echo "Clearing weights..."
coap-client-notls -m post coap://[$SERVER_IP]:$SERVER_PORT/ClearWeights

# Wait for a brief moment to ensure the clear operation is processed
sleep 0.5

# Array of commands to execute
commands=(
  "coap-client-notls -m post coap://[$SERVER_IP]:$SERVER_PORT/UpdateWeights -e '{\"run\":0,\"id\":\"114\",\"weight\":7.5,\"firmwareVersion\":\"1.4.0\",\"time\":\"2024-12-09T21:45:00Z\"}'"
  "coap-client-notls -m post coap://[$SERVER_IP]:$SERVER_PORT/UpdateWeights -e '{\"run\":0,\"id\":\"4\",\"weight\":8.4,\"firmwareVersion\":\"1.4.0\",\"time\":\"2024-12-09T21:45:00Z\"}'"
  "coap-client-notls -m post coap://[$SERVER_IP]:$SERVER_PORT/UpdateWeights -e '{\"run\":0,\"id\":\"176\",\"weight\":6.1,\"firmwareVersion\":\"1.4.0\",\"time\":\"2024-12-09T21:45:00Z\"}'"
  "coap-client-notls -m post coap://[$SERVER_IP]:$SERVER_PORT/UpdateWeights -e '{\"run\":0,\"id\":\"17\",\"weight\":3.2,\"firmwareVersion\":\"1.4.0\",\"time\":\"2024-12-09T21:45:00Z\"}'"
  "coap-client-notls -m post coap://[$SERVER_IP]:$SERVER_PORT/UpdateWeights -e '{\"run\":1,\"id\":\"93\",\"weight\":0,\"firmwareVersion\":\"1.4.0\",\"time\":\"2024-12-09T21:45:00Z\"}'"
)

# Execute each command with a 0.5-second delay
for cmd in "${commands[@]}"; do
  echo "Executing: $cmd"
  eval $cmd
  sleep 0.5
done

echo "All commands executed successfully."
