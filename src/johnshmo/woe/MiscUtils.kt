package johnshmo.woe

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.Industries

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

    when(params.size) {
        1 -> newMarket.addCondition(Conditions.POPULATION_1)
        2 -> newMarket.addCondition(Conditions.POPULATION_2)
        3 -> newMarket.addCondition(Conditions.POPULATION_3)
        4 -> newMarket.addCondition(Conditions.POPULATION_4)
        5 -> newMarket.addCondition(Conditions.POPULATION_5)
        6 -> newMarket.addCondition(Conditions.POPULATION_6)
        7 -> newMarket.addCondition(Conditions.POPULATION_7)
        8 -> newMarket.addCondition(Conditions.POPULATION_8)
        9 -> newMarket.addCondition(Conditions.POPULATION_9)
        10 -> newMarket.addCondition(Conditions.POPULATION_10)
    }
    for (condition in params.marketConditions) {
        newMarket.addCondition(condition)
    }

    newMarket.addIndustry(Industries.POPULATION)
    newMarket.addIndustry(Industries.SPACEPORT)
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