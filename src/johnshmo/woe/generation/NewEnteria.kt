package johnshmo.woe.generation

import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.impl.campaign.ids.Planets
import com.fs.starfarer.api.impl.campaign.ids.StarTypes
import com.fs.starfarer.api.impl.campaign.ids.Terrain
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.MagneticFieldTerrainPlugin
import com.fs.starfarer.api.util.Misc
import java.awt.Color

class NewEnteria : StarSystemGenerator() {
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

    override fun initializeImpl() {
        generateStar()
        generateAlice()
        generateHyperspace()
    }

    private fun generateStar() {
        val generatedStar = "generatedStar"
        val starType = StarTypes.BROWN_DWARF
        val starRadius = 700f
        val coronaSize = 100f
        if (!data.contains(generatedStar)) {
            system.initStar(
                STAR_ID,
                starType,
                starRadius,
                coronaSize,
            )
        }
    }

    private fun generateAlice() {
        val generatedAlice = "generatedAlice"
        val name = "Alice"
        val type = Planets.ROCKY_UNSTABLE
        val radius = 80f
        val angle = 120f
        val orbitRadius = 2120f
        val orbitDays = 90f

        val moonName = "Molly"
        val moonType = Planets.ROCKY_METALLIC
        val moonRadius = 45f
        val moonAngle = 90f
        val moonOrbitRadius = 400f
        val moonOrbitDays = 12f

        if (!data.contains(generatedAlice)) {
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

    private fun generateHyperspace() {
        val hyperspaceVersionId = "hyperspace_version_1"
        val hyperspaceLocationX = 2500f
        val hyperspaceLocationY = -7400f
        if (!data.contains(hyperspaceVersionId)) {
            resetHyperspace()
            system.location.set(hyperspaceLocationX, hyperspaceLocationY)
            system.generateAnchorIfNeeded()
            system.autogenerateHyperspaceJumpPoints(true, true)
            val plugin = Misc.getHyperspaceTerrain().plugin as HyperspaceTerrainPlugin
            val editor = NebulaEditor(plugin)
            val minRadius = plugin.tileSize * 2f
            val radius = system.maxRadiusInHyperspace
            editor.clearArc(system.location.x, system.location.y, 0f, radius + minRadius, 0f, 360f)
            editor.clearArc(system.location.x, system.location.y, 0f, radius + minRadius, 0f, 360f, 0.25f)
            data[hyperspaceVersionId] = true
        }
    }

    companion object {
        private const val ID = "woe_new_enteria"
        private const val NAME = "New Enteria"
        private const val STAR_ID = ID + "_star"
        private const val ALICE_ID = ID + "_alice"
        private const val ALICE_MOLLY_ID = ALICE_ID + "_molly"
    }
}