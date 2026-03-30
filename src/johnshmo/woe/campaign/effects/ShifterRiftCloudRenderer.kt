package johnshmo.woe.campaign.effects

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f

class ShifterRiftCloudRenderer {
    class Request {
        var location: Vector2f = Vector2f()
        var angle: Float = 0f
        var scale: Float = 0f
    }

    @Transient
    private val requests: MutableList<Request> = ArrayList<Request>()

    fun addRequest(request: Request?) {
        requests.add(request!!)
    }

    fun renderFringe() {
        if (requests.isEmpty()) return

        val fringeSprite = Global.getSettings().getSprite("woe_fx", FRINGE_SPRITE_ID)
        val baseWidth = fringeSprite.width
        val baseHeight = fringeSprite.height
        fringeSprite.setAdditiveBlend()

        for (request in requests) {
            val x = request.location.x + (Misc.random.nextFloat() - 0.5f) * 2
            val y = request.location.y + (Misc.random.nextFloat() - 0.5f) * 2
            fringeSprite.setSize(
                request.scale * (baseWidth + (Misc.random.nextFloat() - 0.5f) * (baseWidth * 0.02f)),
                request.scale * (baseHeight + (Misc.random.nextFloat() - 0.5f) * (baseHeight * 0.02f))
            )
            fringeSprite.setAngle(request.angle)
            fringeSprite.renderAtCenter(x, y)
        }

        requests.clear()
    }

    fun renderCore() {
        if (requests.isEmpty()) return

        val coreSprite = Global.getSettings().getSprite("woe_fx", CORE_SPRITE_ID)
        val baseWidth = coreSprite.width
        val baseHeight = coreSprite.height

        for (request in requests) {
            val x = request.location.x + (Misc.random.nextFloat() - 0.5f) * 0.5f
            val y = request.location.y + (Misc.random.nextFloat() - 0.5f) * 0.5f
            coreSprite.setSize(
                request.scale * (baseWidth + (Misc.random.nextFloat() - 0.5f) * (baseWidth * 0.01f)),
                request.scale * (baseHeight + (Misc.random.nextFloat() - 0.5f) * (baseHeight * 0.01f))
            )
            coreSprite.angle = request.angle
            coreSprite.renderAtCenter(x, y)
        }

        requests.clear()
    }

    companion object {
        var CORE_SPRITE_ID: String = "shifter_rift_core"
        var FRINGE_SPRITE_ID: String = "shifter_rift_fringe"
    }
}
