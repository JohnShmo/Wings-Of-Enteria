package johnshmo.woe.effects

import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.util.Misc
import johnshmo.woe.CachedSprite
import johnshmo.woe.InterpolatedFloat
import johnshmo.woe.easeInCubic
import johnshmo.woe.easeInQuad
import johnshmo.woe.easeOutQuad
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

class MantleTapEffect {
    private val cachedBrightSpot = CachedSprite(CATEGORY, BRIGHT_SPOT_ID)
    private val cachedRimLight = CachedSprite(CATEGORY, RIM_LIGHT_ID)
    private val cachedScorchMarks = CachedSprite(CATEGORY, SCORCH_MARKS_ID)
    private val cachedCracks1 = CachedSprite(CATEGORY, CRACKS_1_ID)
    private val cachedCracks2 = CachedSprite(CATEGORY, CRACKS_2_ID)

    private var rimLightFader = 0.0f
    private val brightSpotFader = InterpolatedFloat(0.0f) @JvmSerializableLambda { x: Float -> easeOutQuad(x) }
    private val scorchMarksFader = InterpolatedFloat(0.0f) @JvmSerializableLambda { x: Float -> easeInCubic(x) }
    private val cracks1Fader = InterpolatedFloat(0.0f) @JvmSerializableLambda { x: Float -> easeInCubic(x) }
    private val cracks2Fader = InterpolatedFloat(0.0f) @JvmSerializableLambda { x: Float -> easeInCubic(x) }

    var intensity = 0.0f
    private var particleSpawnTimer = 0.0f

    fun advance(amount: Float, target: PlanetAPI, angle: Float) {
        rimLightFader = easeInQuad(intensity)
        if (intensity < 0.95f) {
            brightSpotFader.set(0.0f, 1.0f)
            scorchMarksFader.set(0.0f, 20.0f)
            cracks1Fader.set(0.0f, 10.0f)
            cracks2Fader.set(0.0f, 15.0f)
        } else {
            brightSpotFader.set(1.0f, 1.0f)
            scorchMarksFader.set(1.0f, 20.0f)
            cracks1Fader.set(1.0f, 25.0f)
            cracks2Fader.set(1.0f, 25.0f)

            particleSpawnTimer += amount * 5.0f
            if (particleSpawnTimer >= 1.0f) {
                particleSpawnTimer -= Misc.random.nextFloat()
                val spawnLocation = MathUtils.getPointOnCircumference(target.location, target.radius, angle)
                val spawnAngle = angle + ((Misc.random.nextFloat() - 0.5f) * 90f)
                val speed = Misc.random.nextFloat() * 100f
                val velocity = Vector2f.add(Misc.getUnitVectorAtDegreeAngle(spawnAngle), target.velocity, null)
                velocity.scale(speed)
                val size = Misc.random.nextFloat() * 16f + 8f
                val rampUp = 0.25f + (1f - Misc.random.nextFloat() * 0.5f) * 0.5f
                val duration = 0.25f + (1f - Misc.random.nextFloat() * 0.5f)
                Misc.addGlowyParticle(
                    target.containingLocation,
                    spawnLocation,
                    velocity,
                    size,
                    rampUp,
                    duration,
                    Misc.genColor(
                        Color(0xFF, 0x2F, 0x2F),
                        Color(0xFF, 0xFF, 0xFF),
                        Misc.random
                    )
                )
            }
        }

        brightSpotFader.advance(amount)
        scorchMarksFader.advance(amount)
        cracks1Fader.advance(amount)
        cracks2Fader.advance(amount)
    }

    fun render(target: PlanetAPI, angle: Float) {
        val brightSpot = cachedBrightSpot.sprite!!
        val rimLight = cachedRimLight.sprite!!
        val scorchMarks = cachedScorchMarks.sprite!!
        val cracks1 = cachedCracks1.sprite!!
        val cracks2 = cachedCracks2.sprite!!

        brightSpot.setAdditiveBlend()
        val brightSpotSize = 64f * brightSpotFader.value * (1.0f - Misc.random.nextFloat() * 0.1f)
        brightSpot.setSize(brightSpotSize, brightSpotSize)
        brightSpot.setCenter(brightSpot.width / 2, brightSpot.height / 2)
        brightSpot.alphaMult = brightSpotFader.value * (1.0f - Misc.random.nextFloat() * 0.025f)

        rimLight.setAdditiveBlend()
        rimLight.color = Color(0xFF, 0x2F, 0x2F)
        rimLight.alphaMult = rimLightFader * (1.0f - Misc.random.nextFloat() * 0.025f)

        scorchMarks.setAdditiveBlend()
        scorchMarks.color = Color(255, (200 * scorchMarksFader.value).toInt(), (200 * scorchMarksFader.value).toInt())
        scorchMarks.alphaMult = scorchMarksFader.value

        cracks1.setAdditiveBlend()
        cracks1.color = Color(200, 100, 0)
        cracks1.alphaMult = cracks1Fader.value * (1.0f - Misc.random.nextFloat() * 0.025f)

        cracks2.setAdditiveBlend()
        cracks2.color = Color(180, 60, 0)
        cracks2.alphaMult = cracks2Fader.value * (1.0f - Misc.random.nextFloat() * 0.05f)

        val planetSize = target.radius * 2
        rimLight.setSize(planetSize, planetSize)
        rimLight.setCenter(planetSize / 2, planetSize / 2)
        scorchMarks.setSize(planetSize, planetSize)
        scorchMarks.setCenter(planetSize / 2, planetSize / 2)
        cracks1.setSize(planetSize, planetSize)
        cracks1.setCenter(planetSize / 2, planetSize / 2)
        cracks2.setSize(planetSize, planetSize)
        cracks2.setCenter(planetSize / 2, planetSize / 2)
        rimLight.angle = angle
        scorchMarks.angle = angle
        cracks1.angle = angle
        cracks2.angle = angle

        val location = target.location
        val brightSpotLocation = MathUtils.getPointOnCircumference(location, target.radius, angle)

        cracks1.renderAtCenter(location.x, location.y)
        cracks2.renderAtCenter(location.x, location.y)
        scorchMarks.renderAtCenter(location.x, location.y)
        rimLight.renderAtCenter(location.x, location.y)
        brightSpot.renderAtCenter(brightSpotLocation.x, brightSpotLocation.y)
    }

    companion object {
        private const val CATEGORY = "woe_fx"
        private const val BRIGHT_SPOT_ID = "mantle_tap_bright_spot"
        private const val RIM_LIGHT_ID = "mantle_tap_rim_light"
        private const val SCORCH_MARKS_ID = "mantle_tap_scorch_marks"
        private const val CRACKS_1_ID = "mantle_tap_cracks_1"
        private const val CRACKS_2_ID = "mantle_tap_cracks_2"
    }
}