package johnshmo.woe.abilities.shift_jump

import johnshmo.woe.abilities.ShiftJump

class ShiftJumpStateInactive(shiftJump: ShiftJump): ShiftJumpState(shiftJump) {
    override fun enter() {
        shiftJump.cooldownDays = 0.0f
        shiftJump.blinking = false
    }
}