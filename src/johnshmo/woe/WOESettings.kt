package johnshmo.woe

import org.json.JSONObject

class WOESettings {
    companion object {
        var hasGraphicsLib = false

        var luminaruHyperspaceLocationX = 2500f
        var luminaruHyperspaceLocationY = -7400f
        var huxleyPopulationSize = 4
        var wingsOfEnteriaPopulationSize = 6

        var mantleTapOreBonus = 2
        var mantleTapRareOreBonus = 2
        var mantleTapVolatilesBonus = 2
        var mantleTapTransplutonicsBaseDemand = 2

        var enterianConcordArmorBonuses = arrayOf(50f, 60f, 70f, 80f)
        var enterianConcordFluxBonusPercent = 8f
        var enterianConcordAmmoCapacityBonusPercent = 20f
        var enterianConcordZeroFluxSpeedPenaltyMult = 0.85f

        var shiftJumpChargeTimeDays = 2.5f
        var shiftJumpChargeTransplutonicsPerDayPerDP = 0.05f
        var shiftJumpCooldownDays = 10f
        var shiftJumpMaxRangeLY = 20.0f
        var shiftJumpMinFuelCostMultiplier = 0.5f
        var shiftJumpMaxFuelCostMultiplier = 1.5f
        var shiftJumpMinCRCost = 0.01f
        var shiftJumpMaxCRCost = 0.75f
        var shiftJumpSensorProfilePenalty = 1000f

        fun loadFromJSON(json: JSONObject?) {
            json?.keys()?.forEach { key ->
                when (key as String) {
                    "luminaruHyperspaceLocationX" -> luminaruHyperspaceLocationX = json.getDouble(key).toFloat()
                    "luminaruHyperspaceLocationY" -> luminaruHyperspaceLocationY = json.getDouble(key).toFloat()
                    "huxleyPopulationSize" -> huxleyPopulationSize = json.getDouble(key).toInt()
                    "wingsOfEnteriaPopulationSize" -> wingsOfEnteriaPopulationSize = json.getDouble(key).toInt()
                    "mantleTapOreBonus" -> mantleTapOreBonus = json.getDouble(key).toInt()
                    "mantleTapRareOreBonus" -> mantleTapRareOreBonus = json.getDouble(key).toInt()
                    "mantleTapVolatilesBonus" -> mantleTapVolatilesBonus = json.getDouble(key).toInt()
                    "mantleTapTransplutonicsBaseDemand" -> mantleTapTransplutonicsBaseDemand =
                        json.getDouble(key).toInt()
                    "enterianConcordArmorBonuses" -> {
                        val jsonArray = json.getJSONArray(key)
                        val result = enterianConcordArmorBonuses
                        for (i in 0 until jsonArray.length()) {
                            if (result.size <= i) break
                            result[i] = jsonArray.getDouble(i).toFloat()
                        }
                        enterianConcordArmorBonuses = result
                    }
                    "enterianConcordFluxBonusPercent" -> enterianConcordFluxBonusPercent = json.getDouble(key).toFloat()
                    "enterianConcordAmmoCapacityBonusPercent" -> enterianConcordAmmoCapacityBonusPercent = json.getDouble(key).toFloat()
                    "enterianConcordZeroFluxSpeedPenaltyMult" -> enterianConcordZeroFluxSpeedPenaltyMult = json.getDouble(key).toFloat()
                    "shiftJumpChargeTimeDays" -> shiftJumpChargeTimeDays = json.getDouble(key).toFloat()
                    "shiftJumpChargeTransplutonicsPerDayPerDP" -> shiftJumpChargeTransplutonicsPerDayPerDP = json.getDouble(key).toFloat()
                    "shiftJumpCooldownDays" -> shiftJumpCooldownDays = json.getDouble(key).toFloat()
                    "shiftJumpMaxRangeLY" -> shiftJumpMaxRangeLY = json.getDouble(key).toFloat()
                    "shiftJumpMinFuelCostMultiplier" -> shiftJumpMinFuelCostMultiplier = json.getDouble(key).toFloat()
                    "shiftJumpMaxFuelCostMultiplier" -> shiftJumpMaxFuelCostMultiplier = json.getDouble(key).toFloat()
                    "shiftJumpMinCRCost" -> shiftJumpMinCRCost = json.getDouble(key).toFloat()
                    "shiftJumpMaxCRCost" -> shiftJumpMaxCRCost = json.getDouble(key).toFloat()
                    "shiftJumpSensorProfilePenalty" -> shiftJumpSensorProfilePenalty = json.getDouble(key).toFloat()
                }
            }
        }
    }
}