package johnshmo.woe.entities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin
import johnshmo.woe.CachedSprite
import johnshmo.woe.InterpolatedFloat
import johnshmo.woe.easeInOutQuad
import johnshmo.woe.easeInQuad
import johnshmo.woe.easeOutQuad
import org.lwjgl.util.vector.Vector2f

class WingsOfEnteria : BaseCustomEntityPlugin() {
    private val station = CachedSprite(CATEGORY, STATION_ID)
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

    fun ensureTransientVals() {
        if (blinkGreenFader == null) blinkGreenFader = InterpolatedFloat(0f, 0.75f) { x: Float -> easeInOutQuad(x) }
        if (blinkYellowFader == null) blinkYellowFader = InterpolatedFloat(0f, 0.75f) { x: Float -> easeInOutQuad(x) }
        if (engineGlowFader == null) engineGlowFader = InterpolatedFloat(0f, 1.0f) { x: Float -> easeInQuad(x) }
        if (ringLightFaders == null) {
            ringLightFaders = ArrayList(11)
            var i = 0
            while (i < ringLights.size) {
                ringLightFaders!!.add(InterpolatedFloat(0f, 0.1f) { x: Float -> easeInQuad(x) })
                i++
            }
        }
        if (ringLightTimer == null) ringLightTimer = 0.0f
    }

    override fun advance(amount: Float) {
        if (Global.getSector().isPaused) return

        ensureTransientVals()

        ringLightTimer = ringLightTimer!!.plus(amount * 0.75f)
        ringLightTimer?.let {
            if (it >= 1.0f) {
                ringLightTimer = ringLightTimer!! - 2.0f
            }
        }
        val ringLightFraction = 1.0f / ringLightFaders!!.size

        for (i in ringLightFaders!!.indices) {
            val prevFraction = ringLightFraction * (i - 1)
            val nextFraction = ringLightFraction * (i)
            if (ringLightTimer!! in prevFraction..nextFraction) {
                ringLightFaders!![i].duration = 0.1f
                ringLightFaders!![i].targetValue = 1.0f
            } else {
                ringLightFaders!![i].duration = 1.0f
                ringLightFaders!![i].targetValue = 0.0f
            }
            ringLightFaders!![i].advance(amount)
        }

        blinkGreenFader!!.advance(amount)
        blinkYellowFader!!.advance(amount)
        engineGlowFader!!.advance(amount)
    }

    override fun render(layer: CampaignEngineLayers?, viewport: ViewportAPI?) {
        if (layer != CampaignEngineLayers.STATIONS) return

        ensureTransientVals()

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
        renderSprite(engineGlowSprite, location, facing, width, height)

        blinkYellowSprite.setAdditiveBlend()
        renderSprite(blinkYellowSprite, location, facing, width, height)

        blinkGreenSprite.setAdditiveBlend()
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
        private const val STATION_ID = "wings_of_enteria"
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