package johnshmo.woe.abilities.shift_jump

import johnshmo.woe.WOESettings
import johnshmo.woe.abilities.ShiftJump

class ShiftJumpStateSelectingTarget(shiftJump: ShiftJump): ShiftJumpState(shiftJump) {
    private fun showDestinationPicker() {
        ShiftJumpDestinationPicker.execute(shiftJump)
    }

    override fun enter() {
        showDestinationPicker()
        shiftJump.blinking = false
    }

    override fun advance(amount: Float): ShiftJump.State {
        if (shiftJump.pickedTarget != null) {
            return ShiftJump.State.JUMPING
        }
        return ShiftJump.State.READY
    }
}