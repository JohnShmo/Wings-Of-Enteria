package johnshmo.woe

import kotlin.math.max
import kotlin.math.min

class InterpolatedFloat(initialValue: Float, var duration: Float, var curveFunction: CurveFunction? = null) {
    private var startValue = initialValue
    private var backingTargetValue: Float = initialValue
    val value: Float
        get() {
            val fraction = if (curveFunction != null) curveFunction!!.invoke(progress) else progress
            return lerp(startValue, targetValue, fraction)
        }

    var targetValue: Float
        get() {
            return backingTargetValue
        }
        set(value) {
            if (backingTargetValue == value) return
            startValue = this.value
            backingTargetValue = value
            progress = 0.0f
        }

    private var progress = 0.0f

    fun advance(amount: Float) {
        progress = max(0f, min(progress + if (duration > 0) amount / duration else 1.0f, 1.0f))
    }

    fun set(targetValue: Float, duration: Float) {
        this.targetValue = targetValue
        this.duration = duration
    }
}