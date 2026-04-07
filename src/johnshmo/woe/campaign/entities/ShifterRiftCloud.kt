package johnshmo.woe.campaign.entities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignEngineLayers
import com.fs.starfarer.api.campaign.LocationAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.util.Misc
import johnshmo.woe.WOECustomEntities
import johnshmo.woe.WOESounds
import johnshmo.woe.campaign.effects.ShifterRiftCloudRenderer
import johnshmo.woe.utils.easeInOutQuad
import johnshmo.woe.utils.easeOutCirc
import johnshmo.woe.utils.lerp
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class ShifterRiftCloud : BaseCustomEntityPlugin() {
    private class CloudInstance(
        var x: Float = 0f,
        var y: Float = 0f,
        var angle: Float = 0f,
        var inLifeTime: Float = 0f,
        var outLifeTime: Float = 0f,
        var size: Float = 0f
    )

    class Params(
        var x: Float = 0f,
        var y: Float = 0f,
        var radius: Float = 0f,
        var duration: Float = 0f
    )

    enum class State {
        IN,
        TRANSITION,
        OUT,
        END
    }

    private var params: Params? = null
    private var cloudInstances: MutableList<CloudInstance>? = null
    var state: State = State.IN
        private set
    private var lifeTime = 0f

    fun expire() {
        if (state == State.IN) state = State.TRANSITION
    }

    fun setLocation(x: Float, y: Float) {
        if (params == null || entity == null) return
        entity.setLocation(x, y)
        params!!.x = x
        params!!.y = y
    }

    var location: Vector2f
        get() = if (params == null || entity == null || entity.isExpired)
                Vector2f()
            else
                Vector2f(params!!.x, params!!.y)
        set(value) {
            setLocation(value.x, value.y)
        }

    fun setFacing(angle: Float) {
        entity.facing = angle
    }

    var containingLocation: LocationAPI?
        get() = if (entity == null || entity.isExpired) null else entity.containingLocation
        set(location) {
            if (entity == null || entity.isExpired) return
            entity.containingLocation?.removeEntity(entity)
            location?.addEntity(entity)
        }

    private fun createCloudInstance(x: Float, y: Float, distance: Float) {
        cloudInstances ?: return
        val cloudInstance = CloudInstance(
            x = x,
            y = y,
            angle = Misc.random.nextFloat() * 360f,
            inLifeTime = -(distance / 128),
            outLifeTime = -(distance / 128),
            size = (Misc.random.nextFloat() * 2f + 0.5f) *
                (0.5f + (1f - distance / max(params!!.radius, 1f)) * 0.5f)
        )
        cloudInstances!!.add(cloudInstance)
    }

    override fun init(entity: SectorEntityToken, pluginParams: Any?) {
        super.init(entity, pluginParams)
        val initParams = pluginParams as? Params ?: return
        params = initParams
        cloudInstances = mutableListOf()
        state = State.IN
        lifeTime = params!!.duration
        entity.setLightSource(null, Color.BLACK)

        val size = params!!.radius
        var d = 0f
        while (d <= size) {
            val distance = ((Misc.random.nextFloat() - 0.5f) * 8) + d
            val cloudsInRing = min(max(4, (d / 16).toInt()), 12)
            val angleIncrement = 360f / cloudsInRing

            var a = 0f
            while (a < 360) {
                val angle = Misc.random.nextFloat() * 360f
                val offsetX = distance * cos(Math.toRadians(angle.toDouble())).toFloat()
                val offsetY = distance * sin(Math.toRadians(angle.toDouble())).toFloat()
                val cloudX = offsetX + (Misc.random.nextFloat() - 0.5f) * 24
                val cloudY = offsetY + (Misc.random.nextFloat() - 0.5f) * 24

                createCloudInstance(cloudX, cloudY, distance)
                a += angleIncrement
            }
            d += 48f
        }

        val lowest = cloudInstances!!.minOfOrNull { it.outLifeTime } ?: 0f
        cloudInstances!!.forEach { it.outLifeTime -= (lowest - 1) }
    }

    override fun getRenderRange(): Float {
        return params?.radius?.plus(500f) ?: 0f
    }

    override fun advance(amount: Float) {
        try {
            if (entity == null || entity.isExpired || params == null || Global.getSector().isPaused) return
            params!!.x = entity.location.x
            params!!.y = entity.location.y

            if (params!!.duration > 0) {
                if (lifeTime <= 0 && state == State.IN) {
                    state = State.TRANSITION
                } else {
                    lifeTime -= amount
                }
            }

            val soundLoopFade = when (state) {
                State.IN -> {
                    val highestLifetime = cloudInstances?.maxOfOrNull { it.inLifeTime } ?: -1f
                    cloudInstances?.forEach { it.inLifeTime += amount * 2 }
                    easeOutCirc(max(min(1f, highestLifetime / 2f), 0f))
                }

                State.OUT -> {
                    val highestLifetime = cloudInstances?.maxOfOrNull { it.outLifeTime } ?: -1f
                    cloudInstances?.forEach { it.outLifeTime -= amount * 2 }
                    if (highestLifetime <= 0) {
                        Misc.fadeAndExpire(entity)
                        cloudInstances = null
                        state = State.END
                    }
                    easeOutCirc(max(min(1f, highestLifetime / 2f), 0f))
                }

                State.TRANSITION -> {
                    cloudInstances?.forEach { it.outLifeTime = min(it.inLifeTime, it.outLifeTime) }
                    state = State.OUT
                    1f
                }

                else -> 0f
            }

            playSoundLoop(0.5f + soundLoopFade * 0.5f, soundLoopFade)
        } catch (_: Exception) {
        }
    }

    private fun playSoundLoop(pitch: Float, volume: Float) {
        if (entity == null || entity.isExpired || params == null || !entity.isInCurrentLocation || !entity.isVisibleToPlayerFleet) return
        Global.getSoundPlayer().playLoop(
            WOESounds.SHIFTER_RIFT_LOOP, this, pitch, volume,
            entity.location, entity.velocity, 0.1f, 0.1f
        )
    }

    override fun render(layer: CampaignEngineLayers, viewport: ViewportAPI?) {
        try {
            if (entity == null || entity.isExpired || params == null || cloudInstances.isNullOrEmpty()) return
            val riftRenderer = ShifterRiftCloudRenderer()

            for (cloudInstance in cloudInstances!!) {
                if (cloudInstance.inLifeTime <= 0) continue

                val lifeTime = if (state == State.IN) cloudInstance.inLifeTime else cloudInstance.outLifeTime
                val t = easeInOutQuad(min(max(lifeTime, 0f), 1f))
                val center = Vector2f(params!!.x, params!!.y)
                var location = Vector2f(params!!.x + cloudInstance.x, params!!.y + cloudInstance.y)
                if (entity.facing != 0f) location = Misc.rotateAroundOrigin(location, entity.facing, center)

                riftRenderer.addRequest(
                    ShifterRiftCloudRenderer.Request().apply {
                        this.location = lerp(center, location, 0.7f + (t * 0.3f))
                        this.scale = cloudInstance.size * t
                        this.angle = cloudInstance.angle
                    }
                )
            }

            when (layer) {
                CampaignEngineLayers.TERRAIN_6B -> riftRenderer.renderFringe()
                CampaignEngineLayers.BELOW_STATIONS -> riftRenderer.renderCore()
                else -> {}
            }
            riftRenderer.clear()
        } catch (_: Exception) {
        }
    }

    companion object {
        @JvmOverloads
        fun create(
            location: LocationAPI?,
            x: Float,
            y: Float,
            radius: Float,
            duration: Float = -1f
        ): ShifterRiftCloud? {
            val params = Params(x, y, radius, duration)
            val entity = if (location != null) {
                spawnEntity(location, x, y, params)
            } else {
                spawnEntity(Global.getSector().hyperspace, x, y, params).also {
                    it.containingLocation?.removeEntity(it)
                }
            }
            return entity.customPlugin as? ShifterRiftCloud
        }

        private fun spawnEntity(location: LocationAPI, x: Float, y: Float, params: Params?): SectorEntityToken {
            return location.addCustomEntity(
                null, null, WOECustomEntities.SHIFTER_RIFT_CLOUD, Factions.NEUTRAL, params
            ).apply {
                setLocation(x, y)
                facing = Misc.random.nextFloat() * 360
            }
        }
    }
}