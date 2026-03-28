package johnshmo.woe.abilities.shift_jump

import johnshmo.woe.abilities.ShiftJump


class ShiftJumpStateFinished(shiftJump: ShiftJump): ShiftJumpState(shiftJump) {
    override fun advance(amount: Float): ShiftJump.State {
        return ShiftJump.State.COOLDOWN
    }
}