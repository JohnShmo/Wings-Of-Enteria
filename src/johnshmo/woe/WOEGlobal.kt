package johnshmo.woe

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignPlugin
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import org.apache.log4j.Logger

class WOEGlobal private constructor() {
    private val memory: MemoryAPI = Global.getFactory().createMemory()

    init {
        Global.getSector().memoryWithoutUpdate.set(GLOBAL_MEMORY_ID, this)
    }

    companion object {
        val logger: Logger = Logger.getLogger(WOEGlobal::class.java.name)

        private const val GLOBAL_MEMORY_ID = "\$johnShmo_wingsOfEnteria"
        private const val GENERATOR_MEMORY_ID = "\$generator"
        private const val CAMPAIGN_PLUGIN_ID = "\$campaignPlugin"

        @JvmStatic
        fun initialize() {
            logger.info("Initializing...")
            generator.initialize()
            campaignPlugin.initialize()
            logger.info("Initialization complete")
        }

        private val instance: WOEGlobal
            get() {
                return try {
                    Global.getSector().memoryWithoutUpdate
                        .get(GLOBAL_MEMORY_ID) as WOEGlobal
                } catch (_: Exception) {
                    WOEGlobal()
                }
            }

        val generator: WOEGenerator
            get() {
                if (!instance.memory.contains(GENERATOR_MEMORY_ID))
                    instance.memory.set(GENERATOR_MEMORY_ID, WOEGenerator())
                return instance.memory.get(GENERATOR_MEMORY_ID) as WOEGenerator
            }

        val campaignPlugin: WOECampaignPlugin
            get() {
                if (!instance.memory.contains(CAMPAIGN_PLUGIN_ID))
                    instance.memory.set(CAMPAIGN_PLUGIN_ID, WOECampaignPlugin())
                return instance.memory.get(CAMPAIGN_PLUGIN_ID) as WOECampaignPlugin
            }
    }
}
