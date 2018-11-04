package com.example.sajin.aot_cam

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ImageReader.OnImageAvailableListener
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.widget.ImageView
import android.widget.Toast
import com.example.sajin.aot_cam.Constants.PwmVals
import com.example.sajin.aot_cam.Constants.StepperDirection
import com.example.sajin.aot_cam.Constants.StepperStyle
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.*
import org.opencv.core.*
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.ORB
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.IOException


class MainActivity : Activity(), LoaderCallbackInterface {

    private val TAG = MainActivity::class.java.simpleName
    private var imgview: ImageView? = null
    private var mCamera: CameraLib? = null
    private var mCameraHandler: Handler? = null
    private var mCameraThread: HandlerThread? = null
    lateinit var pt: String
    private var mCloudHandler: Handler? = null
    lateinit var sh: SurfaceHolder
    private var mCloudThread: HandlerThread? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main);
        Log.d(TAG, "Activity created.")

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "No permission")
            return
        }
        var m = StepperMotorController(PwmController("I2C1"), PwmVals.STEPPER_ONE)
        m.setSpeed(20)
        button4.setOnClickListener {
            var start = System.currentTimeMillis()
            m.step(400, StepperDirection.FORWARD, StepperStyle.DOUBLE)
            val b = System.currentTimeMillis() - start
            Log.d(TAG, b.toString() + "=====================")
            m.reset()

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
        button.setOnClickListener {
            mCamera!!.takePicture()
        }
        mCamera = CameraLib.instance
        mCamera!!.initializeCamera(this, mCameraHandler!!, mOnImageAvailableListener, sh)
    }

    override fun onDestroy() {
        super.onDestroy()
        mCamera!!.shutDown()
        mCameraThread!!.quitSafely()
        try {
        } catch (e: IOException) {
            Log.e(TAG, "button driver error", e)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            Log.d(TAG, "button pressed")
            mCamera!!.takePicture()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private val mOnImageAvailableListener = OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage()
        val imageBuf = image.planes[0].buffer
        val imageBytes = ByteArray(imageBuf.remaining())
        imageBuf.get(imageBytes)
        var img1 = Mat(1, image.height * image.width, CvType.CV_8UC3, imageBuf)
        var img = Imgcodecs.imdecode(img1, Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        image.close()

        onPictureTaken(img)
    }

    private fun onPictureTaken(ima: Mat) {
        run(ima, pt + "tt.png", Imgproc.TM_CCOEFF_NORMED)
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
        Toast.makeText(applicationContext, "opencv ready", Toast.LENGTH_LONG).show()
    }

    override fun onManagerConnected(status: Int) {
        Log.e("DEMO", "connected" + Int)
    }

    override fun onPackageInstall(operation: Int,
                                  callback: InstallCallbackInterface) {
        // TODO Auto-generated method stub
    }

    public fun run(img: Mat, templateFile: String,
                   match_method: Int) {
        println("\nRunning Template Matching")
        var tt = Imgcodecs.imread(templateFile, Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE)

        //  var tt = Mat(templ.rows(),templ.cols(),CvType.CV_8U)
        // Imgproc.Canny(templ,tt,50.0,200.0)

        val result_cols = img.cols() - tt.cols() + 1
        val result_rows = img.rows() - tt.rows() + 1
        val result = Mat(result_rows, result_cols, CvType.CV_32FC1)

        Imgproc.matchTemplate(img, tt, result, match_method)
//core.normalize not required
        //  Core.normalize(result, result, 0.0, 1.0, Core.NORM_MINMAX, -1, Mat())
        Imgproc.threshold(result, result, 0.98, 1.0, Imgproc.THRESH_TOZERO);
        var maxval: Double
        var matchLoc: Point

        var detector = ORB.create()   // astFeatureDetector.create()

        var desc1 = Mat()
        var desc2 = Mat()
        var keypoint1 = MatOfKeyPoint()
        var keypoint2 = MatOfKeyPoint()
        var set1 = detector.detect(tt, keypoint1)
        var set2 = detector.detect(img, keypoint2)

        detector.compute(tt, keypoint1, desc1)

        detector.compute(img, keypoint2, desc2)
        var matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED)
        var matches = MatOfDMatch()
        matcher.match(desc1, desc2, matches)


        loop@ while (true) {
            Log.e("DEMO", "__________")
            var mmr = Core.minMaxLoc(result)
            maxval = mmr.maxVal
            if (match_method == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED) {
                matchLoc = mmr.minLoc
            } else {
                matchLoc = mmr.maxLoc
            }

            if (maxval > .90) {
                Imgproc.rectangle(img, matchLoc, Point(matchLoc.x + tt.cols(),
                        matchLoc.y + tt.rows()), Scalar(0.0, 255.0, 0.0))

                Imgproc.rectangle(result, matchLoc, Point(matchLoc.x + tt.cols(),
                        matchLoc.y + tt.rows()), Scalar(0.0, 255.0, 0.0), -1)
            } else {
                break@loop
            }
        }

        Imgcodecs.imwrite(pt + "kk3.png", img)
        Imgcodecs.imwrite(pt + "kk2.png", tt)
        val bm = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(img, bm)
        val bMap = bm
        runOnUiThread {
            var canvs = sh.lockCanvas()
            sh.setFixedSize(1280, 960)
            canvs.drawBitmap(bMap, 0F, 0F, null)
            sh.unlockCanvasAndPost(canvs)
        }
    }

}

