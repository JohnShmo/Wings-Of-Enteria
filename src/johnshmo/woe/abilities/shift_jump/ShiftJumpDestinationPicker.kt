package johnshmo.woe.abilities.shift_jump

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import johnshmo.woe.abilities.ShiftJump

class ShiftJumpDestinationPicker : InteractionDialogPlugin {
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
            ShiftJumpDestinationPickerListener(this.dialog, this.shiftJump)
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

    // Prevents memory leak
    private fun unsetFields() {
        this.shiftJump = null
        this.dialog = null
    }

    companion object {
        fun execute(shiftJump: ShiftJump) {
            val ui = Global.getSector().campaignUI
            val picker = ShiftJumpDestinationPicker()
            picker.shiftJump = shiftJump
            ui.showInteractionDialog(picker, null)
        }
    }
}