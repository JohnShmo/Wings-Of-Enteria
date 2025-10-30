package johnshmo.woe.entities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin
import com.fs.starfarer.api.util.Misc
import johnshmo.woe.CachedSprite
import johnshmo.woe.InterpolatedFloat
import johnshmo.woe.easeInOutElastic
import johnshmo.woe.easeInQuad
import org.lazywizard.lazylib.MathUtils.clamp
import org.lwjgl.util.vector.Vector2f

class WingsOfEnteria : BaseCustomEntityPlugin() {
    private val blinkGreen = CachedSprite(CATEGORY, BLINK_GREEN_ID)
    private val blinkYellow = CachedSprite(CATEGORY, BLINK_YELLOW_ID)
    private val engineGlow = CachedSprite(CATEGORY, ENGINE_GLOW_ID)
    private val ringLights = arrayOf(
        CachedSprite(CATEGORY, RING_IDS[0]),
        CachedSprite(CATEGORY, RING_IDS[1]),
        CachedSprite(CATEGORY, RING_IDS[2]),
        CachedSprite(CATEGORY, RING_IDS[3]),
        CachedSprite(CATEGORY, RING_IDS[4]),
        CachedSprite(CATEGORY, RING_IDS[5]),
        CachedSprite(CATEGORY, RING_IDS[6]),
        CachedSprite(CATEGORY, RING_IDS[7]),
        CachedSprite(CATEGORY, RING_IDS[8]),
        CachedSprite(CATEGORY, RING_IDS[9]),
        CachedSprite(CATEGORY, RING_IDS[10]),
    )

    @Transient private var blinkGreenFader: InterpolatedFloat? = null
    @Transient private var blinkYellowFader: InterpolatedFloat? = null
    @Transient private var engineGlowFader: InterpolatedFloat? = null
    @Transient private var ringLightFaders: MutableList<InterpolatedFloat>? = null
    @Transient private var ringLightTimer: Float? = null
    @Transient private var blinkTimer: Float? = null
    @Transient private var engineTimer: Float? = null

    fun ensureTransientVals() {
        if (blinkGreenFader == null) blinkGreenFader = InterpolatedFloat(0f, 0.75f) { x: Float -> easeInOutElastic(x) }
        if (blinkYellowFader == null) blinkYellowFader = InterpolatedFloat(0f, 0.75f) { x: Float -> easeInOutElastic(x) }
        if (engineGlowFader == null) engineGlowFader = InterpolatedFloat(0f, 1.0f) { x: Float -> easeInOutElastic(x) }
        if (ringLightFaders == null) {
            ringLightFaders = ArrayList(11)
            var i = 0
            while (i < ringLights.size) {
                ringLightFaders!!.add(InterpolatedFloat(0f, 0.1f) { x: Float -> easeInQuad(x) })
                i++
            }
        }
        if (ringLightTimer == null) ringLightTimer = 0.0f
        if (blinkTimer == null) blinkTimer = 0.0f
        if (engineTimer == null) engineTimer = 0.0f
    }

    override fun advance(amount: Float) {
        if (Global.getSector().isPaused) return

        ensureTransientVals()
        updateGlowEffects(amount)

        val angleToFocus = Misc.getAngleInDegrees(entity.location, entity.orbitFocus.location)
        entity.facing = (angleToFocus + 180f) - 36.5f
    }

