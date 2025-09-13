//#pragma once

#include "CoAPServer/CoAPServer.h"
#include "jsonHelpers/jsonHelpers.h"
#include "debounce/debounce.h"
#include "idDecoder/id_decoder.h"
#include "vector"

#include <signal.h>
#include <iostream>
#define COAP_LISTEN_UCAST_IP "::"

static std::string weights_path;

typedef struct __attribute__((packed)) {
    uint32_t id : 32; // encoded to 32 bits
    uint32_t timestamp : 32; // Until 2106
    uint16_t weight : 16; // Multiplied by 10000 to remove float
    uint8_t firmwareVersion : 8; // Max 256
} data_packet_t;

static void send_json(coap_pdu_t *response, const coap_pdu_code_t code, const char *message) {
    cJSON *resJson = cJSON_CreateObject();
    cJSON_AddStringToObject(resJson, "message", message);
    char *resStr = cJSON_PrintUnformatted(resJson);

    coap_pdu_set_code(response, code);
    coap_add_data(response, strlen(resStr), reinterpret_cast<const uint8_t *>(resStr));
    cJSON_free(resStr);
    cJSON_Delete(resJson);
}

static void send_response(coap_pdu_t *res, const coap_pdu_code_t code) {
    coap_pdu_set_code(res, code);
    coap_add_data(res, 0, nullptr); // Empty response
}

void runApp(){
    std::cout << "We run app" << std::endl;
    system("./runApp.sh");
}


Debounce::Debouncer<void(*)()>* debounceCaller;

int main(int argc, char **argv) {
    if (argc < 2) {
        std::cerr << "Usage: " << argv[0] << " <Path to weights>" << std::endl;
        return 1;
    }

    weights_path = argv[1];

    debounceCaller = new Debounce::Debouncer(2500, runApp);

    CoAPServer server;
    if (server.init(COAP_LISTEN_UCAST_IP)) {
        std::cout << "Server initialised successfully" << std::endl;
    } else {
        std::cerr << "Failed to start server" << std::endl;
    }
    server.start();

    coap_method_handler_t updateWeightsHandler = [](auto, auto, const coap_pdu_t *request, auto, coap_pdu_t *response) {
        size_t len;
        const uint8_t *payloadBuffer;

        coap_show_pdu(COAP_LOG_INFO, request);
        coap_get_data(request, &len, &payloadBuffer);

        // Parse incoming binary payload
        if (len != sizeof(data_packet_t))
            return send_response(response, COAP_RESPONSE_CODE_BAD_REQUEST);

        auto [encoded_id, timestamp, encoded_weight, firmwareVersion] = *reinterpret_cast<const data_packet_t*>(payloadBuffer);
        std::string id = id_decoder(encoded_id).get_id();
        float weight = encoded_weight / 10000.0f;

        printf("Received data: ID: %s, Timestamp: %u, Weight: %.4f, Firmware Version: %u\n", id.c_str(), timestamp, weight, firmwareVersion);

        if (firmwareVersion <= 0 || firmwareVersion > 256
            || timestamp > 0xFFFFFFFF
            || weight > 5 || weight < 0)
            return send_response(response, COAP_RESPONSE_CODE_BAD_REQUEST);

        if (!jsonHelpers::updateWeights(weights_path, id, weight, firmwareVersion, timestamp))
            return send_response(response, COAP_RESPONSE_CODE_INTERNAL_ERROR);

        debounceCaller->call();
        send_response(response, COAP_RESPONSE_CODE_VALID);
    };
    server.post("up", updateWeightsHandler);

    server.post("ClearWeights", [](auto, auto, const coap_pdu_t *req, auto, coap_pdu_t *response) {
        coap_show_pdu(COAP_LOG_WARN, req);

        // Use helper function to clear weights file
        if (!jsonHelpers::clearWeights(weights_path))
            return send_json(response, COAP_RESPONSE_CODE_INTERNAL_ERROR, "Failed to clear weights file");

        // Send success response
        send_json(response, COAP_RESPONSE_CODE_CHANGED, "Weights file cleared successfully");
    });

    //gets current weights
    server.get("CurData", [](auto, auto, const coap_pdu_t *req, auto, coap_pdu_t *res) {
        const std::string data = jsonHelpers::readJson(weights_path);

        coap_show_pdu(COAP_LOG_WARN, req);
        coap_pdu_set_code(res, COAP_RESPONSE_CODE_CONTENT);
        coap_add_data(res, data.size(), reinterpret_cast<const uint8_t *>(data.c_str()));
        coap_show_pdu(COAP_LOG_WARN, res);
    });

    // Wait until the user sends a sigint signal (or CTRL-C)
    sigset_t sigset;
    int signal;

    sigemptyset(&sigset);
    sigaddset(&sigset, SIGINT);
    sigprocmask(SIG_BLOCK, &sigset, nullptr);
    sigwait(&sigset, &signal);

    server.stop();
    delete debounceCaller;
    return 0;
}
