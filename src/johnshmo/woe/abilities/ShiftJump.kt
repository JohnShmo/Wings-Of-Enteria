package johnshmo.woe.abilities

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.characters.AbilityPlugin
import com.fs.starfarer.api.impl.campaign.abilities.BaseAbilityPlugin
import com.fs.starfarer.api.impl.campaign.abilities.EmergencyBurnAbility
import com.fs.starfarer.api.impl.campaign.abilities.FractureJumpAbility
import com.fs.starfarer.api.impl.campaign.abilities.GenerateSlipsurgeAbility
import com.fs.starfarer.api.impl.campaign.abilities.GoDarkAbility
import com.fs.starfarer.api.impl.campaign.abilities.SustainedBurnAbility
import com.fs.starfarer.api.impl.campaign.abilities.TransponderAbility
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.util.Misc
import johnshmo.woe.*
import johnshmo.woe.abilities.shift_jump.ShiftJumpStateCharge
import johnshmo.woe.abilities.shift_jump.ShiftJumpStateCooldown
import johnshmo.woe.abilities.shift_jump.ShiftJumpStateFinished
import johnshmo.woe.abilities.shift_jump.ShiftJumpStateInactive
import johnshmo.woe.abilities.shift_jump.ShiftJumpStateJumping
import johnshmo.woe.abilities.shift_jump.ShiftJumpStateReady
import johnshmo.woe.abilities.shift_jump.ShiftJumpStateSelectingTarget
import johnshmo.woe.utils.StateMachine
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import java.util.*
import kotlin.math.PI
import kotlin.math.abs
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

    private var data: MutableMap<String, Any> = HashMap()

    // Persistent Data Section =========================================================================================

    @Suppress("UNCHECKED_CAST")
    internal val stateMachine: StateMachine<State>
        get() = data.getOrPut("stateMachine") {
            val sm = StateMachine<State>()
            sm.registerState(State.INACTIVE, ShiftJumpStateInactive(this))
            sm.registerState(State.CHARGING, ShiftJumpStateCharge(this))
            sm.registerState(State.READY, ShiftJumpStateReady(this))
            sm.registerState(State.SELECTING_TARGET, ShiftJumpStateSelectingTarget(this))
            sm.registerState(State.JUMPING, ShiftJumpStateJumping(this))
            sm.registerState(State.FINISHED, ShiftJumpStateFinished(this))
            sm.registerState(State.COOLDOWN, ShiftJumpStateCooldown(this))
            sm.state = State.INACTIVE
            sm
        } as StateMachine<State>

    internal var state: State
        get() = stateMachine.state ?: State.INACTIVE
        private set(value) {
            stateMachine.state = value
        }

    internal var chargeAmountDays: Float
        get() = data.getOrPut("chargeAmountDays") { 0.0f } as Float
        set(value) {
            data["chargeAmountDays"] = value
        }

    internal var cooldownDays: Float
        get() = data.getOrPut("cooldownDays") { 0.0f } as Float
        set(value) {
            data["cooldownDays"] = value
        }

    internal var pickedTarget: SectorEntityToken?
        get() = data["pickedTarget"] as SectorEntityToken?
        set(value) {
            if (value == null) {
                data.remove("pickedTarget")
                return
            }
            data["pickedTarget"] = value
        }

    internal var initialChargeCostPerDay: Float
        get() = data.getOrPut("initialChargeCostPerDay") { 0.0f } as Float
        set(value) {
            data["initialChargeCostPerDay"] = value
        }

    // Transient Data Section ==========================================================================================
    @Transient
    private var _cachedChargeCostValue: Float? = null
    private var cachedChargeCostValue: Float
        get() {
            if (_cachedChargeCostValue == null) {
                _cachedChargeCostValue = 0.0f
            }
            return _cachedChargeCostValue!!
        }
        set(value) {
            _cachedChargeCostValue = value
        }

    @Transient
    private var _cachedChargeCost: Boolean? = null
    private var cachedChargeCost: Boolean
        get() {
            if (_cachedChargeCost == null) {
                _cachedChargeCost = false
            }
            return _cachedChargeCost!!
        }
        set(value) {
                _cachedChargeCost = value
        }

    @Transient
    private var _blinking: Boolean? = null
    internal var blinking: Boolean
        get() {
            if (_blinking == null) {
                _blinking = false
            }
            return _blinking!!
        }
        set(value) {
            _blinking = value
        }

    @Transient
    private var _blinkTimer: Float? = null
    private var blinkTimer: Float
        get() {
            if (_blinkTimer == null) {
                _blinkTimer = 0.0f
            }
            return _blinkTimer!!
        }
        set(value) {
            _blinkTimer = value
        }

    @Transient
    private var _blinkValue: Float? = null
    private var blinkValue: Float
        get() {
            if (_blinkValue == null) {
                _blinkValue = 0.0f
            }
            return _blinkValue!!
        }
        set(value) {
            _blinkValue = value
        }

    @Transient
    private var _blinkIntensity: InterpolatedFloat? = null
    private var blinkIntensity: InterpolatedFloat
        get() {
            if (_blinkIntensity == null) {
                _blinkIntensity = InterpolatedFloat(0.0f)
            }
            return _blinkIntensity!!
        }
        set(value) {
            _blinkIntensity = value
        }

    // Methods =========================================================================================================

    fun showFloatingText(text: String, color: Color) {
        if (!fleet.isInCurrentLocation) return
        fleet.addFloatingText(text, color, 0.5f)
    }

    fun getValidDestinationList(): List<SectorEntityToken> {
        val allSystems: List<StarSystemAPI> = Global.getSector().starSystems
        val validDestinations: MutableList<SectorEntityToken> = mutableListOf()
        for (system in allSystems) {
            if (system == fleet.containingLocation) continue
            if (Misc.getDistanceLY(fleet.locationInHyperspace, system.location) <= WOESettings.shiftJumpMaxRangeLY) {
                if (system.star != null) {
                    validDestinations.add(system.star!!)
                }
            }
        }
        return validDestinations
    }

    fun computeFuelCost(target: SectorEntityToken): Int {
        val maxRangeLY = WOESettings.shiftJumpMaxRangeLY
        val distanceLY = Misc.getDistanceLY(fleet, target)
        val t = inverseLerp(0f, maxRangeLY, distanceLY)
        val costMultiplier = lerp(WOESettings.shiftJumpMinFuelCostMultiplier, WOESettings.shiftJumpMaxFuelCostMultiplier, t)
        val regularCost = fleet.logistics.fuelCostPerLightYear * distanceLY
        return (regularCost * costMultiplier).toInt()
    }

    fun computeCRCost(target: SectorEntityToken): Float {
        val maxRangeLY = WOESettings.shiftJumpMaxRangeLY
        val distanceLY = Misc.getDistanceLY(fleet, target)
        val t = inverseLerp(0f, maxRangeLY, distanceLY)
        return lerp(WOESettings.shiftJumpMinCRCost, WOESettings.shiftJumpMaxCRCost, easeInCubic(t))
    }

    fun getMaxRangeLY(): Int {
        return WOESettings.shiftJumpMaxRangeLY.toInt()
    }

    override fun advance(amount: Float) {
        super.advance(amount)
        cachedChargeCost = false
        if (fleet == null) {
            return
        }
        advanceDeactivationBlink(amount)
        stateMachine.advance(amount)
        if (state != State.INACTIVE) {
            interruptIncompatible()
            disableIncompatible()
        }
    }

    override fun isCompatible(other: AbilityPlugin?): Boolean {
        val currentState = state
        if (currentState == State.CHARGING) {
            return when (other) {
                is SustainedBurnAbility -> false
                is EmergencyBurnAbility -> false
                is FractureJumpAbility -> false
                is GenerateSlipsurgeAbility -> false
                is GoDarkAbility -> false
                else -> true
            }
        }
        if (currentState == State.JUMPING) {
            return when (other) {
                is TransponderAbility -> true
                else -> false
            }
        }
        return true
    }

    private fun advanceDeactivationBlink(amount: Float) {
        if (blinking) {
            blinkIntensity.set(1.0f, 0.25f)
            blinkTimer -= amount
            if (blinkTimer <= 0.0f) {
                blinking = false
            }
        } else {
            blinkIntensity.set(0.0f, 0.25f)
        }
        blinkValue += BLINK_SPEED * amount
        while (blinkValue >= PI.toFloat() * 2.0f) {
            blinkValue -= PI.toFloat() * 2.0f
        }
        blinkIntensity.advance(amount)
    }

    override fun setCooldownLeft(days: Float) {
        cooldownDays = days
    }

    override fun getCooldownLeft(): Float {
        return cooldownDays
    }

    override fun getSpriteName(): String {
        return when (state) {
            State.INACTIVE -> Global.getSettings().getSpriteName(ICON_CATEGORY, INACTIVE_ICON_SPRITE_NAME)
            State.CHARGING -> Global.getSettings().getSpriteName(ICON_CATEGORY, CHARGING_ICON_SPRITE_NAME)
            State.READY -> Global.getSettings().getSpriteName(ICON_CATEGORY, READY_ICON_SPRITE_NAME)
            else -> Global.getSettings().getSpriteName(ICON_CATEGORY, ICON_SPRITE_NAME)
        }
    }

    internal fun computeAndCacheChargeCostPerDay(): Float {
        if (cachedChargeCost) {
            return cachedChargeCostValue
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

    internal fun isChargeCostValid(chargeCost: Float): Boolean {
        return abs(chargeCost - initialChargeCostPerDay) <= CHARGE_COST_EPSILON
    }

    override fun activate() {
        if (state == State.INACTIVE) {
            super.activate()
            state = State.CHARGING
        }
    }

    override fun deactivate() {
        val currentState = state
        if (currentState == State.INACTIVE) {
            return
        }
        if (currentState == State.CHARGING || currentState == State.READY || currentState == State.SELECTING_TARGET || currentState == State.JUMPING) {
            super.deactivate()
            state = State.INACTIVE
        } else {
            state = State.COOLDOWN
        }
    }

    override fun getCooldownFraction(): Float {
        val currentState = state
        if (currentState == State.CHARGING) {
            val daysToCharge = WOESettings.shiftJumpChargeTimeDays
            return 1.0f - (chargeAmountDays / daysToCharge)
        }
        if (currentState == State.COOLDOWN) {
            val totalCooldownDays = WOESettings.shiftJumpCooldownDays
            return 1.0f - (cooldownDays / totalCooldownDays)
        }
        if (currentState == State.READY) {
            return 0.0f
        }
        return super.getCooldownFraction()
    }

    override fun getCooldownColor(): Color? {
        val currentState = state
        if (currentState == State.CHARGING) {
            val t: Float = ((sin(blinkValue) + 1.0f) * 0.5f) * blinkIntensity.value
            val blinkColor = DEACTIVATION_BLINK_COLOR
            val defaultColor = CHARGE_UP_COLOR
            return lerpColors(defaultColor, blinkColor, t)
        }
        if (currentState == State.READY) {
            val t: Float = ((sin(blinkValue) + 1.0f) * 0.5f) * blinkIntensity.value
            val blinkColor = Color(255, 255, 255, 0)
            val defaultColor = CHARGE_UP_COLOR
            return lerpColors(defaultColor, blinkColor, t)
        }
        return super.getCooldownColor()
    }

    override fun getActiveColor(): Color? {
        if (state == State.CHARGING) {
            val t: Float = ((sin(blinkValue) + 1.0f) * 0.5f) * blinkIntensity.value
            val blinkColor = DEACTIVATION_BLINK_COLOR
            val defaultColor = super.activeColor
            return lerpColors(defaultColor, blinkColor, t)
        }
        return super.getActiveColor()
    }

    override fun isOnCooldown(): Boolean {
        return state == State.COOLDOWN
    }

    override fun isCooldownRenderingAdditive(): Boolean {
        val currentState = state
        if (currentState == State.CHARGING || currentState == State.READY) {
            return true
        }
        return super.isCooldownRenderingAdditive()
    }

    override fun isUsable(): Boolean {
        if (disableFrames > 0) {
            return false
        }
        if (state == State.INACTIVE) {
            val chargeCostPerDay = WOESettings.shiftJumpChargeTransplutonicsPerDay
            val cargo = fleet.cargo
            val quantity = cargo.getCommodityQuantity(Commodities.RARE_METALS)
            if ((chargeCostPerDay > 0f) && (quantity <= 0f)) {
                return false
            }
        }
        return super.isUsable()
    }

    override fun showCooldownIndicator(): Boolean {
        val currentState = state
        return currentState == State.COOLDOWN || currentState == State.CHARGING || currentState == State.READY
    }

    override fun pressButton() {
        when (state) {
            State.CHARGING -> {
                if (!blinking) {
                    blinking = true
                    blinkTimer = BLINK_DURATION
                    playUISound(offSoundUI, 1.5f, 0.5f)
                    return
                }
                blinking = false
                deactivate()
                playUISound(offSoundUI)
            }
            State.INACTIVE -> {
                activate()
                playUISound(onSoundUI)
            }
            State.READY -> {
                playUISound(onSoundUI)
                state = State.SELECTING_TARGET
            }
            else -> {
                // Do nothing
            }
        }
    }

    private fun playUISound(soundId: String?, pitch: Float = 1f, volume: Float = 1f) {
        if (entity.isPlayerFleet && soundId != null) {
            if (PLAY_UI_SOUNDS_IN_WORLD_SOURCES) {
                Global.getSoundPlayer()
                    .playSound(soundId, pitch, volume, Global.getSoundPlayer().listenerPos, Vector2f())
            } else {
                Global.getSoundPlayer().playUISound(soundId, pitch, volume)
            }
        }
    }

    override fun isActive(): Boolean {
        val currentState = state
        return currentState != State.INACTIVE && currentState != State.COOLDOWN
    }

    override fun fleetJoinedBattle(battle: BattleAPI?) {
        deactivate()
        showFloatingText("Shift drive field destabilized", Misc.setAlpha(Misc.getNegativeHighlightColor(), 255))
    }

    companion object {
        internal const val BLINK_SPEED: Float = 5.0f
        internal const val BLINK_DURATION: Float = 5.0f
        internal const val CHARGE_COST_EPSILON: Float = 0.01f
        internal const val ICON_CATEGORY = "woe_abilities"
        internal const val ICON_SPRITE_NAME = "shift_jump"
        internal const val INACTIVE_ICON_SPRITE_NAME = "shift_jump_inactive"
        internal const val CHARGING_ICON_SPRITE_NAME = "shift_jump_charging"
        internal const val READY_ICON_SPRITE_NAME = "shift_jump_ready"
        internal val CHARGE_UP_COLOR: Color = Color(100, 250, 250, 150)
        internal val DEACTIVATION_BLINK_COLOR: Color = Color(255, 200, 0, 255)
    }
}