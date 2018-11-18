package com.example.sajin.aot_cam.Constants

enum class PwmRegistry(var address: Int) {
    MODE1(0x00),
    MODE2(0x01),
    SUBADR1(0x02),
    SUBADR2(0x03),
    SUBADR3(0x04),
    PRESCALE(0xFE),
    LED_0_ON_L(0x06),
    LED_0_ON_H(0x07),
    LED_0_OFF_L(0x08),
    LED_0_OFF_H(0x09),
    ALL_LED_ON_L(0xFA),
    ALL_LED_ON_H(0xFB),
    ALL_LED_OFF_L(0xFC),
    ALL_LED_OFF_H(0xFD),

}