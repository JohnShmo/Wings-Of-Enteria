package johnshmo.woe.campaign.abilities

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEntityPickerListener
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.AbilityPlugin
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.abilities.BaseAbilityPlugin
import com.fs.starfarer.api.impl.campaign.abilities.EmergencyBurnAbility
import com.fs.starfarer.api.impl.campaign.abilities.FractureJumpAbility
import com.fs.starfarer.api.impl.campaign.abilities.GenerateSlipsurgeAbility
import com.fs.starfarer.api.impl.campaign.abilities.GoDarkAbility
import com.fs.starfarer.api.impl.campaign.abilities.SustainedBurnAbility
import com.fs.starfarer.api.impl.campaign.abilities.TransponderAbility
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import johnshmo.woe.*
import johnshmo.woe.campaign.entities.ShifterRiftCloud
import johnshmo.woe.utils.StateInterface
import johnshmo.woe.utils.StateMachine
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import java.util.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
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
    private val stateMachine: StateMachine<State>
        get() = data.getOrPut("stateMachine") {
            val sm = StateMachine<State>()
            sm.registerState(State.INACTIVE, InactiveState(this))
            sm.registerState(State.CHARGING, ChargeState(this))
            sm.registerState(State.READY, ReadyState(this))
            sm.registerState(State.SELECTING_TARGET, SelectingTargetState(this))
            sm.registerState(State.JUMPING, JumpingState(this))
            sm.registerState(State.FINISHED, FinishedState(this))
            sm.registerState(State.COOLDOWN, CooldownState(this))
            sm.state = State.INACTIVE
            sm
        } as StateMachine<State>

    private var state: State
        get() = stateMachine.state ?: State.INACTIVE
        set(value) {
            stateMachine.state = value
        }

    private var chargeAmountDays: Float
        get() = data.getOrPut("chargeAmountDays") { 0.0f } as Float
        set(value) {
            data["chargeAmountDays"] = value
        }

    private var cooldownDays: Float
        get() = data.getOrPut("cooldownDays") { 0.0f } as Float
        set(value) {
            data["cooldownDays"] = value
        }

    private var pickedTarget: SectorEntityToken?
        get() = data["pickedTarget"] as SectorEntityToken?
        set(value) {
            if (value == null) {
                data.remove("pickedTarget")
                return
            }
            data["pickedTarget"] = value
        }

    private var initialChargeCostPerDay: Float
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
    private var blinking: Boolean
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

    private fun computeAndCacheChargeCostPerDay(): Float {
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

    private fun isChargeCostValid(chargeCost: Float): Boolean {
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
        private const val BLINK_SPEED: Float = 5.0f
        private const val BLINK_DURATION: Float = 5.0f
        private const val CHARGE_COST_EPSILON: Float = 0.01f
        private const val ICON_CATEGORY = "woe_abilities"
        private const val ICON_SPRITE_NAME = "shift_jump"
        private const val INACTIVE_ICON_SPRITE_NAME = "shift_jump_inactive"
        private const val CHARGING_ICON_SPRITE_NAME = "shift_jump_charging"
        private const val READY_ICON_SPRITE_NAME = "shift_jump_ready"
        private val CHARGE_UP_COLOR: Color = Color(100, 250, 250, 150)
        private val DEACTIVATION_BLINK_COLOR: Color = Color(255, 200, 0, 255)
    }

    // Helper Classes ==================================================================================================

    private class DestinationPicker : InteractionDialogPlugin {
        @Transient
        private var shiftJump: ShiftJump? = null

        @Transient
        private var dialog: InteractionDialogAPI? = null

        override fun init(dialog: InteractionDialogAPI?) {
            this.dialog = dialog
            this.dialog!!.showCampaignEntityPicker(
                "Select destination", "Destination:", "Initiate Shift Jump",
                Global.getSector().playerFaction,
                this.shiftJump?.getValidDestinationList(),
                DestinationPickerListener(this.dialog, this.shiftJump)
            )
            unsetFields()
        }

        override fun optionSelected(optionText: String?, optionData: Any?) {
        }

        override fun optionMousedOver(optionText: String?, optionData: Any?) {
        }

        override fun advance(amount: Float) {
        }

        override fun backFromEngagement(battleResult: EngagementResultAPI?) {
            // I sure hope we don't end up here somehow...
        }

        override fun getContext(): Any? {
            return null
        }

        override fun getMemoryMap(): MutableMap<String?, MemoryAPI?>? {
            return null
        }

        private fun unsetFields() {
            this.shiftJump = null
            this.dialog = null
        }

        companion object {
            fun execute(shiftJump: ShiftJump) {
                val ui = Global.getSector().campaignUI
                val picker = DestinationPicker()
                picker.shiftJump = shiftJump
                ui.showInteractionDialog(picker, null)
            }
        }
    }

    private class DestinationPickerListener(
        @field:Transient private var dialog: InteractionDialogAPI?,
        @field:Transient private var shiftJump: ShiftJump?
    ) : BaseCampaignEntityPickerListener() {
        @Transient
        private var playerFleet: CampaignFleetAPI?

        init {
            playerFleet = Global.getSector().playerFleet
        }

        override fun getMenuItemNameOverrideFor(entity: SectorEntityToken?): String? {
            return null
        }

        override fun pickedEntity(entity: SectorEntityToken?) {
            shiftJump?.pickedTarget = entity
            dialog?.dismiss()
            unsetFields()
            Global.getSector().isPaused = false
        }

        override fun cancelledEntityPicking() {
            dialog?.dismiss()
            unsetFields()
            Global.getSector().isPaused = false
        }

        override fun getSelectedTextOverrideFor(entity: SectorEntityToken): String {
            return entity.name + " - " + entity.containingLocation.nameWithTypeShort
        }

        override fun createInfoText(info: TooltipMakerAPI, entity: SectorEntityToken) {
            if (shiftJump == null || playerFleet == null) return

            val cost: Int = shiftJump!!.computeFuelCost(entity)
            val crPenalty = (shiftJump!!.computeCRCost(entity).times(100f)).toInt()
            val available = playerFleet!!.cargo.fuel.toInt()
            val maxRange: Int = shiftJump!!.getMaxRangeLY()
            val distance = Misc.getDistanceLY(playerFleet!!, entity).toInt()
            val supplyCost: Float = computeSupplyCostForCRRecovery(playerFleet!!, shiftJump!!.computeCRCost(entity))

            var requiredFuelColor = Misc.getHighlightColor()
            val highlightColor = Misc.getHighlightColor()
            if (cost > available) {
                requiredFuelColor = Misc.getNegativeHighlightColor()
            }

            info.setParaSmallInsignia()

            info.beginGrid(200f, 3, Misc.getGrayColor())
            info.setGridFontSmallInsignia()
            info.addToGrid(0, 0, "    Maximum range (LY):", maxRange.toString(), highlightColor)
            info.addToGrid(1, 0, " |  Fuel available:", Misc.getWithDGS(available.toFloat()), highlightColor)
            if (crPenalty > 0) info.addToGrid(2, 0, " |  CR penalty:", "$crPenalty%", highlightColor)
            info.addGrid(0f)

            info.beginGrid(200f, 3, Misc.getGrayColor())
            info.setGridFontSmallInsignia()
            info.addToGrid(0, 0, "    Distance (LY):", distance.toString(), highlightColor)
            info.addToGrid(1, 0, " |  Fuel required:", Misc.getWithDGS(cost.toFloat()), requiredFuelColor)
            if (crPenalty > 0) info.addToGrid(
                2,
                0,
                " |  Recovery cost:",
                Misc.getRoundedValueMaxOneAfterDecimal(supplyCost),
                highlightColor
            )
            info.addGrid(0f)
        }

        override fun canConfirmSelection(entity: SectorEntityToken?): Boolean {
            if (shiftJump == null || playerFleet == null) return false
            if (entity == null) return false

            val cost: Int = shiftJump!!.computeFuelCost(entity)
            val available = playerFleet!!.cargo.fuel.toInt()
            return cost <= available
        }

        override fun getFuelColorAlphaMult(): Float {
            return 0.5f
        }

        private fun unsetFields() {
            dialog = null
            shiftJump = null
            playerFleet = null
        }
    }

    private abstract class ShiftJumpState: StateInterface<State> {
        protected var shiftJump: ShiftJump
        protected val data: MutableMap<String, Any> = HashMap()

        constructor(shiftJump: ShiftJump) {
            this.shiftJump = shiftJump
        }

        override fun enter() {

        }

        override fun exit() {

        }

        override fun advance(amount: Float): State? {
            return null
        }
    }

    private class ChargeState(shiftJump: ShiftJump): ShiftJumpState(shiftJump) {
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

        override fun advance(amount: Float): State? {
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
                return State.READY
            }

            return State.CHARGING
        }
    }

    private class CooldownState(shiftJump: ShiftJump): ShiftJumpState(shiftJump) {
        override fun enter() {
            shiftJump.cooldownDays = WOESettings.shiftJumpCooldownDays
        }

        override fun advance(amount: Float): State {
            val daysElapsed = Global.getSector().clock.convertToDays(amount)
            shiftJump.cooldownDays -= daysElapsed
            if (shiftJump.cooldownDays <= 0.0f) {
                return State.INACTIVE
            }
            return State.COOLDOWN
        }
    }

    private class FinishedState(shiftJump: ShiftJump): ShiftJumpState(shiftJump) {
        override fun advance(amount: Float): State {
            return State.COOLDOWN
        }
    }

    private class InactiveState(shiftJump: ShiftJump): ShiftJumpState(shiftJump) {
        override fun enter() {
            shiftJump.cooldownDays = 0.0f
            shiftJump.blinking = false
        }
    }

    private class JumpingState(shiftJump: ShiftJump): ShiftJumpState(shiftJump) {
        private var jumpTimerDays: Float
            get() = data.getOrPut("jumpTimerDays") { 0.0f } as Float
            set(value) {
                data["jumpTimerDays"] = value
            }

        private var jumpTarget: SectorEntityToken?
            get() = data["jumpTarget"] as SectorEntityToken?
            set(value) {
                if (value == null) {
                    data.remove("jumpTarget")
                    return
                }
                data["jumpTarget"] = value
            }

        private var primedPing: EveryFrameScript?
            get() = data["primedPing"] as EveryFrameScript?
            set(value) {
                if (value == null) {
                    data.remove("primedPing")
                    return
                }
                data["primedPing"] = value
            }

        private fun spawnPrimedPing() {
            despawnPrimedPing()
            primedPing = Global.getSector().addPing(shiftJump.fleet, PRIME_PING_ID)
        }

        private fun despawnPrimedPing() {
            if (primedPing != null) Global.getSector().removeScript(primedPing)
            primedPing = null
        }

        private fun createDestinationToken(star: SectorEntityToken): SectorEntityToken {
            val starPlanet = star as PlanetAPI
            val distance: Float = (2f * (star.radius
                    + starPlanet.spec.coronaSize)
                    + 1000.0f)
            val offset = MathUtils.getRandomPointOnCircumference(null, distance)
            return starPlanet.starSystem.createToken(offset.x, offset.y)
        }

        private fun cleanupDestinationToken(token: SectorEntityToken) {
            Misc.fadeAndExpire(token, 0.1f)
        }

        private fun applySlowdown(activateSeconds: Float, amount: Float, fleet: CampaignFleetAPI) {
            val speed = fleet.velocity.length()
            val acc = max(speed, 200f) / activateSeconds + fleet.acceleration
            var ds = acc * amount
            if (ds > speed) ds = speed
            val dv = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(fleet.velocity))
            dv.scale(ds)
            fleet.setVelocity(fleet.velocity.x - dv.x, fleet.velocity.y - dv.y)
        }

        private fun doJump(fleet: CampaignFleetAPI, destination: SectorEntityToken) {
            val dest = JumpDestination(destination, null)
            Global.getSector().doHyperspaceTransition(fleet, fleet, dest)
            fleet.setNoEngaging(2.0f)
            fleet.clearAssignments()
            applyCRCost(destination)
            spendFuel(destination)
            spawnJumpPing(shiftJump.fleet)
            ShifterRiftCloud.create(fleet.containingLocation, fleet.location.x, fleet.location.y, fleet.radius * 2.0f, 3.0f)
            ShifterRiftCloud.create(destination.containingLocation, destination.location.x, destination.location.y, fleet.radius * 2.0f, 3.0f)
        }

        private fun spawnJumpPing(fleet: CampaignFleetAPI?) {
            Global.getSector().addPing(fleet, ACTIVATE_PING_ID)
        }

        private fun spendFuel(target: SectorEntityToken) {
            val cost = shiftJump.computeFuelCost(target)
            shiftJump.fleet.cargo.removeFuel(cost.toFloat())
        }

        private fun applyCRCost(target: SectorEntityToken) {
            var crCost = shiftJump.computeCRCost( target)
            if (crCost < 0.01) return

            val rng = Misc.random
            crCost += crCost * (((rng.nextFloat() - 0.5f) * 2f) * CR_USE_VARIANCE)
            val fleetMembers = shiftJump.fleet.membersWithFightersCopy

            for (member in fleetMembers) {
                if (member.isFighterWing) continue
                val repairTracker = member.repairTracker
                val crAfterUse = max(repairTracker.cr - crCost, 0f)
                var eventMessage = "Combat readiness reduced after Shift Jump."
                if (crAfterUse == 0f) {
                    if (tryApplyDamage(member, rng)) {
                        eventMessage = member.shipName + " was damaged due to complications during Shift Jump."
                        if (shiftJump.fleet.isPlayerFleet) Global.getSector().campaignUI
                            .addMessage(eventMessage, Misc.getNegativeHighlightColor())
                    } else if (tryDisable(member, rng)) {
                        eventMessage = member.shipName + " was destroyed due to complications during Shift Jump."
                        if (shiftJump.fleet.isPlayerFleet) Global.getSector().campaignUI
                            .addMessage(eventMessage, Misc.getNegativeHighlightColor())
                    }
                }
                repairTracker.applyCREvent(-crCost, eventMessage)
            }
        }

        private fun tryApplyDamage(fleetMember: FleetMemberAPI, rng: Random): Boolean {
            val rngResult: Float = rng.nextFloat()
            if (rngResult <= CHANCE_FOR_DAMAGE_AT_0_CR) {
                val damageTaken: Float = rng.nextFloat()
                fleetMember.status.applyDamage(fleetMember.hullSpec.hitpoints * damageTaken)
                return true
            }
            return false
        }

        private fun tryDisable(fleetMember: FleetMemberAPI, rng: Random): Boolean {
            val rngResult: Float = rng.nextFloat()
            if (rngResult <= CHANCE_FOR_DISABLE_AT_0_CR) {
                fleetMember.status.disable()
                return true
            }
            return false
        }

        override fun enter() {
            if (shiftJump.pickedTarget == null) return
            shiftJump.cooldownDays = WOESettings.shiftJumpCooldownDays
            spawnPrimedPing()
            jumpTimerDays = JUMP_TIMER_DAYS
            if (jumpTarget != null) {
                cleanupDestinationToken(jumpTarget!!)
                jumpTarget = null
            }
            jumpTarget = createDestinationToken(shiftJump.pickedTarget!!)
            shiftJump.pickedTarget = null
            shiftJump.showFloatingText("Shift jump initiated", Misc.setAlpha(shiftJump.fleet.indicatorColor, 255))

        }

        override fun exit() {
            despawnPrimedPing()
        }

        override fun advance(amount: Float): State? {
            val chargeCostPerDay = shiftJump.computeAndCacheChargeCostPerDay()
            if (!shiftJump.isChargeCostValid(chargeCostPerDay)) {
                shiftJump.deactivate()
                shiftJump.showFloatingText("Shift drive field destabilized", Misc.setAlpha(Misc.getNegativeHighlightColor(), 255))
                return null
            }

            if (jumpTarget == null) {
                return State.READY
            }
            val fuelCost = shiftJump.computeFuelCost(jumpTarget!!)
            if (fuelCost > shiftJump.fleet.cargo.fuel) {
                shiftJump.showFloatingText("Insufficient fuel for shift jump", Misc.setAlpha(Misc.getNegativeHighlightColor(), 255))
                return State.READY
            }

            val daysElapsed = Global.getSector().clock.convertToDays(amount)
            applySlowdown(5.0f, 1.0f - (jumpTimerDays / JUMP_TIMER_DAYS), shiftJump.fleet)
            jumpTimerDays -= daysElapsed
            if (jumpTimerDays <= 0.0f) {
                doJump(shiftJump.fleet, jumpTarget!!)
                return State.FINISHED
            }

            return State.JUMPING
        }

        companion object {
            private const val PRIME_PING_ID = "woe_shift_jump_prime"
            private const val ACTIVATE_PING_ID = "woe_shift_jump_activate"
            private const val JUMP_TIMER_DAYS: Float = 0.5f
            private const val CR_USE_VARIANCE: Float = 0.1f
            private const val CHANCE_FOR_DAMAGE_AT_0_CR: Float = 0.5f
            private const val CHANCE_FOR_DISABLE_AT_0_CR: Float = 0.1f
        }
    }

    private class ReadyState(shiftJump: ShiftJump): ShiftJumpState(shiftJump) {
        override fun enter() {
            shiftJump.blinking = true
            shiftJump.showFloatingText("Ready for shift jump", Misc.setAlpha(shiftJump.fleet.indicatorColor, 255))
        }

        override fun advance(amount: Float): State? {
            val chargeCostPerDay = shiftJump.computeAndCacheChargeCostPerDay()
            if (!shiftJump.isChargeCostValid(chargeCostPerDay)) {
                shiftJump.deactivate()
                shiftJump.showFloatingText("Shift drive field destabilized", Misc.setAlpha(Misc.getNegativeHighlightColor(), 255))
                return null
            }
            shiftJump.blinking = true
            return State.READY
        }
    }

    private class SelectingTargetState(shiftJump: ShiftJump): ShiftJumpState(shiftJump) {
        private fun showDestinationPicker() {
            DestinationPicker.execute(shiftJump)
        }

        override fun enter() {
            showDestinationPicker()
            shiftJump.blinking = false
        }

        override fun advance(amount: Float): State {
            if (shiftJump.pickedTarget != null) {
                return State.JUMPING
            }
            return State.READY
        }
    }
}