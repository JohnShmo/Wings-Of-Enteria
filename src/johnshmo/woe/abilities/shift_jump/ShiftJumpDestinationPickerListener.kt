package johnshmo.woe.abilities.shift_jump

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEntityPickerListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import johnshmo.woe.abilities.ShiftJump
import johnshmo.woe.computeSupplyCostForCRRecovery

class ShiftJumpDestinationPickerListener(
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
        if (crPenalty > 0) info.addToGrid(2, 0, " |  CR penalty:", crPenalty.toString() + "%", highlightColor)
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

    // Prevents memory leak
    private fun unsetFields() {
        dialog = null
        shiftJump = null
        playerFleet = null
    }
}