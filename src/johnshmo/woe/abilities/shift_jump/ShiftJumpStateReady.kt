package johnshmo.woe.abilities.shift_jump

import com.fs.starfarer.api.util.Misc
import johnshmo.woe.abilities.ShiftJump

class ShiftJumpStateReady(shiftJump: ShiftJump): ShiftJumpState(shiftJump) {
    override fun enter() {
        shiftJump.blinking = true
        shiftJump.showFloatingText("Ready for shift jump", Misc.setAlpha(shiftJump.fleet.indicatorColor, 255))
    }

    override fun advance(amount: Float): ShiftJump.State? {
        val chargeCostPerDay = shiftJump.computeAndCacheChargeCostPerDay()
        if (!shiftJump.isChargeCostValid(chargeCostPerDay)) {
            shiftJump.deactivate()
            shiftJump.showFloatingText("Shift drive field destabilized", Misc.setAlpha(Misc.getNegativeHighlightColor(), 255))
            return null
        }
        shiftJump.blinking = true
        return ShiftJump.State.READY
    }
}