package johnshmo.woe

import com.fs.starfarer.api.BaseModPlugin
import org.magiclib.util.MagicSettings

class WOEModPlugin : BaseModPlugin() {
    @Throws(Exception::class)
    override fun onApplicationLoad() {
        MagicSettings.loadModSettings()
        WOESettings.loadFromJSON(MagicSettings.modSettings?.getJSONObject("wingsOfEnteria"))
    }

    override fun onNewGame() {
        WOEGlobal.initialize()
    }

    override fun onNewGameAfterProcGen() {
       super.onNewGameAfterProcGen()
    }

    override fun onGameLoad(newGame: Boolean) {
        if (!newGame) {
            WOEGlobal.initialize()
        }
    }
}
