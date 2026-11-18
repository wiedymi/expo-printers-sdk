package expo.modules.printers.commons

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

// Half the side of the square of dots that gives the local mean, so the square is 7 x 7 dots, about one character
// on a 192-dot printer.
private const val LOCAL_RADIUS = 3

// A dot is black or white when its lightness differs from the local mean by more than this value.
private const val LOCAL_CONTRAST = 16f

private const val MIDDLE_LIGHTNESS = 128f

// Height for targetWidth with the same aspect ratio.
internal fun Bitmap.scaledHeight(targetWidth: Int): Int =
    (height.toLong() * targetWidth / width)
        .coerceIn(1, Int.MAX_VALUE.toLong())
        .toInt()

// Scales to the width, keeps the aspect ratio, and makes each dot black or white, for printers with few dots.
// Each dot gets the average lightness of all the image pixels that it covers. createScaledBitmap reads only a few
// of these pixels, so thin lines can disappear when it scales down a lot.
// A dot is black when it is darker than the mean of the dots around it by more than LOCAL_CONTRAST, and white when
// it is lighter by more than that. Other dots, for example in an area of one color, are black when they are darker
// than MIDDLE_LIGHTNESS. Thus thin dark text on white paper, thin white text on a dark box and dark text on a light
// gray box all stay visible. A fixed threshold can keep only one of them.
// Transparent pixels count as white.
internal fun Bitmap.toMonochrome(targetWidth: Int): Bitmap {
    val targetHeight = scaledHeight(targetWidth)
    val lightness = areaAverageLightness(targetWidth, targetHeight)
    val means = localMeans(lightness, targetWidth, targetHeight, LOCAL_RADIUS)
    val pixels = IntArray(lightness.size) { index ->
        val difference = lightness[index] - means[index]
        val black = when {
            difference < -LOCAL_CONTRAST -> true
            difference > LOCAL_CONTRAST -> false
            else -> lightness[index] < MIDDLE_LIGHTNESS
        }
        if (black) Color.BLACK else Color.WHITE
    }
    return Bitmap.createBitmap(pixels, targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
}

// Lightness from 0 (black) to 255 (white) of each target dot, in rows. Each value is the mean of the source pixels
// that the dot covers, weighted by the covered area.
private fun Bitmap.areaAverageLightness(targetWidth: Int, targetHeight: Int): FloatArray {
    val scaleX = width.toDouble() / targetWidth
    val scaleY = height.toDouble() / targetHeight
    val sourceRow = IntArray(width)
    val result = FloatArray(Math.multiplyExact(targetWidth, targetHeight))
    for (targetY in 0 until targetHeight) {
        val rowStart = targetY * targetWidth
        forEachOverlap(targetY, scaleY, height) { sourceY, weightY ->
            getPixels(sourceRow, 0, width, 0, sourceY, width, 1)
            for (targetX in 0 until targetWidth) {
                var rowSum = 0.0
                forEachOverlap(targetX, scaleX, width) { sourceX, weightX ->
                    rowSum += lightness(sourceRow[sourceX]) * weightX
                }
                result[rowStart + targetX] += (rowSum * weightY / (scaleX * scaleY)).toFloat()
            }
        }
    }
    return result
}

// Calls action for each source pixel that the target pixel covers, with the covered length of the source pixel.
private inline fun forEachOverlap(target: Int, scale: Double, sourceSize: Int, action: (Int, Double) -> Unit) {
    val start = target * scale
    val end = start + scale
    for (source in floor(start).toInt() until min(ceil(end).toInt(), sourceSize)) {
        val overlap = min(source + 1.0, end) - max(source.toDouble(), start)
        if (overlap > 0) action(source, overlap)
    }
}

// Lightness of a non-premultiplied color on white paper, from 0 (black) to 255 (white).
private fun lightness(color: Int): Double {
    val alpha = Color.alpha(color) / 255.0
    val luma = 0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)
    return luma * alpha + 255 * (1 - alpha)
}

// Mean of the values in the square around each value, up to radius away. At the edges the square is cut.
private fun localMeans(values: FloatArray, width: Int, height: Int, radius: Int): FloatArray {
    // sums[y * stride + x] is the sum of all values above row y and left of column x.
    val stride = width + 1
    val sums = DoubleArray(Math.multiplyExact(stride, height + 1))
    for (y in 0 until height) {
        var rowSum = 0.0
        for (x in 0 until width) {
            rowSum += values[y * width + x]
            sums[(y + 1) * stride + x + 1] = sums[y * stride + x + 1] + rowSum
        }
    }
    return FloatArray(values.size) { index ->
        val x = index % width
        val y = index / width
        val left = max(x - radius, 0)
        val right = min(x + radius + 1, width)
        val top = max(y - radius, 0)
        val bottom = min(y + radius + 1, height)
        val sum = sums[bottom * stride + right] - sums[top * stride + right] -
            sums[bottom * stride + left] + sums[top * stride + left]
        (sum / ((right - left) * (bottom - top))).toFloat()
    }
}
