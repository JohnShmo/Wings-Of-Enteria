package johnshmo.woe.abilities.shift_jump

import com.fs.starfarer.api.Global
import johnshmo.woe.WOESettings
import johnshmo.woe.abilities.ShiftJump

class ShiftJumpStateCooldown(shiftJump: ShiftJump): ShiftJumpState(shiftJump) {
    override fun enter() {
        shiftJump.cooldownDays = WOESettings.shiftJumpCooldownDays
    }

    override fun advance(amount: Float): ShiftJump.State {
        val daysElapsed = Global.getSector().clock.convertToDays(amount)
        shiftJump.cooldownDays -= daysElapsed
        if (shiftJump.cooldownDays <= 0.0f) {
            return ShiftJump.State.INACTIVE
        }
        return ShiftJump.State.COOLDOWN
    }
}