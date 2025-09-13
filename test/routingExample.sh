#!/bin/bash

# Set the base CoAP server address
SERVER_IP="10.42.0.1"
SERVER_PORT="5683"

coap-client-notls -m post coap://[$SERVER_IP]:$SERVER_PORT/UpdateWeights -e '{"run":1,"id":"108","weight":12,"firmwareVersion":"1.4.0","time":"2024-12-09T21:45:00Z"}'
