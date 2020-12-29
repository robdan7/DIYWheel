# DIYWheel
~~Android app and~~ arduino controller for the VESC. This uses the Adafruit BLE libraries and might not be compatible with standard arduino devices.
## Current implementation
The arduino code can send data between VESC and app as a transparent transmission layer. However, large data packets from BLE over approx. 64 byte (e.g. app configs) doesn't seem to work at the moment.
