package com.example.sajin.aot_cam

import com.example.sajin.aot_cam.Constants.PwmRegVal
import com.example.sajin.aot_cam.Constants.PwmRegistry
import com.example.sajin.aot_cam.Constants.PwmVals
import com.google.android.things.pio.I2cDevice
import com.google.android.things.pio.PeripheralManager
import java.io.IOException
import java.util.concurrent.TimeUnit


class PwmController(i2cBusName: String, i2cAddress: Int = PwmVals.DefualtI2cAddress.vals) {
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
        mI2cDevice.writeRegByte(PwmRegistry.ALL_LED_ON_L.address, 0.toByte())
        mI2cDevice.writeRegByte(PwmRegistry.ALL_LED_ON_H.address, 0.toByte())
        mI2cDevice.writeRegByte(PwmRegistry.ALL_LED_OFF_L.address, 0.toByte())
        mI2cDevice.writeRegByte(PwmRegistry.ALL_LED_OFF_H.address, 0.toByte())

        mI2cDevice.writeRegByte(PwmRegistry.MODE2.address, PwmRegVal.OUTDRV.regval.toByte())
        mI2cDevice.writeRegByte(PwmRegistry.MODE1.address, PwmRegVal.ALLCALL.regval.toByte())
        TimeUnit.MICROSECONDS.sleep(500)

        var oldmode1 = mI2cDevice.readRegWord(PwmRegistry.MODE1.address).toInt()
        var mode1 = oldmode1 and PwmRegVal.SLEEP.regval.inv()
        mI2cDevice.writeRegByte(PwmRegistry.MODE1.address, mode1.toByte())
        TimeUnit.MICROSECONDS.sleep(500)


        oldmode1 = mI2cDevice.readRegWord(PwmRegistry.MODE1.address).toInt()
        // Remove the restart and sleep bits.
        mode1 = (oldmode1 and 0x7f) or PwmRegVal.SLEEP.regval
        // Sleep while we set the prescale value.
        mI2cDevice.writeRegByte(PwmRegistry.MODE1.address, mode1.toByte())
        val prescaleval = (25000000f / (4096f * 1500f) - 1).toInt()
        val prescale = (prescaleval).toByte()
        mI2cDevice.writeRegByte(PwmRegistry.PRESCALE.address, prescale)
        TimeUnit.MICROSECONDS.sleep(500)
        // Restart: clear the sleep bit, wait for the oscillator to stabilize, set the restart bit.
        // https://cdn-shop.adafruit.com/datasheets/PCA9685.pdf (page 15)
        mI2cDevice.writeRegByte(PwmRegistry.MODE1.address, (oldmode1).toByte())
        try {
            TimeUnit.MICROSECONDS.sleep(500)
        } catch (ignored: InterruptedException) {
        }

        mI2cDevice.writeRegByte(PwmRegistry.MODE1.address, (oldmode1 or PwmRegVal.RESTART.regval).toByte())

    }

    @Throws(IOException::class)
    fun setPin(pin: Int, value: Boolean) {
        setPwm(pin, if (value) PwmRegVal.DC_PIN_HIGH.regval else PwmRegVal.DC_PIN_LOW.regval, if (value) PwmRegVal.DC_PIN_LOW.regval else PwmRegVal.DC_PIN_HIGH.regval)
    }

    @Throws(IOException::class)
    fun setPwm(channel: Int, on: Int, off: Int) {
        val offset = 4 * channel
        mI2cDevice.writeRegByte(PwmRegistry.LED_0_ON_L.address + offset, (on and 0xFF).toByte())
        mI2cDevice.writeRegByte(PwmRegistry.LED_0_ON_H.address + offset, (on shr 8).toByte())
        mI2cDevice.writeRegByte(PwmRegistry.LED_0_OFF_L.address + offset, (off and 0xFF).toByte())
        mI2cDevice.writeRegByte(PwmRegistry.LED_0_OFF_H.address + offset, (off shr 8).toByte())
    }


}