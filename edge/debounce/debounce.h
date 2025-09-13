#include <chrono>
#include <cstdint>
#include <thread>
#include <functional>
#include <atomic>
#include <iostream>

namespace Debounce {

template <typename Func>
class Debouncer
{
public:
    Debouncer(int delay_ms, Func func)
        : delay(delay_ms), callback(func), call_flag(false), stop_flag(false)
    {
        timer_thread = std::thread(&Debouncer::timerFunction, this);
    }

    ~Debouncer()
    {
        
        stop_flag.store(true);
        if (timer_thread.joinable())
        {
            timer_thread.join();
        }
    }

    void call()
    {
        last_call_time = std::chrono::steady_clock::now();
        call_flag = true;
    }

private:
    u_int64_t delay;
    Func callback;
    std::chrono::steady_clock::time_point last_call_time;
    std::atomic<bool> call_flag;
    std::atomic<bool> stop_flag;
    std::thread timer_thread;

    void timerFunction()
    {
        while (!stop_flag.load())
        {
            if (call_flag.load())
            {
                auto now = std::chrono::steady_clock::now();
                auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(now - last_call_time).count();
                if (duration >= delay)
                {
                    callback();
                    call_flag = false;
                    std::this_thread::sleep_for(std::chrono::milliseconds(4000));
                }
            }
            std::this_thread::sleep_for(std::chrono::milliseconds(500));
        }
    }
};
}