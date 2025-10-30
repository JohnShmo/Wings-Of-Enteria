package johnshmo.woe.generation

import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.Items
import com.fs.starfarer.api.impl.campaign.ids.Planets
import com.fs.starfarer.api.impl.campaign.ids.StarTypes
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import com.fs.starfarer.api.impl.campaign.ids.Terrain
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import com.fs.starfarer.api.util.Misc
import johnshmo.woe.MarketplaceParams
import johnshmo.woe.WOECustomEntities
import johnshmo.woe.WOEFactions
import johnshmo.woe.WOESettings
import johnshmo.woe.createMarketplace
import java.awt.Color

class Luminaru : StarSystemGenerator() {
    override val id: String
        get() = ID
    override val name: String
        get() = NAME

    val star: PlanetAPI
        get() {
            return system.star
        }

    val alice: PlanetAPI
        get() {
            return system.getEntityById(ALICE_ID) as PlanetAPI
        }

    val aliceMoonMolly: PlanetAPI
        get() {
            return system.getEntityById(ALICE_MOLLY_ID) as PlanetAPI
        }

    val wingsOfEnteria: SectorEntityToken
        get() {
            return system.getEntityById(WINGS_OF_ENTERIA_ID) as SectorEntityToken
        }

    override fun initializeImpl() {
        generateStar()
        generateAlice()
        generateWingsOfEnteria()
        generateHyperspace()
    }

    private fun generateStar() {
        val generatedStar = "generatedStar"
        val starType = StarTypes.WHITE_DWARF
        val starRadius = 720f
        val coronaSize = 200f
        if (!data.contains(generatedStar)) {
            data[generatedStar] = true
            val star = system.initStar(
                STAR_ID,
                starType,
                starRadius,
                coronaSize,
            )
            star.lightColorOverrideIfStar = Color(0xC0, 0xF0, 0xFF)
            system.lightColor = Color(0xC0, 0xF0, 0xFF)
        }
    }

    private fun generateAlice() {
        val generatedAlice = "generatedAlice"
        val name = "Alice"
        val type = Planets.ROCKY_UNSTABLE
        val radius = 120f
        val angle = 120f
        val orbitRadius = 2250f
        val orbitDays = 90f

        val moonName = "Molly"
        val moonType = Planets.ROCKY_METALLIC
        val moonRadius = 60f
        val moonAngle = 90f
        val moonOrbitRadius = 500f
        val moonOrbitDays = 12f

        if (!data.contains(generatedAlice)) {
            data[generatedAlice] = true

            val alice = system.addPlanet(
                ALICE_ID,
                star,
                name,
                type,
                angle,
                radius,
                orbitRadius,
                orbitDays
            )
            val magField = system.addTerrain(
                Terrain.MAGNETIC_FIELD,
                MagneticFieldTerrainPlugin.MagneticFieldParams(
                    80f,
                    radius + 100f,
                    alice,
                    radius + 10f,
                    moonOrbitRadius * 0.9f,
                    Color(0x00, 0x5F, 0xAF, 0xA0),
                    0.25f
                )
            )
            magField.setCircularOrbit(alice, 0f, 0f, 0f)

            val molly = system.addPlanet(
                ALICE_MOLLY_ID,
                alice,
                moonName,
                moonType,
                moonAngle,
                moonRadius,
                moonOrbitRadius,
                moonOrbitDays,
            )
        }
    }

