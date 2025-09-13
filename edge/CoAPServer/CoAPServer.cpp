#include "CoAPServer.h"

#include <thread>
#include <atomic>
#include <coap3/coap.h>

void CoAPServer::server_loop() {
    while (running) coap_io_process(ctx, 100);
}

void CoAPServer::create_endpoint(const char *name, const coap_request_t req_type, const coap_method_handler_t handler) {
    coap_resource_t* resource = coap_resource_init(coap_make_str_const(name), 0);
    coap_register_handler(resource, req_type, handler);
    coap_add_resource(ctx, resource);
}

// public:

CoAPServer::CoAPServer() : ctx(nullptr), running(false) {}
CoAPServer::~CoAPServer() { stop(); }

bool CoAPServer::init(const char *uri, const coap_log_t log_level) {
    fprintf(stdout, "Setting up server at URI %s\n", uri);

    coap_str_const_t *my_address = coap_make_str_const(uri);

    // Setting up CoAP
    coap_startup();
    coap_set_log_level(log_level); // Todo: Add options to change

    // Creating a new context
    ctx = coap_new_context(nullptr);
    if (!ctx) {
        coap_log_emerg("Could not create CoAP context\n");
        stop();
    }

    // Allow libcoap to handle multi-block payloads
    coap_context_set_block_mode(ctx, COAP_BLOCK_USE_LIBCOAP | COAP_BLOCK_SINGLE_BODY);

    // Resolve address info
    const uint32_t scheme_hint_bits = coap_get_available_scheme_hint_bits(0, 0, COAP_PROTO_NONE);
    coap_addr_info_t *info_list = coap_resolve_address_info(my_address, 0, 0, 0, 0, 0, scheme_hint_bits,
                                                            COAP_RESOLVE_TYPE_LOCAL);

    // Create CoAP endpoints
    bool have_ep = false;
    for (coap_addr_info_t *info = info_list; info; info = info->next) {
        coap_endpoint_t *ep = coap_new_endpoint(ctx, &info->addr, info->proto);
        if (!ep) {
            coap_log_warn("cannot create endpoint for proto %u\n", info->proto);
        } else {
            have_ep = true;
        }
    }
    coap_free_address_info(info_list);
    if (!have_ep) {
        coap_log_err("No context for listening on %s\n", (char*)my_address->s);
        stop();
    }

    running = true;
    fprintf(stdout, "Ready!\n");
    return running;
}

void CoAPServer::get(const char *name, const coap_method_handler_t handler) {
    create_endpoint(name, COAP_REQUEST_GET, handler);
}
void CoAPServer::post(const char *name, const coap_method_handler_t handler) {
    create_endpoint(name, COAP_REQUEST_POST, handler);
}
void CoAPServer::put(const char *name, const coap_method_handler_t handler) {
    create_endpoint(name, COAP_REQUEST_PUT, handler);
}
void CoAPServer::del(const char *name, const coap_method_handler_t handler) {
    create_endpoint(name, COAP_REQUEST_DELETE, handler);
}

void CoAPServer::start() {
    server_thread = std::thread(&CoAPServer::server_loop, this); // Start server loop in separate thread
}
void CoAPServer::stop() {
    if (running) {
        running = false;
        if (server_thread.joinable()) server_thread.join();
        coap_free_context(ctx);
        coap_cleanup();
    }
}
