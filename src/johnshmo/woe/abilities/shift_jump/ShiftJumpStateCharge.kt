package johnshmo.woe.abilities.shift_jump

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.util.Misc
import johnshmo.woe.WOESettings
import johnshmo.woe.abilities.ShiftJump

class ShiftJumpStateCharge(shiftJump: ShiftJump): ShiftJumpState(shiftJump) {
    private var costFraction: Float
        get() = data.getOrPut("costFraction") { 0.0f } as Float
        set(value) {
            data["costFraction"] = value
        }

    override fun enter() {
        shiftJump.chargeAmountDays = 0.0f
        shiftJump.initialChargeCostPerDay = shiftJump.computeAndCacheChargeCostPerDay()
        shiftJump.showFloatingText("Charging shift drive...", Misc.setAlpha(shiftJump.fleet.indicatorColor, 255))
        shiftJump.fleet.stats.detectedRangeMod.modifyFlat("woe_shift_jump", WOESettings.shiftJumpSensorProfilePenalty, "Shift drive charging")
    }

    override fun exit() {
        shiftJump.fleet.stats.detectedRangeMod.unmodify("woe_shift_jump")
    }

    override fun advance(amount: Float): ShiftJump.State? {
        val daysElapsed = Global.getSector().clock.convertToDays(amount)
        val chargeCostPerDay = shiftJump.computeAndCacheChargeCostPerDay()
        if (!shiftJump.isChargeCostValid(chargeCostPerDay)) {
            shiftJump.deactivate()
            shiftJump.showFloatingText("Shift drive field destabilized", Misc.setAlpha(Misc.getNegativeHighlightColor(), 255))
            return null
        }

        val cargo = shiftJump.fleet.cargo
        val quantity = cargo.getCommodityQuantity(Commodities.RARE_METALS)
        costFraction += chargeCostPerDay * daysElapsed

        var toConsume = 0f
        while (costFraction >= 1.0f) {
            toConsume += 1.0f
            costFraction -= 1.0f
        }
        if (quantity <= 0.0f || toConsume > quantity) {
            shiftJump.deactivate()
            shiftJump.showFloatingText("Ran out of transplutonics", Misc.setAlpha(Misc.getNegativeHighlightColor(), 255))
            return null
        }
        cargo.removeCommodity(Commodities.RARE_METALS, toConsume)

        val daysToCharge = WOESettings.shiftJumpChargeTimeDays
        shiftJump.chargeAmountDays += daysElapsed
        if (shiftJump.chargeAmountDays >= daysToCharge) {
            return ShiftJump.State.READY
        }

        return ShiftJump.State.CHARGING
    }
}