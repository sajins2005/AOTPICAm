package com.example.sajin.aot_cam

import org.opencv.android.InstallCallbackInterface
import org.opencv.android.LoaderCallbackInterface

class CameraObervable : LoaderCallbackInterface {
    override fun onPackageInstall(operation: Int, callback: InstallCallbackInterface?) {
       //LOg.d
    }

    override fun onManagerConnected(status: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}