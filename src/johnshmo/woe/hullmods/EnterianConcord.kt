package johnshmo.woe.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import johnshmo.woe.WOESettings
import kotlin.math.roundToInt

class EnterianConcord : BaseHullMod() {

    override fun applyEffectsBeforeShipCreation(hullSize: ShipAPI.HullSize, stats: MutableShipStatsAPI?, id: String) {
        if (stats == null)
            return

        val armorBonus = when (hullSize) {
            ShipAPI.HullSize.FRIGATE -> WOESettings.enterianConcordArmorBonuses[0]
            ShipAPI.HullSize.DESTROYER -> WOESettings.enterianConcordArmorBonuses[1]
            ShipAPI.HullSize.CRUISER -> WOESettings.enterianConcordArmorBonuses[2]
            ShipAPI.HullSize.CAPITAL_SHIP -> WOESettings.enterianConcordArmorBonuses[3]
            else -> 0f
        }
        val fluxBonusPercent = WOESettings.enterianConcordFluxBonusPercent
        val ammoCapacityBonusPercent = WOESettings.enterianConcordAmmoCapacityBonusPercent
        val zeroFluxSpeedPenaltyMult = WOESettings.enterianConcordZeroFluxSpeedPenaltyMult

        stats.armorBonus.modifyFlat(id, armorBonus)
        stats.fluxCapacity.modifyPercent(id, fluxBonusPercent)
        stats.fluxDissipation.modifyPercent(id, fluxBonusPercent)
        stats.ballisticAmmoBonus.modifyPercent(id, ammoCapacityBonusPercent)
        stats.energyAmmoBonus.modifyPercent(id, ammoCapacityBonusPercent)
        stats.missileAmmoBonus.modifyPercent(id, ammoCapacityBonusPercent)
        stats.zeroFluxSpeedBoost.modifyMult(id, zeroFluxSpeedPenaltyMult)
    }

    override fun getDescriptionParam(index: Int, hullSize: ShipAPI.HullSize?): String? {
        return when (index) {
            in 0..3 -> "${WOESettings.enterianConcordArmorBonuses[index].roundToInt()}"
            4 -> "${WOESettings.enterianConcordFluxBonusPercent.roundToInt()}%"
            5 -> "${WOESettings.enterianConcordAmmoCapacityBonusPercent.roundToInt()}%"
            6 -> "${(100f - WOESettings.enterianConcordZeroFluxSpeedPenaltyMult * 100f).roundToInt()}%"
            else -> super.getDescriptionParam(index, hullSize)
        }
    }
}