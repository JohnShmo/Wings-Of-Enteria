package johnshmo.woe

import org.json.JSONObject

class WOESettings {
    companion object {
        var luminaruHyperspaceLocationX = 2500f
        var luminaruHyperspaceLocationY = -7400f
        var huxleyPopulationSize = 4
        var wingsOfEnteriaPopulationSize = 6
        var mantleTapOreBonus = 2
        var mantleTapRareOreBonus = 2
        var mantleTapVolatilesBonus = 2
        var mantleTapTransplutonicsBaseDemand = 2

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
                }
            }
        }
    }
}