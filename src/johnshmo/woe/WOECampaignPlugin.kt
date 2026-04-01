package johnshmo.woe

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignPlugin

class WOECampaignPlugin : BaseCampaignPlugin(), EveryFrameScript {
    private val data: MutableMap<String, Any> = HashMap()

    @Suppress("UNCHECKED_CAST")
    private val scripts: MutableSet<WOECampaignPluginScript>
        get() = data.getOrDefault("scripts", mutableSetOf<WOECampaignPluginScript>()) as MutableSet<WOECampaignPluginScript>

    init {
        Global.getSector().registerPlugin(this)
    }

    fun init() {
        WOEGlobal.logger.info("Initializing campaign plugin...")

        WOEGlobal.logger.info("Campaign plugin initialized")
    }

    override fun isDone(): Boolean = false

    override fun runWhilePaused(): Boolean = true

    override fun advance(amount: Float) {
        val toRemove = mutableListOf<WOECampaignPluginScript>()

        scripts.forEach {
            if (!(Global.getSector().isPaused && !it.runWhenPaused) && !it.isDone) {
                it.advance(amount)
            }
            if (it.isDone) {
                toRemove.add(it)
            }
        }
        toRemove.forEach {
            removeScript(it)
        }
    }

    fun addScript(script: WOECampaignPluginScript) {
        if (scripts.add(script)) {
            script.init()
        }
    }

    fun removeScript(script: WOECampaignPluginScript) {
        if (scripts.remove(script)) {
            script.cleanup()
        }
    }
}