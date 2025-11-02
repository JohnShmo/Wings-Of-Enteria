package johnshmo.woe

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import org.dark.shaders.util.ShaderLib
import org.dark.shaders.util.TextureData
import org.magiclib.util.MagicSettings

class WOEModPlugin : BaseModPlugin() {
    @Throws(Exception::class)
    override fun onApplicationLoad() {
        MagicSettings.loadModSettings()
        WOESettings.loadFromJSON(MagicSettings.modSettings?.getJSONObject("wingsOfEnteria"))

        val hasGraphicsLib = Global.getSettings ().modManager.isModEnabled ( "shaderLib" );
        if (hasGraphicsLib) {
            WOESettings.hasGraphicsLib = true
            ShaderLib.init()
            TextureData.readTextureDataCSV("data/config/gfx/woe_ship_textures.csv")
        }
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