    private fun updateGlowEffects(amount: Float) {
        ringLightTimer = ringLightTimer!!.plus(amount * 0.75f)
        ringLightTimer?.let {
            if (it >= 1.0f) {
                ringLightTimer = it - 2.0f
            }
        }
        val ringLightFraction = 1.0f / ringLightFaders!!.size

        for (i in ringLightFaders!!.indices) {
            val prevFraction = ringLightFraction * (i - 1)
            val nextFraction = ringLightFraction * (i)
            if (ringLightTimer!! in prevFraction..nextFraction) {
                ringLightFaders!![i].set(1.0f, 0.05f)
            } else {
                ringLightFaders!![i].set(0.0f, 0.5f)
            }
            ringLightFaders!![i].advance(amount)
        }

        blinkTimer = blinkTimer!!.plus(amount * 2f)
        blinkTimer?.let {
            if (it >= 10.0f) {
                blinkTimer = it - 10.0f
            }
        }
        if (blinkTimer!! < 2.0f) {
            blinkGreenFader!!.set(1.0f, 0.5f)
            blinkYellowFader!!.set(0.0f, 2.0f)
        } else if (blinkTimer!! in 3.0f .. 5.0f) {
            blinkGreenFader!!.set(0.0f, 2.0f)
            blinkYellowFader!!.set(1.0f, 0.5f)
        } else {
            blinkYellowFader!!.set(0.0f, 2.0f)
            blinkYellowFader!!.set(0.0f, 2.0f)
        }
        blinkGreenFader!!.advance(amount)
        blinkYellowFader!!.advance(amount)

        engineTimer = engineTimer!!.plus(amount * 5)
        engineTimer?.let {
            if (it >= 1.0f) {
                engineTimer = it - 1.0f
                engineGlowFader!!.set(Misc.random.nextFloat(), 1f / 5f)
            }
        }

        engineGlowFader!!.advance(amount)
    }

    override fun render(layer: CampaignEngineLayers?, viewport: ViewportAPI?) {
        if (layer != CampaignEngineLayers.STATIONS) return
        ensureTransientVals()
        renderGlowEffects()
    }

    private fun renderGlowEffects() {
        val blinkGreenSprite = blinkGreen.sprite!!
        val blinkYellowSprite = blinkYellow.sprite!!
        val engineGlowSprite = engineGlow.sprite!!
        val ringLightSprites = ArrayList<SpriteAPI>(ringLights.size)
        for (spr in ringLights) {
            ringLightSprites.add(spr.sprite!!)
        }
        val location = entity.location
        val facing = entity.facing - 90
        val width = 256f
        val height = 256f

        engineGlowSprite.setAdditiveBlend()
        engineGlowSprite.alphaMult = clamp(
            engineGlowFader!!.value * (1.0f - Misc.random.nextFloat() * 0.3333f),
            0f,
            1f
        )
        renderSprite(engineGlowSprite, location, facing, width, height)

        blinkYellowSprite.setAdditiveBlend()
        blinkYellowSprite.alphaMult = clamp(
            blinkYellowFader!!.value * (1.0f - Misc.random.nextFloat() * 0.15f),
            0f,
            1f
        )
        renderSprite(blinkYellowSprite, location, facing, width, height)

        blinkGreenSprite.setAdditiveBlend()
        blinkGreenSprite.alphaMult = clamp(
            blinkGreenFader!!.value * (1.0f - Misc.random.nextFloat() * 0.15f),
            0f,
            1f
        )
        renderSprite(blinkGreenSprite, location, facing, width, height)

        for (i in ringLightSprites.indices) {
            ringLightSprites[i].setAdditiveBlend()
            ringLightSprites[i].alphaMult = ringLightFaders!![i].value
            renderSprite(ringLightSprites[i], location, facing, width, height)
        }
    }

    private fun renderSprite(sprite: SpriteAPI, location: Vector2f, facing: Float, width: Float, height: Float) {
        sprite.setSize(width, height)
        sprite.setCenter(width / 2, height / 2)
        sprite.angle = facing
        sprite.renderAtCenter(location.x, location.y)
    }

    companion object {
        private const val CATEGORY = "woe_stations"
        private const val BLINK_GREEN_ID = "wings_of_enteria_blink_green"
        private const val BLINK_YELLOW_ID = "wings_of_enteria_blink_yellow"
        private const val ENGINE_GLOW_ID = "wings_of_enteria_engine_glow"
        private val RING_IDS = arrayOf(
            "wings_of_enteria_ring_00",
            "wings_of_enteria_ring_01",
            "wings_of_enteria_ring_02",
            "wings_of_enteria_ring_03",
            "wings_of_enteria_ring_04",
            "wings_of_enteria_ring_05",
            "wings_of_enteria_ring_06",
            "wings_of_enteria_ring_07",
            "wings_of_enteria_ring_08",
            "wings_of_enteria_ring_09",
            "wings_of_enteria_ring_10",
        )
    }
}