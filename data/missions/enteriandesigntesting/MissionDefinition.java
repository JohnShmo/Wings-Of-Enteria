package data.missions.afistfulofcredits;

import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.BattleObjectives;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {
	public void defineMission(MissionDefinitionAPI api) {
		api.initFleet(FleetSide.PLAYER, "", FleetGoal.ATTACK, false);
		api.initFleet(FleetSide.ENEMY, "", FleetGoal.ATTACK, true);

		api.setFleetTagline(FleetSide.PLAYER, "Good guys.");
		api.setFleetTagline(FleetSide.ENEMY, "Bad guys.");
		
		api.addToFleet(FleetSide.PLAYER, "woe_spades_attack", FleetMemberType.SHIP, "Spades", true);
		api.addToFleet(FleetSide.PLAYER, "woe_spades_concord_elite", FleetMemberType.SHIP, "Spades (EC)", false);
		api.addToFleet(FleetSide.PLAYER, "woe_spades_pirate_outdated", FleetMemberType.SHIP, "Spades (P)", false);
		api.addToFleet(FleetSide.PLAYER, "woe_hearts_support", FleetMemberType.SHIP, "Hearts", false);
		api.addToFleet(FleetSide.PLAYER, "woe_hearts_concord_elite", FleetMemberType.SHIP, "Hearts (EC)", false);
		
		api.addToFleet(FleetSide.ENEMY, "woe_spades_attack", FleetMemberType.SHIP, "Spades", false);
		api.addToFleet(FleetSide.ENEMY, "woe_spades_concord_elite", FleetMemberType.SHIP, "Spades (EC)", false);
		api.addToFleet(FleetSide.ENEMY, "woe_spades_pirate_outdated", FleetMemberType.SHIP, "Spades (P)", false);
		api.addToFleet(FleetSide.ENEMY, "woe_hearts_support", FleetMemberType.SHIP, "Hearts", false);
		api.addToFleet(FleetSide.ENEMY, "woe_hearts_concord_elite", FleetMemberType.SHIP, "Hearts (EC)", false);
		
		float width = 12000f;
		float height = 12000f;
		
		api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);
		
		float minX = -width/2;
		float minY = -height/2;
		
		api.addAsteroidField(minX, minY + height / 2, 0, 8000f,
							 20f, 70f, 100);
		
		api.addPlanet(0, 0, 50f, StarTypes.RED_GIANT, 250f, true);
	}
}
