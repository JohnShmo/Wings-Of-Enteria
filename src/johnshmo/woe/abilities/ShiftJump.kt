package johnshmo.woe.abilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.impl.campaign.abilities.BaseAbilityPlugin
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import johnshmo.woe.WOESettings
import java.awt.Color

class ShiftJump : BaseAbilityPlugin() {
    enum class State {
        INACTIVE,
        CHARGING,
        READY,
        SELECTING_TARGET,
        JUMPING,
        FINISHED,
        COOLDOWN,
    }

    var state = State.INACTIVE
    var chargeAmountDays = 0.0f
    var cooldownDays = 0.0f
    var costFraction = 0.0f
    var ranOutOfTransplutonics = false

    override fun advance(amount: Float) {
        if (Global.getSector().isPaused) {
            return
        }

        state = when (state) {
            State.INACTIVE -> advanceInactive()
            State.CHARGING -> advanceCharging(amount)
            State.READY -> TODO()
            State.SELECTING_TARGET -> TODO()
            State.JUMPING -> TODO()
            State.FINISHED -> TODO()
            State.COOLDOWN -> TODO()
        }
    }

    override fun setCooldownLeft(days: Float) {
        cooldownDays = days
    }

    override fun getCooldownLeft(): Float {
        return cooldownDays
    }

    override fun getSpriteName(): String? {
        return super.getSpriteName()
    }

    private fun advanceInactive(): State {
        chargeAmountDays = 0.0f
        cooldownDays = 0.0f
        costFraction = 0.0f
        ranOutOfTransplutonics = false
        if (isActive) {
            return State.CHARGING
        }
        return State.INACTIVE
    }

    private fun advanceCharging(amount: Float): State {
        val daysElapsed = Global.getSector().clock.convertToDays(amount)

        val chargeCostPerDay = WOESettings.shiftJumpChargeTransplutonicsPerDay
        val cargo = fleet.cargo
        val quantity = cargo.getCommodityQuantity(Commodities.RARE_METALS)
        costFraction += chargeCostPerDay * daysElapsed

        var toConsume = 0f
        while (costFraction >= 1.0f) {
            toConsume += 1.0f
            costFraction -= 1.0f
        }
        if (quantity < toConsume) {
            ranOutOfTransplutonics = true
            deactivate()
            return State.INACTIVE
        }
        cargo.removeCommodity(Commodities.RARE_METALS, toConsume)

        val daysToCharge = WOESettings.shiftJumpChargeTimeDays
        chargeAmountDays += daysElapsed
        if (chargeAmountDays >= daysToCharge) {
            return State.READY
        }
        return State.CHARGING
    }

    override fun activate() {
        if (state == State.INACTIVE) {
            super.activate()
        }
    }

    override fun deactivate() {
        if (state == State.CHARGING || state == State.SELECTING_TARGET || state == State.FINISHED ) {
            super.deactivate()
        }
    }

    override fun getDeactivationText(): String? {
        if (ranOutOfTransplutonics) {
            return "Out of transplutonics"
        }
        if (state == State.SELECTING_TARGET) {
            return "Canceled jump"
        }
        return super.getDeactivationText()
    }

    override fun getCooldownFraction(): Float {
        if (state == State.CHARGING) {
            val daysToCharge = WOESettings.shiftJumpChargeTimeDays
            return chargeAmountDays / daysToCharge
        }
        if (state == State.COOLDOWN) {
            val totalCooldownDays = WOESettings.shiftJumpCooldownDays
            return cooldownDays / totalCooldownDays
        }
        return super.getCooldownFraction()
    }

    override fun getCooldownColor(): Color? {
        if (state == State.CHARGING) {
            return Color(50, 100, 100, 171)
        }
        return super.getCooldownColor()
    }

    override fun isOnCooldown(): Boolean {
        return state == State.COOLDOWN
    }

    override fun isUsable(): Boolean {
        val chargeCostPerDay = WOESettings.shiftJumpChargeTransplutonicsPerDay
        val cargo = fleet.cargo
        val quantity = cargo.getCommodityQuantity(Commodities.RARE_METALS)
        if ((chargeCostPerDay > 0f) && (quantity <= 0f)) {
            return false
        }
        return super.isUsable()
    }
}