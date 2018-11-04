package com.example.sajin.aot_cam

import android.util.Log
import com.example.sajin.aot_cam.Constants.PwmVals
import com.example.sajin.aot_cam.Constants.STepperPin
import com.example.sajin.aot_cam.Constants.StepperDirection
import com.example.sajin.aot_cam.Constants.StepperStyle
import java.util.concurrent.TimeUnit

class StepperMotorController(var controller:PwmController , var num: PwmVals ,var steps:Int=200){

    val MC =controller
    val motornum=num
    val revsteps=steps
    var sec_per_step = 0.1
    var  steppingcounter = 0
    var currentstep = 0
    var stepperMotorPinConfig:STepperPin

    val  MICROSTEPS=8
    val MICROSTEP_CURVE = arrayOf(0, 50, 98, 142, 180, 212, 236, 250, 255)
    var v= false

//val MICROSTEPS = 16
//val MICROSTEP_CURVE =arrayOf (0, 25, 50, 74, 98, 120, 141, 162, 180, 197, 212, 225, 236, 244, 250, 253, 255)

    init{
        stepperMotorPinConfig= STepperPin(num)


    }
    fun setSpeed( rpm:  Int) {
         sec_per_step = 60.0 / (revsteps * rpm)
         steppingcounter = 0
    }
    fun oneStep(dir: StepperDirection , style:StepperStyle):Int {
        var start = System.currentTimeMillis()
        var pwm_a = 225
        var pwm_b = 255
        //     MICROSTEPS =4
// first determine what sort of stepping procedure we're up to
        if (style == StepperStyle.SINGLE) {

            var m = currentstep/(MICROSTEPS / 2) % 2

            if (m == 1) {
// we're at an odd step, weird
                if (dir == StepperDirection.FORWARD) {
                    currentstep += MICROSTEPS / 2

                } else {
                    currentstep -= MICROSTEPS / 2
                }

            } else
// go to next even step
                if (dir == StepperDirection.FORWARD) {
                    currentstep += MICROSTEPS
                } else {
                    currentstep -= MICROSTEPS
                }
        }
        if (style == StepperStyle.DOUBLE) {

            if ((currentstep / (MICROSTEPS / 2) % 2) == 1) {
// we're at an even step, weird
                if (dir == StepperDirection.FORWARD) {
                    currentstep += MICROSTEPS / 2
                } else {
                    currentstep -= MICROSTEPS / 2
                }
            } else  if (dir == StepperDirection.FORWARD) {
                currentstep += MICROSTEPS
            } else {
                currentstep -= MICROSTEPS
            }
        }
        if (style == StepperStyle.INTERLEAVE) {
            if (dir == StepperDirection.FORWARD) {
                currentstep += MICROSTEPS / 2
            } else {
                currentstep -= MICROSTEPS / 2
            }
        }
        if (style == StepperStyle.MICROSTEP) {

            if (dir == StepperDirection.FORWARD) {
                currentstep += 1
            } else {
                currentstep -= 1
            }

// go to next 'step' and wrap around
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
    }
// go to next 'step' and wrap around
        currentstep += MICROSTEPS * 4
        currentstep %= MICROSTEPS * 4

// only really used for microstepping, otherwise always on!
        MC.setPwm(stepperMotorPinConfig.PWMA, 0, pwm_a * 16)
        MC.setPwm(stepperMotorPinConfig.PWMB, 0, pwm_b * 16)

// set up coil energizing!
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


        }else {
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

//print "coils state = " + str(coils)



        MC.setPin(stepperMotorPinConfig.AIN2, coils[0])
        MC.setPin(stepperMotorPinConfig.BIN1, coils[1])
        MC.setPin(stepperMotorPinConfig.AIN1, coils[2])
        MC.setPin(stepperMotorPinConfig.BIN2, coils[3])
       /* MC.setPin(AIN2, false)

        MC.setPin(AIN1,false)
        MC.setPin(BIN1, false)
        MC.setPin(BIN2,false)
v=v.not()
*/
        val b=   System.currentTimeMillis() - start
        Log.i("MOtor",b.toString()+"=====================")
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
        print("{} sec per step".format(s_per_s))
        var s = stepsvar

        while (s >= 0) {
            var start = System.currentTimeMillis()
            lateststep = oneStep(direction, stepstyle)
            val offset=   System.currentTimeMillis() - start
            TimeUnit.NANOSECONDS.sleep((s_per_s* 1000000000).toLong()-offset*1000000)
            s = s.minus(1)
            val b=   System.currentTimeMillis() - start
            Log.i("step",b.toString()+"========++=============")

        }
            if (stepstyle == StepperStyle.MICROSTEP) {
// this is an edge case, if we are in between full steps, lets just keep going
// so we end on a full step
                while ((lateststep != 0) && (lateststep != MICROSTEPS)) {

                    lateststep = oneStep(direction, stepstyle)
                    TimeUnit.NANOSECONDS.sleep((s_per_s* 1000000000).toLong())

                }
            }



    }
fun reset(){
    MC.setPin(stepperMotorPinConfig.AIN2, false)
    MC.setPin(stepperMotorPinConfig.BIN1, false)
    MC.setPin(stepperMotorPinConfig.AIN1, false)
    MC.setPin(stepperMotorPinConfig.BIN2, false)


}

}