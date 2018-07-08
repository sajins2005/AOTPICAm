package org.opencv.samples.puzzle15

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

import android.util.Log


/**
 * This class is a controller for puzzle game.
 * It converts the image from Camera into the shuffled image
 */
class Puzzle15Processor {

    private val mIndexes: IntArray
    private val mTextWidths: IntArray
    private val mTextHeights: IntArray

    private var mRgba15: Mat? = null
    private var mCells15: Array<Mat?>? = null
    private var mShowTileNumbers = true

    private val isPuzzleSolvable: Boolean
        get() {

            var sum = 0
            for (i in 0 until GRID_AREA) {
                if (mIndexes[i] == GRID_EMPTY_INDEX)
                    sum += i / GRID_SIZE + 1
                else {
                    var smaller = 0
                    for (j in i + 1 until GRID_AREA) {
                        if (mIndexes[j] < mIndexes[i])
                            smaller++
                    }
                    sum += smaller
                }
            }
            return sum % 2 == 0
        }

    init {
        mTextWidths = IntArray(GRID_AREA)
        mTextHeights = IntArray(GRID_AREA)

        mIndexes = IntArray(GRID_AREA)

        for (i in 0 until GRID_AREA)
            mIndexes[i] = i
    }

    /* this method is intended to make processor prepared for a new game */
    @Synchronized
    fun prepareNewGame() {
        do {
            shuffle(mIndexes)
        } while (!isPuzzleSolvable)
    }

    /* This method is to make the processor know the size of the frames that
     * will be delivered via puzzleFrame.
     * If the frames will be different size - then the result is unpredictable
     */
    @Synchronized
    fun prepareGameSize(width: Int, height: Int) {
        mRgba15 = Mat(height, width, CvType.CV_8UC4)
        mCells15 = arrayOfNulls(size = GRID_AREA)

        for (i in 0 until GRID_SIZE) {
            for (j in 0 until GRID_SIZE) {
                val k = i * GRID_SIZE + j
                mCells15!![k] = mRgba15!!.submat(i * height / GRID_SIZE, (i + 1) * height / GRID_SIZE, j * width / GRID_SIZE, (j + 1) * width / GRID_SIZE)
            }
        }

        for (i in 0 until GRID_AREA) {
            val s = Imgproc.getTextSize(Integer.toString(i + 1), 3/* CV_FONT_HERSHEY_COMPLEX */, 1.0, 2, null)
            mTextHeights[i] = s.height.toInt()
            mTextWidths[i] = s.width.toInt()
        }
    }

    /* this method to be called from the outside. it processes the frame and shuffles
     * the tiles as specified by mIndexes array
     */
    @Synchronized
    fun puzzleFrame(inputPicture: Mat): Mat? {
        val cells = arrayOfNulls<Mat>(GRID_AREA)
        var rows = inputPicture.rows()
        var cols = inputPicture.cols()

        rows = rows - rows % 4
        cols = cols - cols % 4

        for (i in 0 until GRID_SIZE) {
            for (j in 0 until GRID_SIZE) {
                val k = i * GRID_SIZE + j
                cells[k] = inputPicture.submat(i * inputPicture.rows() / GRID_SIZE, (i + 1) * inputPicture.rows() / GRID_SIZE, j * inputPicture.cols() / GRID_SIZE, (j + 1) * inputPicture.cols() / GRID_SIZE)
            }
        }

        rows = rows - rows % 4
        cols = cols - cols % 4

        // copy shuffled tiles
        for (i in 0 until GRID_AREA) {
            val idx = mIndexes[i]
            if (idx == GRID_EMPTY_INDEX)
                mCells15!![i]!!.setTo(GRID_EMPTY_COLOR)
            else {
                cells[idx]!!.copyTo(mCells15!![i])
                if (mShowTileNumbers) {
                    Imgproc.putText(mCells15!![i], Integer.toString(1 + idx), Point(((cols / GRID_SIZE - mTextWidths[idx]) / 2).toDouble(),
                            ((rows / GRID_SIZE + mTextHeights[idx]) / 2).toDouble()), 3/* CV_FONT_HERSHEY_COMPLEX */, 1.0, Scalar(255.0, 0.0, 0.0, 255.0), 2)
                }
            }
        }

        for (i in 0 until GRID_AREA) {
            cells[i]?.release()
        }

        drawGrid(cols, rows, mRgba15)

        return mRgba15
    }

    fun toggleTileNumbers() {
        mShowTileNumbers = !mShowTileNumbers
    }

    fun deliverTouchEvent(x: Int, y: Int) {
        val rows = mRgba15!!.rows()
        val cols = mRgba15!!.cols()

        val row = Math.floor((y * GRID_SIZE / rows).toDouble()).toInt()
        val col = Math.floor((x * GRID_SIZE / cols).toDouble()).toInt()

        if (row < 0 || row >= GRID_SIZE || col < 0 || col >= GRID_SIZE) {
            Log.e(TAG, "It is not expected to get touch event outside of picture")
            return
        }

        val idx = row * GRID_SIZE + col
        var idxtoswap = -1

        // left
        if (idxtoswap < 0 && col > 0)
            if (mIndexes[idx - 1] == GRID_EMPTY_INDEX)
                idxtoswap = idx - 1
        // right
        if (idxtoswap < 0 && col < GRID_SIZE - 1)
            if (mIndexes[idx + 1] == GRID_EMPTY_INDEX)
                idxtoswap = idx + 1
        // top
        if (idxtoswap < 0 && row > 0)
            if (mIndexes[idx - GRID_SIZE] == GRID_EMPTY_INDEX)
                idxtoswap = idx - GRID_SIZE
        // bottom
        if (idxtoswap < 0 && row < GRID_SIZE - 1)
            if (mIndexes[idx + GRID_SIZE] == GRID_EMPTY_INDEX)
                idxtoswap = idx + GRID_SIZE

        // swap
        if (idxtoswap >= 0) {
            synchronized(this) {
                val touched = mIndexes[idx]
                mIndexes[idx] = mIndexes[idxtoswap]
                mIndexes[idxtoswap] = touched
            }
        }
    }

    private fun drawGrid(cols: Int, rows: Int, drawMat: Mat?) {
        for (i in 1 until GRID_SIZE) {
            Imgproc.line(drawMat!!, Point(0.0, (i * rows / GRID_SIZE).toDouble()), Point(cols.toDouble(), (i * rows / GRID_SIZE).toDouble()), Scalar(0.0, 255.0, 0.0, 255.0), 3)
            Imgproc.line(drawMat, Point((i * cols / GRID_SIZE).toDouble(), 0.0), Point((i * cols / GRID_SIZE).toDouble(), rows.toDouble()), Scalar(0.0, 255.0, 0.0, 255.0), 3)
        }
    }

    companion object {

        private val GRID_SIZE = 4
        private val GRID_AREA = GRID_SIZE * GRID_SIZE
        private val GRID_EMPTY_INDEX = GRID_AREA - 1
        private val TAG = "Puzzle15Processor"
        private val GRID_EMPTY_COLOR = Scalar(33.00, 33.0, 33.0, 0.0)

        private fun shuffle(array: IntArray) {
            for (i in array.size downTo 2) {
                val temp = array[i - 1]
                val randIx = (Math.random() * i).toInt()
                array[i - 1] = array[randIx]
                array[randIx] = temp
            }
        }
    }
}
