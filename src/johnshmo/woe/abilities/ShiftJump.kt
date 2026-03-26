package johnshmo.woe.abilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.abilities.BaseAbilityPlugin
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import johnshmo.woe.InterpolatedFloat
import johnshmo.woe.WOESettings
import johnshmo.woe.lerpColors
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.PI
import kotlin.math.sin

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

    private var state = State.INACTIVE
    private var chargeAmountDays = 0.0f
    private var cooldownDays = 0.0f
    private var costFraction = 0.0f
    private var ranOutOfTransplutonics = false

    @Transient
    private var cachedChargeCostValue: Float? = null
    @Transient
    private var cachedChargeCost: Boolean? = null
    @Transient
    private var aboutToDeactivate: Boolean? = null
    @Transient
    private var aboutToDeactivateTimer: Float? = null
    @Transient
    private var blinkValue: Float? = null
    @Transient
    private var blinkIntensity: InterpolatedFloat? = null

    override fun advance(amount: Float) {
        cachedChargeCost = false

        if (Global.getSector().isPaused) {
            return
        }
        if (fleet == null) {
            return
        }

        state = when (state) {
            State.INACTIVE -> advanceInactive()
            State.CHARGING -> advanceCharging(amount)
            State.READY -> State.INACTIVE // TODO
            State.SELECTING_TARGET -> State.INACTIVE // TODO
            State.JUMPING -> State.INACTIVE // TODO
            State.FINISHED -> State.INACTIVE // TODO
            State.COOLDOWN -> State.INACTIVE // TODO
        }

        ensureBlinkTransients()
        if (aboutToDeactivate != null && aboutToDeactivate!!) {
            blinkIntensity!!.set(1.0f, 0.25f)
            if (aboutToDeactivateTimer != null) {
                aboutToDeactivateTimer = aboutToDeactivateTimer!! - amount
                if (aboutToDeactivateTimer!! <= 0.0f) {
                    aboutToDeactivate = false
                }
            }
        } else {
            blinkIntensity!!.set(0.0f, 0.25f)
        }
        blinkValue = blinkValue!! + BLINK_SPEED * amount
        while (blinkValue!! >= PI.toFloat() * 2.0f) {
            blinkValue = blinkValue!! - PI.toFloat() * 2.0f
        }
        blinkIntensity!!.advance(amount)
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

    private fun calculateAndCacheChargeCostPerDay(): Float {
        if (cachedChargeCost != null && cachedChargeCost!!) {
            return cachedChargeCostValue!!
        }
        val chargeCostPerDayMult = WOESettings.shiftJumpChargeTransplutonicsPerDay
        var chargeCostPerDayTotal = 0.0f
        val fleetMembers = fleet.fleetData.membersListCopy
        for (member in fleetMembers) {
            chargeCostPerDayTotal += member.deploymentPointsCost * chargeCostPerDayMult
        }
        cachedChargeCostValue = chargeCostPerDayTotal
        cachedChargeCost = true
        return chargeCostPerDayTotal
    }

    private fun advanceInactive(): State {
        chargeAmountDays = 0.0f
        cooldownDays = 0.0f
        costFraction = 0.0f
        ranOutOfTransplutonics = false
        aboutToDeactivate = false
        return State.INACTIVE
    }

    private fun advanceCharging(amount: Float): State {
        val daysElapsed = Global.getSector().clock.convertToDays(amount)
        val chargeCostPerDay = calculateAndCacheChargeCostPerDay()

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
            state = State.CHARGING
        }
    }

    override fun deactivate() {
        if (state == State.INACTIVE) {
            return
        }
        if (state == State.CHARGING || state == State.SELECTING_TARGET) {
            super.deactivate()
            state = State.INACTIVE
        } else {
            state = State.COOLDOWN
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
            return 1.0f - (chargeAmountDays / daysToCharge)
        }
        if (state == State.COOLDOWN) {
            val totalCooldownDays = WOESettings.shiftJumpCooldownDays
            return cooldownDays / totalCooldownDays
        }
        return super.getCooldownFraction()
    }

    override fun getCooldownColor(): Color? {
        if (state == State.CHARGING) {
            ensureBlinkTransients()
            val t: Float = ((sin(blinkValue!!) + 1.0f) * 0.5f) * blinkIntensity!!.value
            val color = Color(255, 200, 0, 255)
            val defaultColor = Color(100, 250, 250, 150)
            return lerpColors(defaultColor, color, t)
        }
        return super.getCooldownColor()
    }

    private fun ensureBlinkTransients() {
        if (blinkIntensity == null) {
            blinkIntensity = InterpolatedFloat(0.0f)
        }
        if (blinkValue == null) {
            blinkValue = 0f
        }
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

    override fun showCooldownIndicator(): Boolean {
        if (state == State.COOLDOWN || state == State.CHARGING) {
            return true
        }
        return super.showCooldownIndicator()
    }

    override fun pressButton() {
        if (state == State.CHARGING) {
            if (aboutToDeactivate == null || aboutToDeactivate == false) {
                aboutToDeactivate = true
                aboutToDeactivateTimer = BLINK_DURATION
                val soundId = offSoundUI
                if (entity.isPlayerFleet && soundId != null) {
                    if (PLAY_UI_SOUNDS_IN_WORLD_SOURCES) {
                        Global.getSoundPlayer()
                            .playSound(soundId, 1.5f, 0.5f, Global.getSoundPlayer().listenerPos, Vector2f())
                    } else {
                        Global.getSoundPlayer().playUISound(soundId, 1.5f, 0.5f)
                    }
                }
                return
            }
            aboutToDeactivate = false

            deactivate()
            val soundId = offSoundUI
            if (entity.isPlayerFleet && soundId != null) {
                if (PLAY_UI_SOUNDS_IN_WORLD_SOURCES) {
                    Global.getSoundPlayer()
                        .playSound(soundId, 1f, 1f, Global.getSoundPlayer().listenerPos, Vector2f())
                } else {
                    Global.getSoundPlayer().playUISound(soundId, 1f, 1f)
                }
            }
        } else if (state == State.INACTIVE) {
            activate()
            val soundId = onSoundUI
            if (entity.isPlayerFleet && soundId != null) {
                if (PLAY_UI_SOUNDS_IN_WORLD_SOURCES) {
                    Global.getSoundPlayer()
                        .playSound(soundId, 1f, 1f, Global.getSoundPlayer().listenerPos, Vector2f())
                } else {
                    Global.getSoundPlayer().playUISound(soundId, 1f, 1f)
                }
            }
        }
    }

    override fun isActive(): Boolean {
        return state != State.INACTIVE && state != State.COOLDOWN
    }

    override fun getActiveColor(): Color? {
        if (state == State.CHARGING) {
            ensureBlinkTransients()
            val t: Float = ((sin(blinkValue!!) + 1.0f) * 0.5f) * blinkIntensity!!.value
            val color = Color(255, 200, 0, 255)
            val defaultColor = super.activeColor
            return lerpColors(defaultColor, color, t)
        }
        return super.getActiveColor()
    }

    companion object {
        private const val BLINK_SPEED: Float = 5.0f
        private const val BLINK_DURATION: Float = 5.0f
    }
}