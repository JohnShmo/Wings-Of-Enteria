package johnshmo.woe

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI

class WOECombatPlugin : BaseEveryFrameCombatPlugin() {
    @Transient
    private val data: MutableMap<String, Any> = HashMap()
    @Transient
    private var cachedEngine: CombatEngineAPI? = null
    private val engine: CombatEngineAPI
        get() {
            if (cachedEngine != null) return cachedEngine!!
            val result = Global.getCombatEngine()
            cachedEngine = result
            return result
        }
}