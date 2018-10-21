package com.example.sajin.aot_cam
import com.google.android.things.pio.I2cDevice
import com.google.android.things.pio.PeripheralManager
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
//import com.google.android.things.contrib.driver.motorhat.MotorHat.DEFAULT_I2C_ADDRESS
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.google.android.things.contrib.driver.motorhat.MotorHat.MotorState




class StepperControl(i2cBusName:String, i2cAddress:Int=0x60) {
   // # Registers/etc.
   val __MODE1              = 0x00
    val __MODE2              = 0x01
    val __SUBADR1            = 0x02
   val __SUBADR2            = 0x03
    val __SUBADR3            = 0x04
   val __PRESCALE           = 0xFE
   val __LED0_ON_L          = 0x06
   val __LED0_ON_H          = 0x07
    val __LED0_OFF_L         = 0x08
    val __LED0_OFF_H         = 0x09
    val __ALL_LED_ON_L       = 0xFA
    val __ALL_LED_ON_H       = 0xFB
    val __ALL_LED_OFF_L      = 0xFC
    val __ALL_LED_OFF_H      = 0xFD

   // # Bits
    val  __RESTART            = 0x80
    val __SLEEP              = 0x10
    val __ALLCALL            = 0x01
    val __INVRT              = 0x10
    val __OUTDRV             = 0x04
    private val REG_MODE_1 = 0x00
    private val REG_MODE_2 = 0x01
    private val REG_PRESCALE = 0xFE
    private val REG_LED_0_ON_L = 0x06
    private val REG_LED_0_ON_H = 0x07
    private val REG_LED_0_OFF_L = 0x08
    private val REG_LED_0_OFF_H = 0x09
    private val REG_ALL_LED_ON_L = 0xFA
    private val REG_ALL_LED_ON_H = 0xFB
    private val REG_ALL_LED_OFF_L = 0xFC
    private val REG_ALL_LED_OFF_H = 0xFD

    private val ALLCALL: Byte = 0x01
    private val OUTDRV: Byte = 0x04
    private val SLEEP: Byte = 0x10
    private val RESTART = 0x80.toByte()

    private val DC_PIN_LOW = 0
    private val DC_PIN_HIGH = 4096

    private val MIN_SPEED = 0
    private val MAX_SPEED = 255
private lateinit var mI2cDevice: I2cDevice




     init {
        val pioService = PeripheralManager.getInstance()
        val device = pioService.openI2cDevice(i2cBusName, i2cAddress)
        try {
            initialize(device)
        } catch (e: IOException) {
            try {
  //              close()
            } catch (ignored: IOException) {
            } catch (ignored: RuntimeException) {
            }

            throw e
        } catch (e: RuntimeException) {
            try {
    //            close()
            } catch (ignored: IOException) {
            } catch (ignored: RuntimeException) {
            }

            throw e
        }


    }

    @Throws(IOException::class)
    private fun initialize(device: I2cDevice) {
        mI2cDevice = device

        // reset
        mI2cDevice.writeRegByte(REG_ALL_LED_ON_L, 0.toByte())
        mI2cDevice.writeRegByte(REG_ALL_LED_ON_H, 0.toByte())
        mI2cDevice.writeRegByte(REG_ALL_LED_OFF_L, 0.toByte())
        mI2cDevice.writeRegByte(REG_ALL_LED_OFF_H, 0.toByte())

        mI2cDevice.writeRegByte(REG_MODE_2, OUTDRV)
        mI2cDevice.writeRegByte(REG_MODE_1, ALLCALL)

        var mode1 = mI2cDevice.readRegByte(REG_MODE_1)
        // Remove the restart and sleep bits.
        mode1 = mode1 and (RESTART or SLEEP).inv()
        // Sleep while we set the prescale value.
        mI2cDevice.writeRegByte(REG_MODE_1, (mode1 or SLEEP) as Byte)
        val prescaleval = ((25000000f // 25MHz

                / 4096f // 12-bit

                / 1600f) // motor frequency
                - 1.0) // pineapple
        val prescale = (prescaleval + 0.5f).toByte()
        mI2cDevice.writeRegByte(REG_PRESCALE, prescale)

        // Restart: clear the sleep bit, wait for the oscillator to stabilize, set the restart bit.
        // https://cdn-shop.adafruit.com/datasheets/PCA9685.pdf (page 15)
        mI2cDevice.writeRegByte(REG_MODE_1, (mode1 and SLEEP.inv()) as Byte)
        try {
            TimeUnit.MICROSECONDS.sleep(500)
        } catch (ignored: InterruptedException) {
        }

       // mI2cDevice.writeRegByte(REG_MODE_1, (mode1 or RESTART) as Byte)

        /*  mMotors = arrayOfNulls<DcMotor>(MAX_DC_MOTORS)
        mMotors[0] = DcMotor(8, 9, 10)
        mMotors[1] = DcMotor(13, 12, 11)
        mMotors[2] = DcMotor(2, 3, 4)
        mMotors[3] = DcMotor(7, 6, 5)
    }
*/
    }

    @Throws(IOException::class)
     fun setPin(pin: Int, value: Boolean) {
        setPwm(pin, if (value) DC_PIN_HIGH else DC_PIN_LOW, if (value) DC_PIN_LOW else DC_PIN_HIGH)
    }

    @Throws(IOException::class)
     fun setPwm(channel: Int, on: Int, off: Int) {
        val offset = 4 * channel
        mI2cDevice.writeRegByte(REG_LED_0_ON_L + offset, (on and 0xFF).toByte())
        mI2cDevice.writeRegByte(REG_LED_0_ON_H + offset, (on shr 8).toByte())
        mI2cDevice.writeRegByte(REG_LED_0_OFF_L + offset, (off and 0xFF).toByte())
        mI2cDevice.writeRegByte(REG_LED_0_OFF_H + offset, (off shr 8).toByte())
    }

    @Throws(IOException::class)
    fun setMotorSpeed(motor: Int, speed: Int) {
        if (mI2cDevice == null) {
            throw IllegalStateException("I2C device not open")
        }
       // mMotors[motor].setSpeed(speed)
    }

    @Throws(IOException::class)
    fun setMotorState(motor: Int, @MotorState state: Int) {
        if (mI2cDevice == null) {
            throw IllegalStateException("I2C device not open")
        }
       // mMotors[motor].setState(state)
    }
}