package johnshmo.woe.utils

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.util.Misc
import org.lazywizard.lazylib.MathUtils.clamp
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import java.util.*


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

fun lerp(a: Float, b: Float, t: Float): Float {
    return a + (b - a) * t
}

fun lerp(a: Vector2f, b: Vector2f, t: Float): Vector2f = Vector2f(
    lerp(a.x, b.x, t),
    lerp(a.y, b.y, t)
)

fun lerpColors(a: Color, b: Color, t: Float): Color {
    val clampedT = clamp(t, 0.0f, 1.0f)
    return Color(
        clamp(lerp(a.red.toFloat(), b.red.toFloat(), clampedT).toInt(),0, 255),
        clamp(lerp(a.green.toFloat(), b.green.toFloat(), clampedT).toInt(), 0, 255),
        clamp(lerp(a.blue.toFloat(), b.blue.toFloat(), clampedT).toInt(), 0, 255),
        clamp(lerp(a.alpha.toFloat(), b.alpha.toFloat(), clampedT).toInt(), 0, 255)
    )
}

fun inverseLerp(a: Float, b: Float, value: Float): Float {
    if (a != b) return clamp((value - a) / (b - a), 0.0f, 1.0f)
    return a
}

fun computeSupplyCostForCRRecovery(fleet: CampaignFleetAPI, crLoss: Float): Float {
    val members = fleet.fleetData.membersListCopy
    var totalSupplies = 0f
    for (member in members) {
        totalSupplies += computeSupplyCostForCRRecovery(member, crLoss)
    }
    return totalSupplies
}

fun computeSupplyCostForCRRecovery(fleetMember: FleetMemberAPI, crLoss: Float): Float {
    val deploymentCostSupplies = fleetMember.getDeploymentCostSupplies()
    val deploymentCostCR = fleetMember.getDeployCost() * 100f
    val suppliesPerCR = deploymentCostSupplies / deploymentCostCR
    return suppliesPerCR * (crLoss * 100f)
}

fun pickSystem(predicate: (StarSystemAPI) -> Boolean): StarSystemAPI? {
    val random: Random = Misc.random
    val allSystems = Global.getSector().starSystems
    val starSystems: MutableList<StarSystemAPI> = ArrayList<StarSystemAPI>()

    for (starSystem in allSystems) {
        if (predicate(starSystem) && starSystem.star != null) starSystems.add(starSystem)
    }

    var pickedSystem: StarSystemAPI? = null
    if (!starSystems.isEmpty()) {
        val pickedIndex: Int = random.nextInt(starSystems.size)
        pickedSystem = starSystems[pickedIndex]
    }

    return pickedSystem
}

fun pickSystem(procGenOnly: Boolean): StarSystemAPI? {
    return pickSystem { starSystem: StarSystemAPI -> !(procGenOnly && !starSystem.isProcgen) }
}

fun pickSystem(): StarSystemAPI? {
    return pickSystem(false)
}

fun pickSystem(starSystems: MutableList<StarSystemAPI?>): StarSystemAPI? {
    val random: Random = Misc.random
    var pickedSystem: StarSystemAPI? = null
    if (!starSystems.isEmpty()) {
        val pickedIndex: Int = random.nextInt(starSystems.size)
        pickedSystem = starSystems.get(pickedIndex)
    }
    return pickedSystem
}

fun pickMarket(predicate: (MarketAPI) -> Boolean): MarketAPI? {
    val random: Random = Misc.random

    val allMarkets = Global.getSector().economy.marketsCopy
    val markets: MutableList<MarketAPI> = mutableListOf()
    for (market in allMarkets) {
        if (predicate(market)) markets.add(market)
    }

    var pickedMarket: MarketAPI? = null
    if (!markets.isEmpty()) {
        val pickedIndex: Int = random.nextInt(markets.size)
        pickedMarket = markets.get(pickedIndex)
    }

    return pickedMarket
}

fun pickMarket(minSize: Int, factionIdSet: Set<String>): MarketAPI? {
    return pickMarket { market: MarketAPI ->
        !market.isInvalidMissionTarget && !market.isHidden && market.size >= minSize && factionIdSet.contains(
            market.factionId
        )
    }
}

fun pickMarket(minSize: Int, vararg factionIds: String): MarketAPI? {
    val factionIdSet: MutableSet<String> = mutableSetOf()
    Collections.addAll(factionIdSet, *factionIds)
    return pickMarket(minSize, factionIdSet)
}

fun pickMarket(minSize: Int): MarketAPI? {
    return pickMarket { market: MarketAPI -> !market.isInvalidMissionTarget && !market.isHidden && market.size >= minSize }
}

fun pickMarket(): MarketAPI? {
    return pickMarket(0)
}

fun pickUncolonizedPlanet(system: StarSystemAPI): PlanetAPI? {
    val allPlanets = system.planets
    val planets: MutableList<PlanetAPI> = mutableListOf()
    for (planet in allPlanets) {
        if (planet.isStar
            || planet.typeId == "black_hole"
            || (planet.market != null && planet.market.isInEconomy)
        ) continue
        planets.add(planet)
    }

    var picked: PlanetAPI? = null
    if (!planets.isEmpty()) {
        val index = Misc.random.nextInt(planets.size)
        picked = planets[index]
    }
    return picked
}