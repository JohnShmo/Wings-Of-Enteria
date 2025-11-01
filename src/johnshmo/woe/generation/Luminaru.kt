package johnshmo.woe.generation

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor
import com.fs.starfarer.api.impl.campaign.procgen.StarAge
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.terrain.BaseTiledTerrain.TileParams
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import com.fs.starfarer.api.util.Misc
import johnshmo.woe.*
import java.awt.Color

class Luminaru : WOEStarSystem() {
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

    val azureNexus: PlanetAPI
        get() {
            return system.getEntityById(AZURE_NEXUS_ID) as PlanetAPI
        }

    val azureNexusMoonHuxley: PlanetAPI
        get() {
            return system.getEntityById(AZURE_NEXUS_HUXLEY_ID) as PlanetAPI
        }

    val azureNexusMoonCemykKeye: PlanetAPI
        get() {
            return system.getEntityById(AZURE_NEXUS_CEMYK_KEYE_ID) as PlanetAPI
        }

    val tura: PlanetAPI
        get() {
            return system.getEntityById(TURA_ID) as PlanetAPI
        }

    val turaMoonKira: PlanetAPI
        get() {
            return system.getEntityById(TURA_KIRA_ID) as PlanetAPI
        }

    val wingsOfEnteria: SectorEntityToken
        get() {
            return system.getEntityById(WINGS_OF_ENTERIA_ID) as SectorEntityToken
        }

    override fun initializeImpl() {
        generateStar()
        generateAlice()
        generateAzureNexus()
        generateAsteroidBelt()
        generateTura()
        generateWingsOfEnteria()
        generateMisc()
    }

    private fun generateStar() {
        val generatedStar = "generatedStar"
        val starType = StarTypes.RED_DWARF
        val starRadius = 720f
        val coronaSize = 200f
        if (!data.contains(generatedStar)) {
            data[generatedStar] = true
            system.initStar(
                STAR_ID,
                starType,
                starRadius,
                coronaSize,
            )
            system.lightColor = Color(0xF0, 0xC0, 0xAF)
        }
    }

