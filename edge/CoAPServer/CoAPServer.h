#ifndef SERVERCLASS_H
#define SERVERCLASS_H

#include <string>
#include <coap3/coap.h>
#include <thread>
#include <atomic>

class CoAPServer {
    coap_context_t *ctx;
    std::thread server_thread;
    std::atomic<bool> running;

    void server_loop();
    void create_endpoint(const char *name, coap_request_t req_type, coap_method_handler_t handler);
public:
    CoAPServer();
    ~CoAPServer();
    bool init(const char *uri, coap_log_t log_level = COAP_LOG_INFO);

    void get(const char *name, coap_method_handler_t handler);
    void post(const char *name, coap_method_handler_t handler);
    void put(const char *name, coap_method_handler_t handler);
    void del(const char *name, coap_method_handler_t handler);

    void start();
    void stop();
};

#endif // SERVERCLASS_H