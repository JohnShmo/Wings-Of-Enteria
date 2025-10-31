package johnshmo.woe.econ

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.econ.BaseHazardCondition
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import johnshmo.woe.WOESettings
import java.util.*

class MantleTap : BaseHazardCondition() {
    override fun apply(id: String) {
        super.apply(id)

        val oreCommodity = Commodities.ORE
        val rareOreCommodity = Commodities.RARE_ORE
        val volatileCommodity = Commodities.VOLATILES
        val transplutonics = Commodities.RARE_METALS

        val miningIndustry = market.getIndustry(Industries.MINING) ?: return
        val size = market.size
        if (size < 1) {
            return
        }

        miningIndustry.getDemand(transplutonics).quantity.modifyFlat(
            id + "_0",
            WOESettings.mantleTapTransplutonicsBaseDemand + size.toFloat()
        )
        if (miningIndustry.isFunctional) {
            miningIndustry.supply(id + "_1_base", oreCommodity, size, "Base value for colony size")
            miningIndustry.supply(id + "_2_base", rareOreCommodity, size - 2, "Base value for colony size")
            miningIndustry.supply(id + "_3_base", volatileCommodity, size - 2, "Base value for colony size")

            miningIndustry.supply(
                id + "_1",
                oreCommodity,
                WOESettings.mantleTapOreBonus,
                Misc.ucFirst(condition.name.lowercase())
            )
            miningIndustry.supply(
                id + "_2",
                rareOreCommodity,
                WOESettings.mantleTapRareOreBonus,
                Misc.ucFirst(condition.name.lowercase())
            )
            miningIndustry.supply(
                id + "_3",
                volatileCommodity,
                WOESettings.mantleTapVolatilesBonus,
                Misc.ucFirst(condition.name.lowercase())
            )
        } else {
            miningIndustry.getSupply(oreCommodity).quantity.unmodify(id + "_1_base")
            miningIndustry.getSupply(rareOreCommodity).quantity.unmodify(id + "_2_base")
            miningIndustry.getSupply(volatileCommodity).quantity.unmodify(id + "_3_base")

            miningIndustry.getSupply(oreCommodity).quantity.unmodify(id + "_1")
            miningIndustry.getSupply(rareOreCommodity).quantity.unmodify(id + "_2")
            miningIndustry.getSupply(volatileCommodity).quantity.unmodify(id + "_3")
        }
    }

    override fun createTooltipAfterDescription(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltipAfterDescription(tooltip, expanded)

        val oreCommoditySpec = Global.getSettings().getCommoditySpec(Commodities.ORE)
        val rareOreCommoditySpec = Global.getSettings().getCommoditySpec(Commodities.RARE_ORE)
        val volatilesCommoditySpec = Global.getSettings().getCommoditySpec(Commodities.VOLATILES)
        val transplutonicsSpec = Global.getSettings().getCommoditySpec(Commodities.RARE_METALS)

        val oreBonus = WOESettings.mantleTapOreBonus
        val rareOreBonus = WOESettings.mantleTapRareOreBonus
        val volatilesBonus = WOESettings.mantleTapVolatilesBonus

        val pad = 10f
        tooltip?.addPara(
            "%s " + oreCommoditySpec.lowerCaseName + " production (mining)",
            pad,
            Misc.getHighlightColor(),
            getProductionBonusString(oreBonus)
        )
        tooltip?.addPara(
            "%s " + rareOreCommoditySpec.lowerCaseName + " production (mining)",
            pad,
            Misc.getHighlightColor(),
            getProductionBonusString(rareOreBonus)
        )
        tooltip?.addPara(
            "%s " + volatilesCommoditySpec.lowerCaseName + " production (mining)",
            pad,
            Misc.getHighlightColor(),
            getProductionBonusString(volatilesBonus)
        )
        tooltip?.addPara(
            transplutonicsSpec.name + " demand %s (mining)",
            pad,
            Misc.getHighlightColor(),
            "based on colony size"
        )
    }

    private fun getProductionBonusString(bonus: Int): String {
        var bonusStr = ""
        if (bonus == 0) {
            bonusStr = "No bonuses or penalties to"
        } else if (bonus > 1) {
            bonusStr = "+$bonus"
        } else {
            bonusStr = "-$bonus"
        }
        return bonusStr
    }
}