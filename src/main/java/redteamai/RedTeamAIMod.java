package redteamai;

import arc.Events;
import arc.util.Log;
import mindustry.game.EventType.UnitCreateEvent;
import mindustry.mod.Mod;

public class RedTeamAIMod extends Mod {

    @Override
    public void init() {
        Log.info("[RedTeamAI] Mod initialized!");

        Events.on(UnitCreateEvent.class, event -> {
            var u = event.unit;
            if (u == null || u.dead) return;

            if (u.team != mindustry.game.Team.crux) return;
            if (u.isFlying()) return;

            EnhancedGroundAI ai = new EnhancedGroundAI();
            u.controller(ai);
            ai.unit(u);
            ai.init();
            Log.info("[RedTeamAI] Replaced AI for " + u.type.name);
        });
    }
}
