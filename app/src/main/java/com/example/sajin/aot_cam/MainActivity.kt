package com.example.sajin.aot_cam

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.view.*
import android.view.TextureView.SurfaceTextureListener
import kotlinx.android.synthetic.main.activity_main.view.*


/**
 * Skeleton of an Android Things activity.
 *
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * val service = PeripheralManagerService()
 * val mLedGpio = service.openGpio("BCM6")
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
 * mLedGpio.value = true
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 *
 */
class MainActivity : Activity() {

    private val TAG = MainActivity::class.java.simpleName
  private  var imgview:ImageView? =null

    private var mCamera: DoorbellCamera? = null

    /**
     * Driver for the doorbell button;
     */
    //private var mButtonInputDriver: ButtonInputDriver? = null

    /**
     * A [Handler] for running Camera tasks in the background.
     */
    private var mCameraHandler: Handler? = null

    /**
     * An additional thread for running Camera tasks that shouldn't block the UI.
     */
    private var mCameraThread: HandlerThread? = null

    /**
     * A [Handler] for running Cloud tasks in the background.
     */
    private var mCloudHandler: Handler? = null
lateinit var sh:SurfaceHolder
    /**
     * An additional thread for running Cloud tasks that shouldn't block the UI.
     */
    private var mCloudThread: HandlerThread? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main);
        Log.d(TAG, "Doorbell Activity created.")

        // We need permission to access the camera
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // A problem occurred auto-granting the permission
            Log.e(TAG, "No permission")
            return
        }


        // Creates new handlers and associated threads for camera and networking operations.
        mCameraThread = HandlerThread("CameraBackground")
        mCameraThread!!.start()
        mCameraHandler = Handler(mCameraThread!!.looper)

       // mCloudThread = HandlerThread("CloudThread")
       // mCloudThread!!.start()
      //  mCloudHandler = Handler(mCloudThread!!.looper)
        val button =button2

        // Initialize the doorbell button driver
        button.setOnClickListener( {
            mCamera!!.takePicture()

        })

//surfaceView3
        sh  =surfaceView3.holder
//sh.setSizeFromLayout()
        sh.setFixedSize(40,40)
        // Camera code is complicated, so we've shoved it all in this closet class for you.
        mCamera = DoorbellCamera.instance
      mCamera!!.initializeCamera(this, mCameraHandler!!, mOnImageAvailableListener,    sh

      )
//sh.setSizeFromLayout()
       // surfaceView3.


    }

    private fun initPIO() {


    }

    override fun onDestroy() {
        super.onDestroy()
        mCamera!!.shutDown()

        mCameraThread!!.quitSafely()
        mCloudThread!!.quitSafely()
        try {
          //  mButtonInputDriver!!.close()
        } catch (e: IOException) {
            Log.e(TAG, "button driver error", e)
        }

    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            // Doorbell rang!
            Log.d(TAG, "button pressed")
            mCamera!!.takePicture()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }



    /**
     * Listener for new camera images.
     */
    private val mOnImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage()
        // get image bytes
        val imageBuf = image.planes[0].buffer
        val imageBytes = ByteArray(imageBuf.remaining())
        imageBuf.get(imageBytes)

       image.close()

     //  onPictureTaken(imageBytes)

    }

    /**
     * Upload image data to Firebase as a doorbell event.
     */
    private fun onPictureTaken(imageBytes: ByteArray?) {
        if (imageBytes != null) {
            runOnUiThread( {

                     val bMap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                var canvs=   sh.lockCanvas()


                canvs.drawBitmap(bMap, 0F,0F,null)
surfaceView3.holder.unlockCanvasAndPost(canvs)
            })

            // upload image to storage


        }
    }

    /**
     * Process image contents with Cloud Vision.
     */
}
