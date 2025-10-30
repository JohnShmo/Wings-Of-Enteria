package johnshmo.woe

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.ids.Factions
import exerelin.campaign.SectorManager
import johnshmo.woe.generation.Luminaru
import johnshmo.woe.generation.StarSystemGenerator


class WOEGenerator {
    private val data: MutableMap<String, Any> = HashMap()

    fun initialize() {
        generateStarSystems()
        generateFactions()
    }

    private fun generateStarSystems() {
        WOEGlobal.logger.info("Generating star systems...")
        val isNexerelinEnabled = Global.getSettings().modManager.isModEnabled("nexerelin")

        if (!isNexerelinEnabled || SectorManager.getManager().isCorvusMode) {
            luminaru.initialize()
            WOEGlobal.logger.info("Star systems generated.")
        } else {
            WOEGlobal.logger.info("Skipped star system generation for random sector.")
        }
    }

    private fun generateFactions() {
        WOEGlobal.logger.info("Generating factions...")
        val isNexerelinEnabled = Global.getSettings().modManager.isModEnabled("nexerelin")

        val generatedEnterianFragment = "generatedEnterianFragment"
        if (data[generatedEnterianFragment] == null) {
            data[generatedEnterianFragment] = true
            val enterianFragment = Global.getSector().getFaction(WOEFactions.ENTERIAN_FRAGMENT)

            // Relationships don't need to be set if Nex is handling it.
            if (!isNexerelinEnabled) {
                val pirates = Global.getSector().getFaction(Factions.PIRATES)
                val pathers = Global.getSector().getFaction(Factions.LUDDIC_PATH)
                val hegemony = Global.getSector().getFaction(Factions.HEGEMONY)
                val league = Global.getSector().getFaction(Factions.PERSEAN)
                val derelict = Global.getSector().getFaction(Factions.DERELICT)
                val remnant = Global.getSector().getFaction(Factions.REMNANTS)
                val omega = Global.getSector().getFaction(Factions.OMEGA)
                val threat = Global.getSector().getFaction(Factions.THREAT)
                val dweller = Global.getSector().getFaction(Factions.DWELLER)

                enterianFragment.setRelationship(pirates.id, -0.5f)
                enterianFragment.setRelationship(pathers.id, -0.5f)
                enterianFragment.setRelationship(hegemony.id, -0.5f)
                enterianFragment.setRelationship(league.id, 0.2f)
                enterianFragment.setRelationship(derelict.id, -0.5f)
                enterianFragment.setRelationship(remnant.id, -0.5f)
                enterianFragment.setRelationship(omega.id, -0.5f)
                enterianFragment.setRelationship(threat.id, -0.5f)
                enterianFragment.setRelationship(dweller.id, -1.0f)
            }
        }

        WOEGlobal.logger.info("Factions generated.")
    }

    val luminaru: Luminaru
        get() {
            return getOrMakeStarSystem("newEnteria") { Luminaru() } as Luminaru
        }

    private fun getOrMakeStarSystem(id: String, factory: () -> StarSystemGenerator): StarSystemGenerator {
        if (!data.contains(id)) {
            data[id] = factory()
        }
        return (data[id] as StarSystemGenerator)
    }
}
