package com.example.sajin.aot_cam

//import org.bytedeco.javacpp.BytePointe

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
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
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.*
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer


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
        mCamera = CameraLib.instance
        mCamera!!.initializeCamera(this, mCameraHandler!!, mOnImageAvailableListener, sh

        )


    }

    private fun initPIO() {


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
        // var  buf: byte[]
        val imageBytes = ByteArray(imageBuf.remaining())

        imageBuf.get(imageBytes)
        var img1 = Mat(1, image.height * image.width, CvType.CV_8UC3, imageBuf)
        var img = Imgcodecs.imdecode(img1, Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);

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
        var tt = Imgcodecs.imread(templateFile)
        //  var tt = Mat(templ.rows(),templ.cols(),CvType.CV_8U)
          // Imgproc.Canny(templ,tt,50.0,200.0)
        //   // / Create the result matrix
        val result_cols = img.cols() - tt.cols() + 1
        val result_rows = img.rows() - tt.rows() + 1
        val result = Mat(result_rows, result_cols, CvType.CV_32FC1)

        // / Do the Matching and Normalize
        Imgproc.matchTemplate(img, tt, result, match_method)

        Core.normalize(result, result, 0.0, 1.0, Core.NORM_MINMAX, -1, Mat())
        Imgproc.threshold(result, result, 0.98, 1.0, Imgproc.THRESH_TOZERO);

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
            if (maxval > .99) {
                Imgproc.rectangle(img, matchLoc, Point(matchLoc.x + tt.cols(),
                        matchLoc.y + tt.rows()), Scalar(0.0, 255.0, 0.0))
                Imgproc.rectangle(result, matchLoc, Point(matchLoc.x + tt.cols(),
                        matchLoc.y + tt.rows()), Scalar(0.0, 255.0, 0.0), -1)
            } else {
                break@loop
            }

        }
        //println("Writing $outFile")
        Imgcodecs.imwrite(pt + "kk3.png", img)
        Imgcodecs.imwrite(pt + "kk2.png", tt)
        // val return_buff = ByteArray((img.total() * img.channels()) as Int)
        val bm = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(img, bm)
        val bMap = bm
        runOnUiThread({

            var canvs = sh.lockCanvas()
            canvs.drawBitmap(bMap, 0F, 0F, null)
            sh.unlockCanvasAndPost(canvs)
        })
    }


    fun imageToMat(image: Image): Mat {
        var buffer: ByteBuffer
        var rowStride: Int
        var pixelStride: Int
        val width = image.getWidth()
        val height = image.getHeight()
        var offset = 0

        val planes = image.getPlanes()
        val data = ByteArray(image.getWidth() * image.getHeight() * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8)
        val rowData = ByteArray(planes[0].getRowStride())

        for (i in planes.indices) {
            buffer = planes[i].getBuffer()
            rowStride = planes[i].getRowStride()
            pixelStride = planes[i].getPixelStride()
            val w = if (i == 0) width else width / 2
            val h = if (i == 0) height else height / 2
            for (row in 0 until h) {
                val bytesPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8
                if (pixelStride == bytesPerPixel) {
                    val length = w * bytesPerPixel
                    buffer.get(data, offset, length)
                    if (h - row != 1) {
                        buffer.position(buffer.position() + rowStride - length)
                    }
                    offset += length
                } else {
                    if (h - row == 1) {
                        buffer.get(rowData, 0, width - pixelStride + 1)
                    } else {
                        buffer.get(rowData, 0, rowStride)
                    }

                    for (col in 0 until w) {
                        data[offset++] = rowData[col * pixelStride]
                    }
                }
            }
        }

        // Finally, create the Mat.
        val mat = Mat(height + height / 2, width, CvType.CV_8UC3)
        mat.put(0, 0, data)

        return mat
    }

}

