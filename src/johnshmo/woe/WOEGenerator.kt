package johnshmo.woe

import com.fs.starfarer.api.Global
import exerelin.campaign.SectorManager
import johnshmo.woe.generation.NewEnteria
import johnshmo.woe.generation.StarSystemGenerator


class WOEGenerator {
    private val data: MutableMap<String, Any> = HashMap()

    fun initialize() {
        WOEGlobal.logger.info("Generating star systems...")
        val isNexerelinEnabled = Global.getSettings().modManager.isModEnabled("nexerelin")
        if (!isNexerelinEnabled || SectorManager.getManager().isCorvusMode) {
            newEnteria.initialize()
            WOEGlobal.logger.info("Star systems generated.")
        } else {
            WOEGlobal.logger.info("Skipped star system generation for random sector.")
        }
    }

    val newEnteria: NewEnteria
        get() {
            return getOrMakeStarSystem("newEnteria") { NewEnteria() } as NewEnteria
        }

    private fun getOrMakeStarSystem(id: String, factory: () -> StarSystemGenerator): StarSystemGenerator {
        if (!data.contains(id)) {
            data[id] = factory()
        }
        return (data[id] as StarSystemGenerator)
    }
}
