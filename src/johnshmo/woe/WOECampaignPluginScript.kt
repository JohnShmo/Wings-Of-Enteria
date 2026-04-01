package johnshmo.woe

interface WOECampaignPluginScript {
    val isDone: Boolean
    val runWhenPaused: Boolean

    fun init()
    fun advance(amount: Float)
    fun cleanup()
}
