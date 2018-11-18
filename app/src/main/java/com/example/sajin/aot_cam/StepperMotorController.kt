package com.example.sajin.aot_cam

import android.util.Log
import com.example.sajin.aot_cam.Constants.PwmVals
import com.example.sajin.aot_cam.Constants.STepperPin
import com.example.sajin.aot_cam.Constants.StepperDirection
import com.example.sajin.aot_cam.Constants.StepperStyle
import java.util.concurrent.TimeUnit

class StepperMotorController(var controller: PwmController, var num: PwmVals, var steps: Int = 200) {
    val MC = controller
    val motornum = num
    val revsteps = steps
    var sec_per_step = 0.1
    var steppingcounter = 0
    var currentstep = 0
    var stepperMotorPinConfig: STepperPin
    val MICROSTEPS = 8
    val MICROSTEP_CURVE = arrayOf(0, 50, 98, 142, 180, 212, 236, 250, 255)
    var coil0 = false
    var coil1 = false
    var coil2 = false
    var coil3 = false
    var pwm_a = 255
    var pwm_b = 255

    init {
        stepperMotorPinConfig = STepperPin(num)
        MC.setPwm(stepperMotorPinConfig.PWMA, 0, pwm_a * 16)
        MC.setPwm(stepperMotorPinConfig.PWMB, 0, pwm_b * 16)

    }

    fun setSpeed(rpm: Int) {
        sec_per_step = 60.0 / (revsteps * rpm)
        steppingcounter = 0
    }

    fun oneStep(dir: StepperDirection, style: StepperStyle): Int {
        var start = System.currentTimeMillis()
        when (style) {
            StepperStyle.SINGLE -> {
                var m = currentstep / (MICROSTEPS / 2) % 2
                if (m == 0) {
                    setDirection(dir, MICROSTEPS / 2)
                } else {
                    setDirection(dir, MICROSTEPS)
                }
            }
            StepperStyle.DOUBLE -> {
                if ((currentstep / (MICROSTEPS / 2) % 2) == 0) {
                    setDirection(dir, MICROSTEPS / 2)
                } else {
                    setDirection(dir, MICROSTEPS)
                }
            }
            StepperStyle.INTERLEAVE -> {
                setDirection(dir, MICROSTEPS / 2)
            }
            StepperStyle.MICROSTEP -> {
                setDirection(dir, 1)
                currentstep += MICROSTEPS * 4
                currentstep %= MICROSTEPS * 4
                pwm_a = 0
                pwm_b = 0
                if ((currentstep >= 0) && (currentstep < MICROSTEPS)) {
                    pwm_a = MICROSTEP_CURVE[MICROSTEPS - currentstep]
                    pwm_b = MICROSTEP_CURVE[currentstep]
                } else if ((currentstep >= MICROSTEPS) && (currentstep < MICROSTEPS * 2)) {
                    pwm_a = MICROSTEP_CURVE[currentstep - MICROSTEPS]
                    pwm_b = MICROSTEP_CURVE[MICROSTEPS * 2 - currentstep]
                } else if ((currentstep >= MICROSTEPS * 2) && (currentstep < MICROSTEPS * 3)) {
                    pwm_a = MICROSTEP_CURVE[MICROSTEPS * 3 - currentstep]
                    pwm_b = MICROSTEP_CURVE[currentstep - MICROSTEPS * 2]
                } else if ((currentstep >= MICROSTEPS * 3) && (currentstep < MICROSTEPS * 4)) {
                    pwm_a = MICROSTEP_CURVE[currentstep - MICROSTEPS * 3]
                    pwm_b = MICROSTEP_CURVE[MICROSTEPS * 4 - currentstep]
                }
                MC.setPwm(stepperMotorPinConfig.PWMA, 0, pwm_a * 16)
                MC.setPwm(stepperMotorPinConfig.PWMB, 0, pwm_b * 16)

            }
        }

// go to next 'step' and wrap around
        currentstep += MICROSTEPS * 4
        currentstep %= MICROSTEPS * 4
        var coils = arrayOf(false, false, false, false)

        if (style == StepperStyle.MICROSTEP) {
            if ((currentstep >= 0) && (currentstep < MICROSTEPS)) {
                coils = arrayOf(true, true, false, false)
            } else if ((currentstep >= MICROSTEPS) && (currentstep < MICROSTEPS * 2)) {
                coils = arrayOf(false, true, true, false)
            } else if ((currentstep >= MICROSTEPS * 2) && (currentstep < MICROSTEPS * 3)) {
                coils = arrayOf(false, false, true, true)
            } else if ((currentstep >= MICROSTEPS * 3) && (currentstep < MICROSTEPS * 4)) {
                coils = arrayOf(true, false, false, true)
            }
        } else {
            var step2coils = arrayOf(
                    arrayOf(true, false, false, false),
                    arrayOf(true, true, false, false),
                    arrayOf(false, true, false, false),
                    arrayOf(false, true, true, false),
                    arrayOf(false, false, true, false),
                    arrayOf(false, false, true, true),
                    arrayOf(false, false, false, true),
                    arrayOf(true, false, false, true))
            coils = step2coils[currentstep / (MICROSTEPS / 2)]
        }
        if (coils[0].xor(coil0))
            MC.setPin(stepperMotorPinConfig.AIN2, coils[0])
        if (coils[1].xor( coil1))
            MC.setPin(stepperMotorPinConfig.BIN1, coils[1])
        if (coils[2].xor(coil2))
            MC.setPin(stepperMotorPinConfig.AIN1, coils[2])
        if (coils[3].xor( coil3))
            MC.setPin(stepperMotorPinConfig.BIN2, coils[3])

        val b = System.currentTimeMillis() - start
        Log.i("MOtor", b.toString() + "=====================")
        coil0 = coils[0]
        coil1 = coils[1]
        coil2 = coils[2]
        coil3 = coils[3]
        return currentstep
    }

    fun step(steps: Int, direction: StepperDirection, stepstyle: StepperStyle) {
        var stepsvar = steps

        var s_per_s = sec_per_step
        var lateststep = 0

        if (stepstyle == StepperStyle.INTERLEAVE) {
            s_per_s = s_per_s / 2.0
        }
        if (stepstyle == StepperStyle.MICROSTEP) {
            s_per_s /= MICROSTEPS
            stepsvar *= MICROSTEPS
        }
//        print("{} sec per step".format(s_per_s))
        var s = stepsvar

        while (s >= 0) {
            var start = System.currentTimeMillis()
            lateststep = oneStep(direction, stepstyle)
            val offset = System.currentTimeMillis() - start
            //        TimeUnit.NANOSECONDS.sleep((s_per_s* 1000000000).toLong()-offset*1000000)
            s = s.minus(1)
            val b = System.currentTimeMillis() - start
            Log.i("step", b.toString() + "========++=============")
        }
        if (stepstyle == StepperStyle.MICROSTEP) {
            while ((lateststep != 0) && (lateststep != MICROSTEPS)) {
                lateststep = oneStep(direction, stepstyle)
                TimeUnit.NANOSECONDS.sleep((s_per_s * 1000000000).toLong())
            }
        }
    }

    private fun setDirection(dir: StepperDirection, microsteps: Int) {
        if (dir == StepperDirection.FORWARD) {
            currentstep += microsteps
        } else {
            currentstep -= microsteps
        }
    }

    fun reset() {
        MC.setPin(stepperMotorPinConfig.AIN2, false)
        MC.setPin(stepperMotorPinConfig.BIN1, false)
        MC.setPin(stepperMotorPinConfig.AIN1, false)
        MC.setPin(stepperMotorPinConfig.BIN2, false)
    }

}