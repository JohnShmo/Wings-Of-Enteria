package johnshmo.woe.effects

import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils.clamp
import java.awt.Color
import johnshmo.woe.CachedSprite
import org.lwjgl.util.vector.Vector2f

class CampaignLaser(
    coreSpriteCategory: String,
    coreSpriteId: String,
    fringeSpriteCategory: String,
    fringeSpriteId: String,
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
    private val cachedCoreSprite = CachedSprite(coreSpriteCategory, coreSpriteId)
    private val cachedFringeSprite = CachedSprite(fringeSpriteCategory, fringeSpriteId)

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
        coreCycle -= amount * coreCycleRate
        if (coreCycle < 0.0f) {
            coreCycle += 1.0f
        }
        if (coreCycle >= 1.0f) {
            coreCycle -= 1.0f
        }
        fringeCycle -= amount * fringeCycleRate
        if (fringeCycle < 0.0f) {
            fringeCycle += 1.0f
        }
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

    fun render(from: Vector2f, to: Vector2f) {
        if (cachedCoreSprite.sprite == null) return
        if (cachedFringeSprite.sprite == null) return
        if (intensity <= 0.0f) return
        val coreSprite = cachedCoreSprite.sprite!!
        val fringeSprite = cachedFringeSprite.sprite!!
        val angle = Misc.getAngleInDegrees(from, to)
        val direction = Misc.getUnitVector(from, to)
        val length = Misc.getDistance(from, to)
        val originalCoreHeight = coreSprite.height
        val originalFringeHeight = fringeSprite.height
        val originalCoreWidth = coreSprite.width
        val originalFringeWidth = fringeSprite.width

        val segmentLength = coreSprite.width
        val currCoreWidth = coreWidth * widthFraction
        val currFringeWidth = fringeWidth * widthFraction
        val currLength = length * lengthFraction
        coreSprite.width = segmentLength
        coreSprite.height = currCoreWidth
        fringeSprite.width = segmentLength
        fringeSprite.height = currFringeWidth
        coreSprite.angle = angle
        fringeSprite.angle = angle

        coreSprite.centerX = coreSprite.width / 2f
        coreSprite.centerY = coreSprite.height / 2f
        fringeSprite.centerX = fringeSprite.width / 2f
        fringeSprite.centerY = fringeSprite.height / 2f
        coreSprite.setAdditiveBlend()
        coreSprite.color = coreColor
        coreSprite.alphaMult = intensity
        fringeSprite.setAdditiveBlend()
        fringeSprite.color = fringeColor
        fringeSprite.alphaMult = intensity

        var remainingLength = currLength
        var offset = Vector2f(direction)
        offset.scale(segmentLength / 2)
        offset = Vector2f.add(offset, from, null)

        val increment = Vector2f(direction)
        increment.scale(segmentLength)
        while (remainingLength > 0) {
            if (remainingLength >= segmentLength) {
                var toAdd = Vector2f(direction)
                toAdd.scale(1f - segmentLength * fringeCycle)
                val fringePos = Vector2f.add(offset, toAdd, null)
                fringeSprite.renderRegionAtCenter(fringePos.x, fringePos.y, fringeCycle, 0f, 1f, 1f)

                toAdd = Vector2f(direction)
                toAdd.scale(1f - segmentLength * coreCycle)
                val corePos = Vector2f.add(offset, toAdd, null)
                coreSprite.renderRegionAtCenter(corePos.x, corePos.y, coreCycle, 0f, 1f, 1f)
            } else {
                val fraction = remainingLength / segmentLength

                var toAdd = Vector2f(direction)
                toAdd.scale(1f - segmentLength * fringeCycle)
                val fringePos = Vector2f.add(offset, toAdd, null)
                fringeSprite.renderRegionAtCenter(fringePos.x, fringePos.y, fringeCycle, 0f, fraction, 1f)

                toAdd = Vector2f(direction)
                toAdd.scale(1f - segmentLength * coreCycle)
                val corePos = Vector2f.add(offset, toAdd, null)
                coreSprite.renderRegionAtCenter(corePos.x, corePos.y, coreCycle, 0f, fraction, 1f)
            }
            offset = Vector2f.add(offset, increment, null)
            remainingLength -= segmentLength
        }

        coreSprite.width = originalCoreWidth
        coreSprite.height = originalCoreHeight
        fringeSprite.width = originalFringeWidth
        fringeSprite.height = originalFringeHeight
    }

    private fun advanceInactive(): State {
        widthFraction = 0.0f
        lengthFraction = 0.0f
        return State.INACTIVE
    }

    private fun advanceIn(amount: Float): State {
        widthFraction = clamp(widthFraction + amount * inSpeed * 2f, 0f, 1f)
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