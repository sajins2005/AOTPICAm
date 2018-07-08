package com.example.sajin.aot_cam
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Display
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.widget.ImageView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.*
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File

import java.io.FileOutputStream
import java.io.IOException


class MainActivity : Activity(), LoaderCallbackInterface {

    private val TAG = MainActivity::class.java.simpleName
    private var imgview: ImageView? = null
    private var mCamera: DoorbellCamera? = null
    private var mCameraHandler: Handler? = null
    private var mCameraThread: HandlerThread? = null
    lateinit var pt: String
    private var mCloudHandler: Handler? = null
    lateinit var sh: SurfaceHolder
    private var mCloudThread: HandlerThread? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main);
        Log.d(TAG, "Doorbell Activity created.")

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // A problem occurred auto-granting the permission
            Log.e(TAG, "No permission")
            return
        }

        val mediaStorageDir = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraDemo")

        if (!mediaStorageDir.exists()) {
            !mediaStorageDir.mkdirs()
        }
        pt = mediaStorageDir.path + File.separator
        sh = surfaceView3.holder
        mCameraThread = HandlerThread("CameraBackground")
        mCameraThread!!.start()
        mCameraHandler = Handler(mCameraThread!!.looper)
        val button = button

        button.setOnClickListener({
            mCamera!!.takePicture()
        })

      //  sh.setFixedSize(480, 620)
        mCamera = DoorbellCamera.instance
        mCamera!!.initializeCamera(this, mCameraHandler!!, mOnImageAvailableListener, sh

        )


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


    private val mOnImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage()
        // get image bytes
        val imageBuf = image.planes[0].buffer
        val imageBytes = ByteArray(imageBuf.remaining())
        imageBuf.get(imageBytes)
        image.close()
        onPictureTaken(imageBytes)
    }

    private fun onPictureTaken(imageBytes: ByteArray?) {
        if (imageBytes != null) {
            runOnUiThread({

                val bMap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                val fos = FileOutputStream(pt+"image.jpg")
                try {
                    fos.write(imageBytes)
                } finally {
                    fos.close()
                }
           //     sh.setFixedSize()
                run(pt + "image.jpg", pt + "tt1.png", Imgproc.TM_CCOEFF_NORMED)
               // var canvs = sh.lockCanvas()


                //canvs.drawBitmap(bMap, Rect(0,0,480,620),Rect(0,0,382,512),null)

            })
        }
    }

    protected var mOpenCVCallBack: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    onOpenCVReady()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()

        Log.i("DEMO", "Trying to load OpenCV library")
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mOpenCVCallBack)) {
            Log.e("DEMO", "Cannot connect to OpenCV Manager")
        }
    }

    protected fun onOpenCVReady() {
        //this should crash if opencv is not loaded
        // val img = Mat()

        Toast.makeText(applicationContext, "opencv ready", Toast.LENGTH_LONG).show()

    }

    override fun onManagerConnected(status: Int) {
        Log.e("DEMO", "connected" + Int)

    }

    override fun onPackageInstall(operation: Int,
                                  callback: InstallCallbackInterface) {
        // TODO Auto-generated method stub

    }

    public fun run(src: String, templateFile: String,
                   match_method: Int) {
        println("\nRunning Template Matching")
        var img =Imgcodecs.imread(src)

        val templ = Imgcodecs.imread(templateFile)

        // / Create the result matrix
        val result_cols = img.cols() - templ.cols() + 1
        val result_rows = img.rows() - templ.rows() + 1
        val result = Mat(result_rows, result_cols, CvType.CV_32FC1)

        // / Do the Matching and Normalize
        Imgproc.matchTemplate(img, templ, result, match_method)

        Core.normalize(result, result, 0.0, 1.0, Core.NORM_MINMAX, -1, Mat())
        Imgproc.threshold(result, result, 0.8, 1.0, Imgproc.THRESH_TOZERO);

        var mmr = Core.minMaxLoc(result)
        var maxval = mmr.maxVal
        var matchLoc: Point
        if (match_method == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED) {
            matchLoc = mmr.minLoc
        } else {
            matchLoc = mmr.maxLoc
        }
        loop@ while (true) {
            Log.e("DEMO", "__________")
            mmr = Core.minMaxLoc(result)
            maxval = mmr.maxVal
            matchLoc = mmr.maxLoc
            // dst = img.clone();
            // / Show me what you got
            if (maxval > .85) {
                Imgproc.rectangle(img, matchLoc, Point(matchLoc.x + templ.cols(),
                        matchLoc.y + templ.rows()), Scalar(0.0, 255.0, 0.0))
                Imgproc.rectangle(result, matchLoc, Point(matchLoc.x + templ.cols(),
                        matchLoc.y + templ.rows()), Scalar(0.0, 255.0, 0.0), -1)
            } else {
                break@loop
            }

        }

        // Save the visualized detection.
        //println("Writing $outFile")
        Imgcodecs.imwrite(pt + "kk3.png", img)
        // val return_buff = ByteArray((img.total() * img.channels()) as Int)
        val bm = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(img, bm)
        val bMap = bm
        var canvs = sh.lockCanvas()


      canvs.drawBitmap(bMap, 0F, 0F, null)
        sh.unlockCanvasAndPost(canvs)

    }


}



/*
class MainActivity : Activity() , CameraBridgeViewBase.CvCameraViewListener2 {
    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {

                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")
                    mOpenCvCameraView?.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback)
    }

    private var mOpenCvCameraView: CameraBridgeViewBase? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "called onCreate")
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = findViewById(R.id.HelloOpenCvView) as CameraBridgeViewBase
        mOpenCvCameraView!!.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView!!.setCvCameraViewListener(this)
    }

    public override fun onPause() {
        super.onPause()
        if (mOpenCvCameraView != null)
            mOpenCvCameraView!!.disableView()
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (mOpenCvCameraView != null)
            mOpenCvCameraView!!.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {}

    override fun onCameraViewStopped() {}

    override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat {
        return inputFrame.rgba()
    }
}
    /*

     */
        */
