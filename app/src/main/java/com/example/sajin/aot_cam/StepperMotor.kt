package com.example.sajin.aot_cam

class StepperMotor(var controller: StepperControl, var num: Int ,var steps:Int=200){

    val MC =controller
    val motornum=num
    val revsteps=steps
    val sec_per_step = 0.1
    val  steppingcounter = 0
    var currentstep = 0

    var PWMA:Int
    var AIN2:Int
   var AIN1:Int
    var PWMB :Int
     var BIN2 :Int
    var BIN1 :Int
    var  MICROSTEPS=8
    val MICROSTEP_CURVE = arrayOf(0, 50, 98, 142, 180, 212, 236, 250, 255)
    var v= false

////MICROSTEPS = 16
//// a sinusoidal curve NOT LINEAR!
////MICROSTEP_CURVE = [0, 25, 50, 74, 98, 120, 141, 162, 180, 197, 212, 225, 236, 244, 250, 253, 255]

    init{

        num = num- 1

        if (num == 0) {
             PWMA = 8
             AIN2 = 9
             AIN1 = 10
             PWMB = 13
             BIN2 = 12
             BIN1 = 11

        }else if(num == 1){
             PWMA = 2
             AIN2 = 3
             AIN1 = 4
             PWMB = 7
             BIN2 = 6
             BIN1 = 5
        }else {

            throw ( Exception("MotorHAT Stepper must be between 1 and 2 inclusive"))
        }
    }
    fun setSpeed( rpm:  Int) {
        var sec_per_step = 60.0 / (revsteps * rpm)
        var steppingcounter = 0
    }
    fun oneStep(dir: String , style:String):Int {
        var pwm_a=225
        var  pwm_b = 255
        var  MICROSTEPS: Int =4
// first determine what sort of stepping procedure we're up to
        if (style == "SINGLE") {

            var m = currentstep.div(MICROSTEPS / 2) % 2

            if (m == 1) {
// we're at an odd step, weird
                if (dir == "FORWARD") {
                    currentstep += MICROSTEPS / 2


                } else {
                    currentstep -= MICROSTEPS / 2
                }

            } else
// go to next even step
                if (dir == "FORWARD") {
                    currentstep += MICROSTEPS
                } else {
                    currentstep -= MICROSTEPS
                }
        }
        if (style == "DOUBLE") {

            if ((currentstep / (MICROSTEPS / 2) % 2) == 0) {
// we're at an even step, weird
                if (dir == "FORWARD") {
                    currentstep += MICROSTEPS / 2
                } else {
                    currentstep -= MICROSTEPS / 2
                }
            } else if (dir == "FORWARD") {
                currentstep += MICROSTEPS
            } else {
                currentstep -= MICROSTEPS
            }
        }
        if (style == "INTERLEAVE") {
            if (dir == "FORWARD") {
                currentstep += MICROSTEPS / 2
            } else {
                currentstep -= MICROSTEPS/2
            }
        }
        if (style == "MICROSTEP") {

            if (dir == "FORWARD") {
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
        MC.setPwm(PWMA, 0, pwm_a * 16)
        MC.setPwm(PWMB, 0, pwm_b * 16)

// set up coil energizing!
        var coils = arrayOf(false, false, false, false)

        if (style == "MICROSTEP") {
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

//print "coils state = " + str(coils)



        MC.setPin(AIN2, coils[0])
        MC.setPin(BIN1, coils[1])
        MC.setPin(AIN1, coils[2])
        MC.setPin(BIN2, coils[3])
       /* MC.setPin(AIN2, false)

        MC.setPin(AIN1,false)
        MC.setPin(BIN1, false)
        MC.setPin(BIN2,false)
v=v.not()
*/
        return currentstep

    }

    fun step(steps: Int, direction: String, stepstyle: String) {
        var stepsvar = steps

        var s_per_s = sec_per_step
        var lateststep = 0

        if (stepstyle == "INTERLEAVE") {
            s_per_s = s_per_s / 2.0
        }
        if (stepstyle == "MICROSTEP") {
            s_per_s /= MICROSTEPS
        }
        stepsvar = stepsvar * MICROSTEPS

        print("{} sec per step".format(s_per_s))
        var s = steps
        while (s >= 0) {
            lateststep = oneStep(direction, stepstyle)
            Thread.sleep(s_per_s.toLong())
            s = s.minus(1)
            if (stepstyle == "SINGLE") {
// this is an edge case, if we are in between full steps, lets just keep going
// so we end on a full step
                while ((lateststep != 0) && (lateststep != MICROSTEPS)) {
                    lateststep = oneStep(direction, stepstyle)
                    Thread.sleep(s_per_s.toLong())
                }
            }

        }
        MC.setPin(AIN2, false)
        MC.setPin(BIN1, false)
        MC.setPin(AIN1, false)
        MC.setPin(BIN2, false)
    }


}