package johnshmo.woe

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

typealias CurveFunction = (Float) -> Float

fun easeInOutLinear(x: Float): Float = x
fun easeInQuad(x: Float): Float = x * x
fun easeOutQuad(x: Float): Float = 1f - (1f - x) * (1f - x);
fun easeInOutQuad(x: Float): Float = if (x < 0.5f) 2f * x * x else 1f - (-2f * x + 2f).pow(2.0f) / 2f
fun easeInCubic(x: Float): Float = x * x * x
fun easeOutCubic(x: Float): Float = 1f - (1f - x).pow(3.0f)
fun easeInOutCubic(x: Float): Float = if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f).pow(3.0f) / 2
fun easeInQuart(x: Float): Float = x * x * x * x
fun easeOutQuart(x: Float): Float = 1f - (1f - x).pow(4.0f)
fun easeInOutQuart(x: Float): Float = if (x < 0.5f) 8f * x * x * x * x else 1f - (-2f * x + 2f).pow(4.0f) / 2f
fun easeInQuint(x: Float): Float = x * x * x * x * x
fun easeOutQuint(x: Float): Float = 1f - (1f - x).pow(5.0f)
fun easeInOutQuint(x: Float): Float = if (x < 0.5f) 16f * x * x * x * x * x else 1f - (-2f * x + 2f).pow(5.0f) / 2f
fun easeInSine(x: Float): Float = 1f - cos((x * Math.PI) / 2f).toFloat()
fun easeOutSine(x: Float): Float = sin((x * Math.PI) / 2f).toFloat()
fun easeInOutSine(x: Float): Float = (1f - cos(x * Math.PI)).toFloat() / 2f
fun easeInExpo(x: Float): Float = if (x == 0.0f) 0.0f else 2f.pow(10f * x - 10f)
fun easeOutExpo(x: Float): Float = if (x == 1.0f) 1.0f else 1f - 2f.pow(-10f * x)
fun easeInOutExpo(x: Float): Float = if (x == 0.0f) 0.0f else if (x == 1.0f) 1.0f else if (x < 0.5f) 2f.pow(20f * x - 10f) / 2f else (2f.pow(-20f * x + 10f) - 2f) / 2f
fun easeInCirc(x: Float): Float = 1f - sqrt(1f - x.pow(2f))
fun easeOutCirc(x: Float): Float = sqrt(1f - (1f - x).pow(2f))
fun easeInOutCirc(x: Float): Float = if (x < 0.5f) (1f - sqrt(1f - 4f * x.pow(2f))) / 2f else (sqrt(1f - (-2f * x + 2f).pow(2.0f)) + 1f) / 2f

fun easeInOutElastic(x: Float): Float {
    val c5 = (2 * Math.PI).toFloat() / 4.5f

    if (x == 0.0f) return 0.0f
    if (x == 1.0f) return 1.0f
    if (x < 0.5f) return -(2f.pow(20f * x - 10f) * sin((20f * x - 11.125f) * c5)) / 2f
    return (2f.pow(-20f * x + 10f) * sin((20f * x - 11.125f) * c5)) / 2f + 1f
}