package johnshmo.woe

import kotlin.math.pow


typealias CurveFunction = (Float) -> Float

fun lerp(a: Float, b: Float, t: Float): Float {
    return a + (b - a) * t
}

fun easeInOutLinear(x: Float): Float = x
fun easeInQuad(x: Float): Float = x * x
fun easeOutQuad(x: Float): Float = 1f - (1f - x) * (1f - x);
fun easeInOutQuad(x: Float): Float = if (x < 0.5f) 2f * x * x else 1f - (-2f * x + 2f).pow(2.0f) / 2f
fun easeInCubic(x: Float): Float = x * x * x
fun easeOutCubic(x: Float): Float = 1f - (1f - x).pow(3.0f)
fun easeInOutCubic(x: Float): Float = if (x < 0.5f) 4f * x * x * x else 1f - (-2f * x + 2f).pow(3.0f) / 2;