package johnshmo.woe.generation

import com.fs.starfarer.api.impl.campaign.ids.StarTypes

class NewEnteria : StarSystem() {
    override val params: Params
        get() = Params(
            "woe_new_enteria",
            "New Enteria",
            2500f,
            -7400f,
            StarParams(
                StarTypes.BROWN_DWARF,
                700f,
                100f
            )
        )

    override fun initializeImpl() {

    }
}