    private fun generateAlice() {
        val generatedAlice = "generatedAlice"
        val name = "Alice"
        val type = Planets.ROCKY_UNSTABLE
        val radius = 120f
        val angle = 120f
        val orbitRadius = 2000f
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
                    Color(0x00, 0x5F, 0xAF, 0x40),
                    0.0f
                )
            )
            magField.setCircularOrbit(alice, 0f, 0f, 0f)

            system.addPlanet(
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

    private fun generateAzureNexus() {
        val generatedAzureNexus = "generatedAzureNexus"
        val name = "Azure Nexus"
        val type = Planets.ICE_GIANT
        val radius = 270f
        val angle = 20f
        val orbitRadius = 4000f
        val orbitDays = 200f

        if (!data.contains(generatedAzureNexus)) {
            data[generatedAzureNexus] = true
            val azureNexus = system.addPlanet(
                AZURE_NEXUS_ID,
                star,
                name,
                type,
                angle,
                radius,
                orbitRadius,
                orbitDays,
            )
            val magField = system.addTerrain(
                Terrain.MAGNETIC_FIELD,
                MagneticFieldTerrainPlugin.MagneticFieldParams(
                    120f,
                    radius + 200f,
                    azureNexus,
                    radius + 10f,
                    radius + 800f,
                    Color(0x00, 0x5F, 0xAF, 0x90),
                    0.10f
                )
            )
            magField.setCircularOrbit(azureNexus, 0f, 0f, 0f)

            val huxley = system.addPlanet(
                AZURE_NEXUS_HUXLEY_ID,
                azureNexus,
                "Huxley",
                Planets.PLANET_TERRAN_ECCENTRIC,
                80f,
                122f,
                785f,
                57f
            )

            system.addPlanet(
                AZURE_NEXUS_CEMYK_KEYE_ID,
                azureNexus,
                "Cemyk'Keye",
                Planets.BARREN_BOMBARDED,
                60f,
                70f,
                1300f,
                85f
            )

            val marketSize = WOESettings.huxleyPopulationSize
            val industries = when (marketSize) {
                4 -> listOf(
                    Industries.GROUNDDEFENSES,
                    Industries.ORBITALSTATION_MID,
                    Industries.FARMING,
                    Industries.LIGHTINDUSTRY,
                    Industries.PATROLHQ
                )

                5 -> listOf(
                    Industries.GROUNDDEFENSES,
                    Industries.ORBITALSTATION_MID,
                    Industries.FARMING,
                    Industries.LIGHTINDUSTRY,
                    Industries.MINING,
                    Industries.PATROLHQ
                )

                in 6..10 -> listOf(
                    Industries.HEAVYBATTERIES,
                    Industries.BATTLESTATION_MID,
                    Industries.FARMING,
                    Industries.LIGHTINDUSTRY,
                    Industries.MINING,
                    Industries.PATROLHQ
                )

                else -> listOf(
                    Industries.GROUNDDEFENSES,
                    Industries.FARMING,
                    Industries.PATROLHQ
                )
            }

            createMarketplace(
                MarketplaceParams(
                    WOEFactions.ENTERIAN_FRAGMENT,
                    huxley,
                    listOf(),
                    "Huxley",
                    marketSize,
                    listOf(
                        Conditions.HABITABLE,
                        Conditions.EXTREME_WEATHER,
                        Conditions.TECTONIC_ACTIVITY,
                        Conditions.FARMLAND_RICH,
                        Conditions.ORGANICS_TRACE
                    ),
                    industries,
                    listOf(
                        Submarkets.SUBMARKET_STORAGE,
                        Submarkets.SUBMARKET_OPEN,
                        Submarkets.SUBMARKET_BLACK
                    )
                )
            )

        }
    }

    private fun generateAsteroidBelt() {
        val generatedAsteroidBelt = "generatedAsteroidBelt"
        if (!data.contains(generatedAsteroidBelt)) {
            data[generatedAsteroidBelt] = true
            system.addAsteroidBelt(
                star,
                1000,
                6000f,
                600f,
                280f,
                300f
            )
            system.addRingBand(
                star,
                "misc",
                "rings_dust0",
                256f,
                2,
                Color(0x00, 0x5F, 0xAF, 0xF0),
                600f,
                6000f,
                290f
            )
        }
    }

    private fun generateTura() {
        val generatedTura = "generatedTura"
        val name = "Tura"
        val type = Planets.GAS_GIANT
        val radius = 510f
        val angle = 70f
        val orbitRadius = 8100f
        val orbitDays = 520f

        val moonName = "Kira"
        val moonType = Planets.PLANET_LAVA_MINOR
        val moonRadius = 100f
        val moonAngle = 90f
        val moonOrbitRadius = 1400f
        val moonOrbitDays = 80f

        if (!data.contains(generatedTura)) {
            data[generatedTura] = true

            val tura = system.addPlanet(
                TURA_ID,
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
                    120f,
                    radius + 200f,
                    tura,
                    radius + 10f,
                    radius + 600f,
                    Color(0xF0, 0x5F, 0xAF, 0x60),
                    0.30f
                )
            )
            magField.setCircularOrbit(tura, 0f, 0f, 0f)
            system.addAsteroidBelt(
                tura,
                200,
                moonOrbitRadius,
                moonRadius * 6f,
                moonOrbitDays * 0.7f,
                moonOrbitDays * 1.6f
            )
            system.addRingBand(
                tura,
                "misc",
                "rings_dust0",
                256f,
                1,
                Color(0xA0, 0x5F, 0x1F, 0xFA),
                moonRadius * 6,
                moonOrbitRadius,
                moonOrbitDays * 0.8f
            )
            val kira = system.addPlanet(
                TURA_KIRA_ID,
                tura,
                moonName,
                moonType,
                moonAngle,
                moonRadius,
                moonOrbitRadius,
                moonOrbitDays
            )
            system.addRingBand(
                kira,
                "misc",
                "rings_dust0",
                256f,
                1,
                Color(0xF0, 0x5F, 0x4F, 0xFF),
                moonRadius * 4f,
                moonRadius * 3f,
                moonOrbitDays * 0.6f
            )
            val kiraRing2 = system.addRingBand(
                kira,
                "misc",
                "rings_asteroids0",
                256f,
                1,
                Color(0xA0, 0x5F, 0x4F, 0xFF),
                moonRadius * 2f,
                moonRadius * 4f,
                moonOrbitDays * 0.5f
            )
            kiraRing2.isSpiral = true
            kiraRing2.minSpiralRadius = moonRadius
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
            wingsOfEnteria.setCircularOrbit(
                turaMoonKira,
                90f,
                600f,
                turaMoonKira.circularOrbitPeriod
            )

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
                        WOEConditions.MANTLE_TAP
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

    private fun generateMisc() {
        val generatedMisc = "generatedMisc"
        val hyperspaceLocationX = WOESettings.luminaruHyperspaceLocationX
        val hyperspaceLocationY = WOESettings.luminaruHyperspaceLocationY
        if (!data.contains(generatedMisc)) {
            data[generatedMisc] = true
            system.location.set(hyperspaceLocationX, hyperspaceLocationY)
            system.generateAnchorIfNeeded()

            val comRelay = system.addCustomEntity(
                ID + "_" + Entities.COMM_RELAY,
                null,
                Entities.COMM_RELAY,
                WOEFactions.ENTERIAN_FRAGMENT
            )
            comRelay.setCircularOrbit(star, 35f + 180f, 2500f, 200f)

            val azureJumpPoint = Global.getFactory().createJumpPoint(ID + "_azure_jump_point", "Azure Jump-point")
            system.addEntity(azureJumpPoint)
            azureJumpPoint.setCircularOrbit(star, 45f, 2500f, 200f)

            val enteriaJumpPoint = Global.getFactory().createJumpPoint(ID + "_enteria_jump_point", "Enteria Jump-point")
            system.addEntity(enteriaJumpPoint)
            enteriaJumpPoint.setCircularOrbit(star,  100f, 10500f, 520f)

            val sensorArray = system.addCustomEntity(
                ID + "_" + Entities.SENSOR_ARRAY,
                null,
                Entities.SENSOR_ARRAY_MAKESHIFT,
                WOEFactions.ENTERIAN_FRAGMENT
            )
            sensorArray.setCircularOrbit(star, 230f, 11000f, 700f)

            val nebula = system.addTerrain(
                Terrain.NEBULA, TileParams(
                    "   xx " +
                            "  xx x" +
                            " xxxx " +
                            "xxxxxx" +
                            "  xx  " +
                            "    x ",
                    6, 6,  // size of the nebula grid, should match above string
                    "terrain", "nebula_amber", 4, 4, null
                )
            )
            nebula.setCircularOrbit(star, 140f, 11000f, 500f)

            StarSystemGenerator.addOrbitingEntities(
                system, star, StarAge.OLD,
                2, 4,
                12500f,
                3,
                true,
                false
            )

            StarSystemGenerator.addSystemwideNebula(system, StarAge.OLD)
            StarSystemGenerator.addStableLocations(system, 1)

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
        private const val AZURE_NEXUS_ID = ID + "_azure_nexus"
        private const val AZURE_NEXUS_HUXLEY_ID = AZURE_NEXUS_ID + "_huxley"
        private const val AZURE_NEXUS_CEMYK_KEYE_ID = AZURE_NEXUS_ID + "_cemyk_keye"
        private const val TURA_ID = ID + "_tura"
        private const val TURA_KIRA_ID = TURA_ID + "_kira"
        private const val WINGS_OF_ENTERIA_ID = "woe_wings_of_enteria"
    }
}