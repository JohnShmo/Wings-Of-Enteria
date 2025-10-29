package johnshmo.woe

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import johnshmo.woe.generation.NewEnteria
import johnshmo.woe.generation.StarSystem

class WOEGenerator {
    private val memory: MemoryAPI = Global.getFactory().createMemory()

    fun initialize() {
        WOEGlobal.logger.info("Generating star systems...")
        newEnteria.initialize()
        WOEGlobal.logger.info("Star systems generated.")
    }

    val newEnteria: NewEnteria
        get() {
            return getOrMakeStarSystem("\$newEnteria") { NewEnteria() } as NewEnteria
        }

    private fun getOrMakeStarSystem(id: String, factory: () -> StarSystem): StarSystem {
        if (!memory.contains(id)) {
            memory.set(id, factory())
        }
        return (memory.get(id) as StarSystem)
    }
}
