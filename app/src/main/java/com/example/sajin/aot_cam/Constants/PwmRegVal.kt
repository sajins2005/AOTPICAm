package com.example.sajin.aot_cam.Constants

enum class PwmRegVal(var regval: Int) {
    //#Bits
    RESTART(0x80),
    SLEEP(0x10),
    ALLCALL(0x01),
    INVRT(0x10),
    OUTDRV(0x04),
    DC_PIN_LOW(0),
    DC_PIN_HIGH(4096),
    MIN_SPEED(0),
    MAX_SPEED(255)
}
