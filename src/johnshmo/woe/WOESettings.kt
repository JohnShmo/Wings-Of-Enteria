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

        fun loadFromJSON(json: JSONObject?) {
            json?.keys()?.forEach { key ->
                when (key as String) {
                    "luminaruHyperspaceLocationX" -> luminaruHyperspaceLocationX = json.getDouble(key).toFloat()
                    "luminaruHyperspaceLocationY" -> luminaruHyperspaceLocationY = json.getDouble(key).toFloat()
                    "huxleyPopulationSize" -> huxleyPopulationSize = json.getInt(key)
                    "wingsOfEnteriaPopulationSize" -> wingsOfEnteriaPopulationSize = json.getInt(key)
                    "mantleTapOreBonus" -> mantleTapOreBonus = json.getInt(key)
                    "mantleTapRareOreBonus" -> mantleTapRareOreBonus = json.getInt(key)
                    "mantleTapVolatilesBonus" -> mantleTapVolatilesBonus = json.getInt(key)
                    "mantleTapTransplutonicsBaseDemand" -> mantleTapTransplutonicsBaseDemand =
                        json.getInt(key)
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
                }
            }
        }
    }
}