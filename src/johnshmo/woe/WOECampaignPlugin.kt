package johnshmo.woe

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignPlugin
import com.fs.starfarer.api.campaign.rules.MemoryAPI

class WOECampaignPlugin : BaseCampaignPlugin() {
    private val memory: MemoryAPI = Global.getFactory().createMemory()

    init {
        Global.getSector().registerPlugin(this)
    }

    fun initialize() {
        WOEGlobal.logger.info("Initializing campaign plugin...")

        WOEGlobal.logger.info("Campaign plugin initialized")
    }
}