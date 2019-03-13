package com.example.sajin.aot_cam

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ImageReader.OnImageAvailableListener
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore.Images.Media.insertImage
import android.util.Log
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.widget.ImageView
import android.widget.Toast
import com.example.sajin.aot_cam.Constants.PwmVals
import com.example.sajin.aot_cam.Constants.StepperDirection
import com.example.sajin.aot_cam.Constants.StepperStyle
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.*
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.util.concurrent.Callable
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


class MainActivity : Activity(), LoaderCallbackInterface {

    private val TAG = MainActivity::class.java.simpleName
   // private var imgview: ImageView? = null
    private var mCamera: CameraLib? = null
    private var mCameraHandler: Handler? = null
    private var mCameraThread: HandlerThread? = null
    lateinit var pt: String
   // private var mCloudHandler: Handler? = null
    lateinit var  sh: WeakReference< SurfaceHolder>
  //  private var mCloudThread: HandlerThread? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main);
        Log.d(TAG, "Activity created.")

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "No permission")
            return
        }
        var stepcontroll: Single<PwmController> = Single.just(PwmController("I2C1"))
                .subscribeOn(Schedulers.newThread())

        button4.setOnClickListener {
            var start = System.currentTimeMillis()
            stepcontroll.observeOn(Schedulers.newThread()).subscribe { t1, _->

                var m1 = StepperMotorController(t1, PwmVals.STEPPER_TWO)
                m1.setSpeed(500)
                m1.step(800, StepperDirection.BACKWARD, StepperStyle.DOUBLE)
                val b = System.currentTimeMillis() - start
                Log.d(TAG, b.toString() + "=====================")
                m1.reset()
                Log.i("Motor1 thread",Thread.currentThread().id.toString())
            }
            stepcontroll.observeOn(Schedulers.newThread()).subscribe { t1, _ ->
                var m2 = StepperMotorController(t1, PwmVals.STEPPER_ONE)
                m2.setSpeed(50)
                m2.step(800, StepperDirection.FORWARD, StepperStyle.DOUBLE)
                val b = System.currentTimeMillis() - start
                Log.d(TAG, b.toString() + "=====================")
                m2.reset()
                Log.i("Motor2 thread",Thread.currentThread().id.toString())
            }

            Log.i("Main thread",Thread.currentThread().id.toString())
            // stepperone!!.start()
            // var  stepperoneHandler = Handler(mCameraThread!!.looper)
        }

        val mediaStorageDir = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraDemo")
        if (!mediaStorageDir.exists()) {
            !mediaStorageDir.mkdirs()
        }
        pt = mediaStorageDir.path + File.separator
        sh = WeakReference(surfaceView3.holder)
        mCameraThread = HandlerThread("CameraBackground")
        mCameraThread!!.start()
        mCameraHandler = Handler(mCameraThread!!.looper)
        val button = button
        button.setOnClickListener {

            mCamera!!.takePicture()
        }

            mCamera = CameraLib.instance
            mCamera!!.initializeCamera(this, mCameraHandler!!, mOnImageAvailableListener, sh.get()!!)
        Log.i("main thread",Thread.currentThread().id.toString())

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


        var imgob: Flowable<Mat> = Flowable.create<Mat>({ it ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                var width = image.width
                var height = image.height
                val imageBuf = image.planes[0].buffer
                val imageBytes = ByteArray(imageBuf.remaining())
              var a=  BitmapFactory.decodeByteArray(imageBytes,0,imageBytes.size)
                imageBuf.get(imageBytes)
                image.close()

                var img1 = Mat(1,height*width, CvType.CV_8UC3, imageBuf)
                var img = Imgcodecs.imdecode(img1, Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);

                Log.i("Threadonlistenr",Thread.currentThread().id.toString())
                if (!it.isCancelled) {
                    it.onNext(img)

                } else {
                    Log.d("Subscribe", "cancelled")
                }
            } else {
                Log.d("Image", "image null")
                // image!!.close()
            }

        }, BackpressureStrategy.LATEST).take(1)
        //emitter.onComplete()
        // imgob.subscribeOn(Schedulers.io())

        onPictureTaken(imgob.timeout(200, TimeUnit.MILLISECONDS))

    }

    private fun onPictureTaken(ima: Flowable<Mat>) {
        var disp = CompositeDisposable()

        disp.add(run(ima, pt + "tt.png", Imgproc.TM_CCOEFF_NORMED).subscribeOn(Schedulers.single())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({


                    var canvs = sh.get()!!.lockCanvas()
                    sh.get()!!.setFixedSize(640, 480)
                    canvs.drawBitmap(it, 0F, 0F, null)
                    sh.get()!!.unlockCanvasAndPost(canvs)
                    ima.subscribe().dispose()
                    it.recycle()
                }, {
                    Log.d("error", it.message)
                    // throw it
                }))



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

        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mOpenCVCallBack)) {
            Log.e("OPENCV", "Cannot connect to OpenCV Manager")
        }
    }

    protected fun onOpenCVReady() {
        Toast.makeText(applicationContext, "opencv ready", Toast.LENGTH_LONG).show()
    }
    override fun onManagerConnected(status: Int) {
        Log.e("OPENCV", "connected" + Int)
    }
    override fun onPackageInstall(operation: Int,
                                  callback: InstallCallbackInterface) {
        // TODO Auto-generated method stub
    }

    public fun run(img: Flowable<Mat>, templateFile: String,
                   match_method: Int): Flowable<Bitmap> {
        //   println("\nRunning Template Matching")
        var tt = Flowable.create<Mat>({
            var tt = Imgcodecs.imread(templateFile, Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE)
            Log.i("run thread",Thread.currentThread().id.toString())
            it.onNext(tt)

        }, BackpressureStrategy.LATEST)
        Log.i("Motor1 thread",Thread.currentThread().id.toString())
        return img.zipWith(tt, BiFunction { cameraImg: Mat, template: Mat ->
            Log.d("Camera++++++++++", cameraImg.cols().toString())
            val result_cols = cameraImg.cols() - template.cols() + 1
            val result_rows = cameraImg.rows() - template.rows() + 1
            val result = Mat(result_rows, result_cols, CvType.CV_32FC1)
            Imgproc.matchTemplate(cameraImg, template, result, match_method)
            Imgproc.threshold(result, result, 0.90, 1.0, Imgproc.THRESH_TOZERO);
            var maxval: Double
            var matchLoc: Point
            Log.i("Motor1 thread",Thread.currentThread().id.toString())
           // loop@ while (true) {
               // Log.e("DEMO", "__________")
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
                   // Imgproc.rectangle(result, matchLoc, Point(matchLoc.x + template.cols(), matchLoc.y + template.rows()), Scalar(0.0, 255.0, 0.0), -1)

                }
                //else {
                   // break@loop
                //}
           // }
           // Imgcodecs.imwrite(pt + "kk3.png", cameraImg)
            val bm = Bitmap.createBitmap(cameraImg.cols(), cameraImg.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(cameraImg, bm)
            Log.i("Run2",Thread.currentThread().id.toString())
            template.release()
            cameraImg.release()
            return@BiFunction bm

        })
        //Log.i("Run1",Thread.currentThread().id.toString())
        //  img.subscribe().dispose()
        //tt.subscribe().dispose()
        //return obBmp

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

