package redteamai;

import arc.Events;
import arc.util.Log;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.game.Team;
import mindustry.gen.Unit;
import mindustry.mod.Mod;

public class RedTeamAIMod extends Mod {
    private int totalBound = 0;

    @Override
    public void init() {
        Log.info("[RedTeamAI] === Mod init ===");
        Events.run(WorldLoadEvent.class, () -> {
            int n = takeOverAll();
            totalBound += n;
            Log.info("[RedTeamAI] 初始接管 " + n + " 个");
        });
        Events.run(Trigger.update, new Runnable() {
            int tick = 0;
            @Override
            public void run() {
                tick++;
                if (tick % 30 == 0) {
                    int n = takeOverAll();
                    if (n > 0) { totalBound += n; Log.info("[RedTeamAI] 轮询接管 " + n + " 个"); }
                }
            }
        });
    }

    static int cruxGroundCount() {
        int c = 0;
        for (Unit u : mindustry.Vars.state.teams.get(Team.crux).units) {
            if (u != null && !u.dead && !u.isFlying()) c++;
        }
        return c;
    }

    private int takeOverAll() {
        int bound = 0;
        try {
            var team = mindustry.Vars.state.teams.get(Team.crux);
            if (team == null || team.units == null) return 0;
            for (Unit u : team.units) {
                if (u == null || u.dead) continue;
                try {
                    if (u.isFlying()) {
                        if (u.controller() instanceof EnhancedFlyingAI) continue;
                        EnhancedFlyingAI ai = new EnhancedFlyingAI();
                        ai.unit(u); ai.init(); u.controller(ai); bound++;
                    } else {
                        if (u.controller() instanceof EnhancedGroundAI) continue;
                        EnhancedGroundAI ai = new EnhancedGroundAI();
                        ai.unit(u); ai.init(); u.controller(ai); bound++;
                    }
                } catch (Exception ex) { Log.err("[RedTeamAI] 接管失败: " + ex.getMessage()); }
            }
        } catch (Exception ex) { Log.err("[RedTeamAI] takeOverAll: " + ex.getMessage()); }
        return bound;
    }
}
