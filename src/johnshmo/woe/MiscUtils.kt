package johnshmo.woe

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI

data class MarketplaceParams(
    val factionId: String,
    val primaryEntity: SectorEntityToken,
    val connectedEntities: List<SectorEntityToken>,
    val name: String,
    val size: Int,
    val marketConditions: List<String>,
    val industries: List<String>,
    val subMarkets: List<String>,
    val tariff: Float = 0.3f
)

fun createMarketplace(params: MarketplaceParams): MarketAPI {
    val globalEconomy = Global.getSector().economy
    val planetID = params.primaryEntity.id
    val newMarket = Global.getFactory().createMarket(planetID, params.name, params.size)
    newMarket.factionId = params.factionId
    newMarket.primaryEntity = params.primaryEntity
    newMarket.tariff.modifyFlat("generator", params.tariff)

    for (market in params.subMarkets) {
        newMarket.addSubmarket(market)
    }

    for (condition in params.marketConditions) {
        newMarket.addCondition(condition)
    }

    for (industry in params.industries) {
        newMarket.addIndustry(industry)
    }

    for (entity in params.connectedEntities) {
        newMarket.connectedEntities.add(entity)
    }

    globalEconomy.addMarket(newMarket, true)
    params.primaryEntity.market = newMarket
    params.primaryEntity.setFaction(params.factionId)

    for (entity in params.connectedEntities) {
        entity.market = newMarket
        entity.setFaction(params.factionId)
    }

    return newMarket
}