package johnshmo.woe

import com.fs.starfarer.api.BaseModPlugin

class WOEModPlugin : BaseModPlugin() {
    @Throws(Exception::class)
    override fun onApplicationLoad() {
        super.onApplicationLoad()
    }

    override fun onNewGame() {
        super.onNewGame()
        WOEGlobal.initialize()
    }

    override fun onGameLoad(newGame: Boolean) {
        super.onGameLoad(newGame)
        if (!newGame) {
            WOEGlobal.initialize()
        }
    }
}
