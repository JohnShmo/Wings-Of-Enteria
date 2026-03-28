package johnshmo.woe.abilities.shift_jump

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.util.Misc
import johnshmo.woe.WOESettings
import johnshmo.woe.abilities.ShiftJump
import org.lazywizard.lazylib.MathUtils
import java.util.Random
import kotlin.math.max

class ShiftJumpStateJumping(shiftJump: ShiftJump): ShiftJumpState(shiftJump) {
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

    override fun advance(amount: Float): ShiftJump.State? {
        val chargeCostPerDay = shiftJump.computeAndCacheChargeCostPerDay()
        if (!shiftJump.isChargeCostValid(chargeCostPerDay)) {
            shiftJump.deactivate()
            shiftJump.showFloatingText("Shift drive field destabilized", Misc.setAlpha(Misc.getNegativeHighlightColor(), 255))
            return null
        }

        if (jumpTarget == null) {
            return ShiftJump.State.READY
        }
        val fuelCost = shiftJump.computeFuelCost(jumpTarget!!)
        if (fuelCost > shiftJump.fleet.cargo.fuel) {
            shiftJump.showFloatingText("Insufficient fuel for shift jump", Misc.setAlpha(Misc.getNegativeHighlightColor(), 255))
            return ShiftJump.State.READY
        }

        val daysElapsed = Global.getSector().clock.convertToDays(amount)
        applySlowdown(5.0f, 1.0f - (jumpTimerDays / JUMP_TIMER_DAYS), shiftJump.fleet)
        jumpTimerDays -= daysElapsed
        if (jumpTimerDays <= 0.0f) {
            doJump(shiftJump.fleet, jumpTarget!!)
            return ShiftJump.State.FINISHED
        }

        return ShiftJump.State.JUMPING
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