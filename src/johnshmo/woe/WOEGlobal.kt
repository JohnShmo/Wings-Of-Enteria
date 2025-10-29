package johnshmo.woe

import com.fs.starfarer.api.Global
import org.apache.log4j.Logger

class WOEGlobal private constructor() {
    private val data: MutableMap<String, Any> = HashMap()
    val customData: MutableMap<String, Any> = HashMap()

    init {
        Global.getSector().persistentData[GLOBAL_ID] = this
    }

    companion object {
        val logger: Logger = Logger.getLogger(WOEGlobal::class.java.name)

        private const val GLOBAL_ID = "johnShmo_wingsOfEnteria"
        private const val GENERATOR_ID = "generator"
        private const val CAMPAIGN_ID = "campaignPlugin"

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
                    Global.getSector().persistentData[GLOBAL_ID] as WOEGlobal
                } catch (_: Exception) {
                    WOEGlobal()
                }
            }

        val generator: WOEGenerator
            get() {
                if (!instance.data.contains(GENERATOR_ID))
                    instance.data[GENERATOR_ID] = WOEGenerator()
                return instance.data[GENERATOR_ID] as WOEGenerator
            }

        val campaignPlugin: WOECampaignPlugin
            get() {
                if (!instance.data.contains(CAMPAIGN_ID))
                    instance.data[CAMPAIGN_ID] = WOECampaignPlugin()
                return instance.data[CAMPAIGN_ID] as WOECampaignPlugin
            }
    }
}
