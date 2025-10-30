package johnshmo.woe.effects

import org.lazywizard.lazylib.MathUtils.clamp
import java.awt.Color

class CampaignLaser(
    val coreSpriteCategory: String,
    val coreSpriteId: String,
    val fringeSpriteCategory: String,
    val fringeSpriteId: String,
    val coreColor: Color,
    val fringeColor: Color,
    val coreWidth: Float,
    val fringeWidth: Float,
    val coreCycleRate: Float,
    val fringeCycleRate: Float,
    val inSpeed: Float,
    val outSpeed: Float
) {
    enum class State {
        INACTIVE,
        IN,
        ACTIVE,
        OUT
    }

    private var stateImpl = State.INACTIVE
    val state: State
        get() = stateImpl

    private var widthFraction = 0.0f
    private var lengthFraction = 0.0f
    private var coreCycle = 0.0f
    private var fringeCycle = 0.0f

    val intensity: Float
        get() {
            return widthFraction * lengthFraction
        }

    fun activate() {
        if (state == State.INACTIVE || state == State.OUT) {
            stateImpl = State.IN
        }
    }

    fun deactivate() {
        if (state == State.ACTIVE || state == State.IN) {
            stateImpl = State.OUT
        }
    }

    fun advance(amount: Float) {
        coreCycle += amount * coreCycleRate
        if (coreCycle >= 1.0f) {
            coreCycle -= 1.0f
        }
        fringeCycle += amount * fringeCycleRate
        if (fringeCycle >= 1.0f) {
            fringeCycle -= 1.0f
        }
        stateImpl = when (state) {
            State.INACTIVE -> advanceInactive()
            State.IN -> advanceIn(amount)
            State.ACTIVE -> advanceActive()
            State.OUT -> advanceOut(amount)
        }
    }

    private fun advanceInactive(): State {
        return State.INACTIVE
    }

    private fun advanceIn(amount: Float): State {
        widthFraction = clamp(widthFraction + amount * inSpeed, 0f, 1f)
        lengthFraction = clamp(lengthFraction + amount * inSpeed, 0f, 1f)
        if (widthFraction >= 1.0f && lengthFraction >= 1.0f) return State.ACTIVE
        return State.IN
    }

    private fun advanceActive(): State {
        return State.ACTIVE
    }

    private fun advanceOut(amount: Float): State {
        widthFraction = clamp(widthFraction - amount * outSpeed, 0f, 1f)
        if (widthFraction <= 0.0f) return State.INACTIVE
        return State.OUT
    }
}