package johnshmo.woe

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.graphics.SpriteAPI

class CachedSprite(val category: String, val id: String) {
    @Transient
    private var cachedSprite: SpriteAPI? = null
    val sprite: SpriteAPI?
        get() {
            if (cachedSprite == null) {
                cachedSprite = Global.getSettings().getSprite(category, id)
            }
            return cachedSprite
        }
}