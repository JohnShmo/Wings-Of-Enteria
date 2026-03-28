package johnshmo.woe.abilities.shift_jump

import johnshmo.woe.abilities.ShiftJump
import johnshmo.woe.utils.StateInterface

abstract class ShiftJumpState: StateInterface<ShiftJump.State> {
    protected var shiftJump: ShiftJump
    protected val data: MutableMap<String, Any> = HashMap()

    constructor(shiftJump: ShiftJump) {
        this.shiftJump = shiftJump
    }

    override fun enter() {

    }

    override fun exit() {

    }

    override fun advance(amount: Float): ShiftJump.State? {
        return null
    }
}