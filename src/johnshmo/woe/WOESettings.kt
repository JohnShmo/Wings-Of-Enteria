package johnshmo.woe

import org.json.JSONObject

class WOESettings {
    companion object {
        var luminaruHyperspaceLocationX = 2500f
        var luminaruHyperspaceLocationY = -7400f
        var wingsOfEnteriaPopulationSize = 6

        fun loadFromJSON(json: JSONObject?) {
            json?.keys()?.forEach { key ->
                when(key as String) {
                    "luminaruHyperspaceLocationX" -> luminaruHyperspaceLocationX = json.getDouble(key).toFloat()
                    "luminaruHyperspaceLocationY" -> luminaruHyperspaceLocationY = json.getDouble(key).toFloat()
                    "wingsOfEnteriaPopulationSize" -> wingsOfEnteriaPopulationSize = json.getInt(key)
                }
            }
        }
    }
}