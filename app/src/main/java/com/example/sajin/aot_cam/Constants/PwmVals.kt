package com.example.sajin.aot_cam.Constants

enum class PwmVals(var vals: Int) {


    DC_PIN_LOW(0),
    DC_PIN_HIGH(4096),

    MIN_SPEED(0),
    MAX_SPEED(255),


    MOTOR_ONE(1),
    MOTOR_TWO(2),
    MOTOR_THREE(3),
    MOTOR_FOUR(4),

    STEPPER_ONE(1),
    STEPPER_TWO(2),

    DefualtI2cAddress(0x60)

}

enum class StepperStyle() {
    SINGLE,
    DOUBLE,
    INTERLEAVE,
    MICROSTEP
}

enum class StepperDirection() {
    FORWARD,
    BACKWARD

}

data class STepperPin(val motorNumber: PwmVals) {
    val PWMA: Int
    val AIN2: Int
    val AIN1: Int
    val PWMB: Int
    val BIN2: Int
    val BIN1: Int

    init {
        if (motorNumber == PwmVals.STEPPER_ONE) {
            PWMA = 8
            AIN2 = 9
            AIN1 = 10
            PWMB = 13
            BIN2 = 12
            BIN1 = 11

        } else if (motorNumber == PwmVals.STEPPER_TWO) {
            PWMA = 2
            AIN2 = 3
            AIN1 = 4
            PWMB = 7
            BIN2 = 6
            BIN1 = 5
        } else {

            throw (Exception("MotorHAT Stepper must be STEPPER_ONE or  STEPPER_TWO"))
        }
    }
}