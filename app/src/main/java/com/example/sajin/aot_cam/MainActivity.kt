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
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.*
import org.opencv.core.*
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
        var stepcontroll = PwmController("I2C1")

        button4.setOnClickListener {
            var start = System.currentTimeMillis()
            Thread {
                var m1 = StepperMotorController(stepcontroll, PwmVals.STEPPER_TWO)
                m1.setSpeed(50)
                m1.step(8000, StepperDirection.BACKWARD, StepperStyle.DOUBLE)
                val b = System.currentTimeMillis() - start
                Log.d(TAG, b.toString() + "=====================")
                m1.reset()

            }.start()
            Thread {
                var m2 = StepperMotorController(stepcontroll, PwmVals.STEPPER_ONE)
                m2.setSpeed(50)
                m2.step(8000, StepperDirection.FORWARD, StepperStyle.DOUBLE)
                val b = System.currentTimeMillis() - start
                Log.d(TAG, b.toString() + "=====================")
                m2.reset()

            }.start()

            // stepperone!!.start()
            // var  stepperoneHandler = Handler(mCameraThread!!.looper)


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
        if (image!=null) {
            val imageBuf = image.planes[0].buffer
            val imageBytes = ByteArray(imageBuf.remaining())
            imageBuf.get(imageBytes)
            var img1 = Mat(1, image.height * image.width, CvType.CV_8UC3, imageBuf)
            var img = Imgcodecs.imdecode(img1, Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
            image.close()
            var imgob: Flowable<Mat> = Flowable.create<Mat>({
                it->
                it.onNext(img)

            },BackpressureStrategy.DROP)



                //emitter.onComplete()
            imgob.subscribeOn(Schedulers.io())
            onPictureTaken(imgob)
        }
    }

    private fun onPictureTaken(ima: Flowable<Mat>) {
        var ob= run(ima, pt + "tt.png", Imgproc.TM_CCOEFF_NORMED)
                 .observeOn(AndroidSchedulers.mainThread())
                 .subscribe({

                         var canvs = sh.lockCanvas()
                         sh.setFixedSize(1024, 768)
                         canvs.drawBitmap(it, 0F, 0F, null)
                         sh.unlockCanvasAndPost(canvs)

                     },{
                     Log.d("error",it.message)
                     throw it
                 })


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

    public fun run(img: Flowable<Mat>, templateFile: String,
                   match_method: Int): Flowable<Bitmap> {
        //   println("\nRunning Template Matching")
        var tt = Flowable.create<Mat>( {
          var tt=  Imgcodecs.imread(templateFile, Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE)
            it.onNext(tt)
        },BackpressureStrategy.BUFFER).subscribeOn(Schedulers.io())

        var obBmp= Flowable.zip( img,tt, BiFunction { cameraImg: Mat, template: Mat ->
            Log.d("Camera++++++++++", cameraImg.cols().toString())
            val result_cols = cameraImg.cols() -template.cols() + 1
            val result_rows = cameraImg.rows() - template.rows() + 1
            val result = Mat(result_rows, result_cols, CvType.CV_32FC1)
            Imgproc.matchTemplate(cameraImg, template, result, match_method)
            Imgproc.threshold(result, result, 0.90, 1.0, Imgproc.THRESH_TOZERO);
            var maxval: Double
            var matchLoc: Point

            loop@ while (true) {
                Log.e("DEMO", "__________")
                var mmr = Core.minMaxLoc(result)
                maxval = mmr.maxVal
                if (match_method == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED) {
                    matchLoc = mmr.minLoc
                } else {
                    matchLoc = mmr.maxLoc
                }
                //p =Mat(img.rows()+tt.rows(), img.cols()+tt.cols(),  CvType.CV_8UC3)
                //      Features2d.drawMatches(tt,keypoint1,img,keypoint2,matches,p)
                if (maxval > .90) {
                    Imgproc.rectangle(cameraImg, matchLoc, Point(matchLoc.x + template.cols(),
                            matchLoc.y + template.rows()), Scalar(0.0, 255.0, 0.0))
                    Imgproc.rectangle(result, matchLoc, Point(matchLoc.x + template.cols(),
                            matchLoc.y + template.rows()), Scalar(0.0, 255.0, 0.0), -1)

                } else {
                    break@loop
                }
            }
            Imgcodecs.imwrite(pt + "kk3.png", cameraImg)
            val bm = Bitmap.createBitmap(cameraImg.cols(), cameraImg.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(cameraImg, bm)

            return@BiFunction bm
        }).subscribeOn(Schedulers.io())

      //  img.subscribe().dispose()
        //tt.subscribe().dispose()
return obBmp

    }

    //  return@map bMap


    // }.subscribeOn(Schedulers.io()).


    //   var tt = Mat(templ.rows(),templ.cols(),CvType.CV_8U)
    // Imgproc.Canny(templ,tt,50.0,200.0)


//core.normalize not required
    //  Core.normalize(result, result, 0.0, 1.0, Core.NORM_MINMAX, -1, Mat())


    /*    var detector = ORB.create()   // astFeatureDetector.create()

        var desc1 = Mat()
        var desc2 = Mat()
        var keypoint1 = MatOfKeyPoint()
        var keypoint2 = MatOfKeyPoint()
        var set1 = detector.detect(tt, keypoint1)
        var set2 = detector.detect(img, keypoint2)

        detector.compute(tt, keypoint1, desc1)

        detector.compute(img, keypoint2, desc2)
        var matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE)
        var matches = MatOfDMatch()
        matcher.match(desc1, desc2, matches)
        var d=matches.toList()
        var p:Mat
*/


    //  Imgcodecs.imwrite(pt + "kk3.png", img)
    // Imgcodecs.imwrite(pt + "kk2.png", tt)

    /*unOnUiThread
    {
        var canvs = sh.lockCanvas()
        sh.setFixedSize(1024, 768)
        canvs.drawBitmap(bMap, 0F, 0F, null)
        sh.unlockCanvasAndPost(canvs)
    }
}
}
*/
}

