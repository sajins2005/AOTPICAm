package com.example.sajin.aot_cam

import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.util.Log
import android.util.Range
import android.view.Surface
import android.view.SurfaceHolder

public class CameraLib
private constructor() {

    private var mCameraDevice: CameraDevice? = null
    private var mCaptureSession: CameraCaptureSession? = null
    private var bcHandler: Handler? = null
    private var mImageReader: ImageReader? = null
    private lateinit var fps: Array<out Range<Int>>
    private val mStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            Log.d(TAG, "Opened camera.")
            mCameraDevice = cameraDevice
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            Log.d(TAG, "Camera disconnected, closing.")
            cameraDevice.close()
        }

        override fun onError(cameraDevice: CameraDevice, i: Int) {
            Log.d(TAG, "Camera device error, closing.")
            cameraDevice.close()
        }

        override fun onClosed(cameraDevice: CameraDevice) {
            Log.d(TAG, "Closed camera, releasing")
            mCameraDevice = null
        }
    }

    /**
     * Callback handling session state changes
     */
    private val mSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
            // The camera is already closed
            if (mCameraDevice == null) {
                return
            }
            // When the session is ready, we start capture.
            mCaptureSession = cameraCaptureSession

            triggerImageCapture()
        }

        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
            Log.e(TAG, "Failed to configure camera")
        }
    }

    /**
     * Callback handling capture session events
     */
    private val mCaptureCallback = object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureProgressed(session: CameraCaptureSession, request: CaptureRequest, partialResult: CaptureResult) {
            Log.d(TAG, "Partial result")
        }

        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
          //  session.close()
            //mCaptureSession = null
            Log.d(TAG, "CaptureSession closed")
        }
    }

    public object InstanceHolder {
        var mCamera = CameraLib()
    }

    lateinit var ss: Surface
    /**
     * Initialize the camera device
     */
    fun initializeCamera(context: Context, backgroundHandler: Handler, imageAvailableListener: ImageReader.OnImageAvailableListener, surfaceT: SurfaceHolder) {
        ss = surfaceT.surface
        val manager = context.getSystemService(CAMERA_SERVICE) as CameraManager
        var camIds = arrayOf<String>()
        try {
            camIds = manager.cameraIdList
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Cam access exception getting IDs", e)
        }

        if (camIds.size < 1) {
            Log.e(TAG, "No cameras found")
            return
        }
        val id = camIds[0]
        val characteristics = manager.getCameraCharacteristics(id)
        fps = characteristics[CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES]

        Log.d(TAG, "Using camera id $id")

        // Initialize the image processor
        mImageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT,
                ImageFormat.JPEG, 5)
        mImageReader!!.setOnImageAvailableListener(
                imageAvailableListener, backgroundHandler)

        // Open the camera resource
        try {
            manager.openCamera(id, mStateCallback, backgroundHandler)
        } catch (cae: CameraAccessException) {
            Log.d(TAG, "Camera access exception", cae)
        }
    }

    fun takePicture() {
        if (mCameraDevice == null) {
            Log.e(TAG, "Cannot capture image. Camera not initialized.")
            return
        }
        try {


            mCameraDevice!!.createCaptureSession(
                    listOf<Surface>(mImageReader!!.surface),
                    mSessionCallback, bcHandler)
        } catch (cae: CameraAccessException) {
            Log.e(TAG, "access exception while preparing pic", cae)
        }
    }

    private fun triggerImageCapture() {
        try {
            val captureBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            captureBuilder.addTarget(mImageReader!!.surface)

            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            Log.d(TAG, "Session initialized.")
            var captureRequest = captureBuilder.build()
            mCaptureSession!!.setRepeatingRequest(captureRequest, mCaptureCallback, bcHandler)
            mCaptureSession!!.capture(captureBuilder.build(), mCaptureCallback, null)
        } catch (cae: CameraAccessException) {
            Log.e(TAG, "camera capture exception", cae)
        }
    }

    fun shutDown() {
        if (mCameraDevice != null) {
            mCameraDevice!!.close()
        }
    }

    companion object {
        private val TAG = CameraLib::class.java.simpleName

        private val IMAGE_WIDTH = 1024
        private val IMAGE_HEIGHT = 768
        private val MAX_IMAGES = 5

        val instance: CameraLib
            get() = InstanceHolder.mCamera
        fun dumpFormatInfo(context: Context) {
            val manager = context.getSystemService(CAMERA_SERVICE) as CameraManager
            var camIds = arrayOf<String>()
            try {
                camIds = manager.cameraIdList
            } catch (e: CameraAccessException) {
                Log.d(TAG, "Cam access exception getting IDs")
            }

            if (camIds.size < 1) {
                Log.d(TAG, "No cameras found")
            }
            val id = camIds[0]
            Log.d(TAG, "Using camera id $id")
            try {
                val characteristics = manager.getCameraCharacteristics(id)
                val configs = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                for (format in configs!!.outputFormats) {
                    Log.d(TAG, "Getting sizes for format: $format")
                    for (s in configs.getOutputSizes(format)) {
                        Log.d(TAG, "\t" + s.toString())
                    }
                }
                val effects = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS)
                for (effect in effects!!) {
                    Log.d(TAG, "Effect available: $effect")
                }
            } catch (e: CameraAccessException) {
                Log.d(TAG, "Cam access exception getting characteristics.")
            }
        }
    }
}