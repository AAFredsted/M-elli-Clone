# Mülli IoT Repository

This repository contains the code responsible for our the software side of our garbage cans.
As we use the ESP32 family of microcontrollers, this code depends on the
[ESP-IDF](https://github.com/espressif/esp-idf) framework, which must be set up separately.

## Repository Structure

The code in this repository is split in three parts separated into three directories.

#### `components/lib/`

The lib component contains all code that can be abstracted away from the hardware.
It is responsible for evaluating the collected sensor data before an update is sent to the edge
component.
Since this component must compile in different environments, it should be implemented in a portable
way.

#### `main/`

The main component is the entrypoint for our ESP application.
All hardware specific code must be placed here.
This component also provides a high level hardware abstraction layer for our library to interact
with the ESP-specific code.

#### `test/`

The test component contains test cases for the lib component.
It uses [GoogleTest](https://github.com/google/googletest) as a test framework and compiles to a
native binary.
ESP-specific code is not available here so all hardware abstractions provided by the main component
need to be mocked.

We chose this approach over the ESP-IDF linux target with the integrated unit test framework as it
is poorly documented and it is therefore unclear if it covers our use case well.
Note that we may still make use of the integrated framework for hardware tests in the future.

## Development Environment

To successfully build this project, the ESP-IDF framework as well as its dependencies are required.

### Nix

If you have [nix](https://nixos.org/) and [direnv](https://github.com/direnv/direnv) installed, just
`cd` into the cloned directory and approve the contents of the `.envrc` file.
Everything else will be set up automatically.

### Docker Container

Expressif provide a [preconfigured docker image](https://hub.docker.com/r/espressif/idf) that has
all required components set up.
While it is mainly useful for CI, it may be useful for development as well.

### Manual(ish) Installation

Prerequisites can be installed like this:

```shell
# Debian and derivatives
apt install git wget flex bison gperf python3 python3-pip python3-venv cmake ninja-build ccache libffi-dev libssl-dev dfu-util libusb-1.0-0

# Arch
sudo pacman -S --needed gcc git make flex bison gperf python cmake ninja ccache dfu-util libusb

# Source and more info:
# https://docs.espressif.com/projects/esp-idf/en/stable/esp32/get-started/linux-macos-setup.html
```

The rest of the setup can be automated with in vscode with the
[ESP-IDF extension](https://marketplace.visualstudio.com/items?itemName=espressif.esp-idf-extension).

## Build and Run

We use cmake presets to tailor the environment for both the ESP and CI builds. To configure the
project for ESP development, use `cmake --preset esp .`.
The project can then be built and deployed with `idf.py -p /dev/ttyUSB1 flash` (replace /dev/ttyUSB1
with the correct path to the serial port).

For the CI tester, configure, build and run the project like this:

```shell
cmake --preset ci-test .
cd build-ci
ninja -j $(nproc)
./test/test_runner
```
