package johnshmo.woe.generation

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin
import com.fs.starfarer.api.util.Misc
import johnshmo.woe.WOEGlobal


abstract class StarSystem {
    val system: StarSystemAPI
    protected val memory: MemoryAPI
    private var initialized: Boolean = false

    class Params(
        val id: String,
        val name: String,
        val hyperspaceLocationX: Float,
        val hyperspaceLocationY: Float,
        val centerStar: StarParams? = null
    )

    class StarParams(
        val starType: String,
        val starRadius: Float,
        val coronaSize: Float,
    )

    abstract val params: Params

    abstract fun initializeImpl()

    fun initialize() {
        WOEGlobal.logger.info("Initializing StarSystem: ${params.name}")

        if (!initialized) {
            system.optionalUniqueId = params.id

            if (params.centerStar != null) {
                system.initStar(
                    params.id + "_star",
                    params.centerStar?.starType,
                    params.centerStar?.starRadius as Float,
                    params.centerStar?.coronaSize as Float,
                )
            } else {
                system.initNonStarCenter()
            }
        }

        initializeImpl()

        if (!initialized) {
            system.location.set(params.hyperspaceLocationX, params.hyperspaceLocationY)
            system.generateAnchorIfNeeded()
            system.autogenerateHyperspaceJumpPoints(true, true)
            val plugin = Misc.getHyperspaceTerrain().plugin as HyperspaceTerrainPlugin
            val editor = NebulaEditor(plugin)
            val minRadius = plugin.tileSize * 2f
            val radius = system.maxRadiusInHyperspace
            editor.clearArc(system.location.x, system.location.y, 0f, radius + minRadius, 0f, 360f)
            editor.clearArc(system.location.x, system.location.y, 0f, radius + minRadius, 0f, 360f, 0.25f)
        }

        initialized = true
        WOEGlobal.logger.info("Finished initializing StarSystem: ${params.name}")
    }

    init {
        system = Global.getSector().createStarSystem(params.name)
        memory = Global.getFactory().createMemory()
    }
}