    private fun generateWingsOfEnteria() {
        val generatedWingsOfEnteria = "generatedWingsOfEnteria"
        if (!data.contains(generatedWingsOfEnteria)) {
            data[generatedWingsOfEnteria] = true

            val wingsOfEnteria = system.addCustomEntity(
                WINGS_OF_ENTERIA_ID,
                "Wings of Enteria",
                WOECustomEntities.WINGS_OF_ENTERIA,
                WOEFactions.ENTERIAN_FRAGMENT
            )
            wingsOfEnteria.setCircularOrbit(star, 10f, 3000f, 200f)

            val marketSize = WOESettings.wingsOfEnteriaPopulationSize
            val industries = when (marketSize) {
                4 -> listOf(
                    Industries.GROUNDDEFENSES,
                    Industries.ORBITALSTATION_MID,
                    Industries.MINING,
                    Industries.HEAVYINDUSTRY,
                    Industries.PATROLHQ
                )

                5 -> listOf(
                    Industries.GROUNDDEFENSES,
                    Industries.BATTLESTATION_MID,
                    Industries.MINING,
                    Industries.REFINING,
                    Industries.HEAVYINDUSTRY,
                    Industries.PATROLHQ
                )

                in 6..10 -> listOf(
                    Industries.HEAVYBATTERIES,
                    Industries.STARFORTRESS_MID,
                    Industries.MINING,
                    Industries.REFINING,
                    Industries.ORBITALWORKS,
                    Industries.HIGHCOMMAND
                )

                else -> listOf(
                    Industries.GROUNDDEFENSES,
                    Industries.ORBITALSTATION_MID,
                    Industries.MINING,
                    Industries.PATROLHQ
                )
            }

            val market = createMarketplace(
                MarketplaceParams(
                    WOEFactions.ENTERIAN_FRAGMENT,
                    wingsOfEnteria,
                    listOf(),
                    "Wings of Enteria",
                    marketSize,
                    listOf(
                        Conditions.ORE_RICH,
                        Conditions.RARE_ORE_RICH,
                        Conditions.VOLATILES_ABUNDANT,
                        Conditions.POLLUTION
                    ),
                    industries,
                    listOf(
                        Submarkets.SUBMARKET_STORAGE,
                        Submarkets.GENERIC_MILITARY,
                        Submarkets.SUBMARKET_OPEN,
                        Submarkets.SUBMARKET_BLACK
                    )
                )
            )

            val heavyIndustry = when (marketSize) {
                in 4..5 -> Industries.HEAVYINDUSTRY
                in 6..10 -> Industries.ORBITALWORKS
                else -> null
            }
            if (heavyIndustry != null) {
                market.getIndustry(heavyIndustry).specialItem = SpecialItemData(
                    Items.CORRUPTED_NANOFORGE,
                    null
                )
                market.getIndustry(heavyIndustry).aiCoreId = "gamma_core"
            }
            market.getIndustry(Industries.MINING).aiCoreId = "gamma_core"
        }
    }

    private fun generateHyperspace() {
        val generatedHyperspace = "generatedHyperspace"
        val hyperspaceLocationX = WOESettings.luminaruHyperspaceLocationX
        val hyperspaceLocationY = WOESettings.luminaruHyperspaceLocationY
        if (!data.contains(generatedHyperspace)) {
            data[generatedHyperspace] = true
            system.location.set(hyperspaceLocationX, hyperspaceLocationY)
            system.generateAnchorIfNeeded()
            system.autogenerateHyperspaceJumpPoints(true, true)
            val plugin = Misc.getHyperspaceTerrain().plugin as HyperspaceTerrainPlugin
            val editor = NebulaEditor(plugin)
            val minRadius = plugin.tileSize * 2f
            val radius = system.maxRadiusInHyperspace
            editor.clearArc(system.location.x, system.location.y, 0f, radius + minRadius, 0f, 360f)
            editor.clearArc(system.location.x, system.location.y, 0f, radius + minRadius, 0f, 360f, 0.25f)
        }
    }

    companion object {
        private const val ID = "woe_luminaru"
        private const val NAME = "Luminaru"
        private const val STAR_ID = ID + "_star"
        private const val ALICE_ID = ID + "_alice"
        private const val ALICE_MOLLY_ID = ALICE_ID + "_molly"
        private const val WINGS_OF_ENTERIA_ID = "woe_wings_of_enteria"
    }
}