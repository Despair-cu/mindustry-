package redteamai;

import arc.Events;
import arc.util.Log;
import mindustry.game.EventType.UnitDestroyEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.game.Team;
import mindustry.gen.Unit;
import mindustry.mod.Mod;

import java.util.HashMap;
import java.util.Map;

public class RedTeamAIMod extends Mod {

    private static final int GRID = 32;
    public static final Map<Long, Integer> deathHeat = new HashMap<>();
    private int totalBound = 0;

    @Override
    public void init() {
        Log.info("[RedTeamAI] === Mod init ===");

        // 世界加载：重置威胁地图 + 初始接管
        Events.run(WorldLoadEvent.class, () -> {
            deathHeat.clear();
            int n = takeOverAll();
            totalBound += n;
            Log.info("[RedTeamAI] 初始接管 " + n + " 个");
        });

        // 监听友军死亡 -> 记录热点（用 lambda 参数直接接收事件）
        Events.on(UnitDestroyEvent.class, event -> {
            Unit u = event.unit;
            if (u == null || u.team != Team.crux) return;
            long key = gridKey(u.x, u.y);
            deathHeat.merge(key, 1, Integer::sum);
        });

        // 每 30 tick 轮询接管新单位
        Events.run(Trigger.update, new Runnable() {
            int tick = 0;
            @Override
            public void run() {
                tick++;
                if (tick % 30 == 0) {
                    int n = takeOverAll();
                    if (n > 0) {
                        totalBound += n;
                        Log.info("[RedTeamAI] 轮询接管 " + n + " 个");
                    }
                }
            }
        });

        Log.info("[RedTeamAI] === 监听器注册完成 ===");
    }

    static long gridKey(float x, float y) {
        int gx = (int)(x / GRID);
        int gy = (int)(y / GRID);
        return ((long)gx << 32) | (gy & 0xFFFFFFFFL);
    }

    static int threatAtPoint(float x, float y) {
        int gx = (int)(x / GRID);
        int gy = (int)(y / GRID);
        int sum = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                long key = ((long)(gx + dx) << 32) | ((gy + dy) & 0xFFFFFFFFL);
                Integer v = deathHeat.get(key);
                if (v != null) sum += v;
            }
        }
        return sum;
    }

    static int totalThreat() {
        int s = 0;
        for (Integer v : deathHeat.values()) s += v;
        return s;
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
                if (u.isFlying()) continue;
                if (u.controller() instanceof EnhancedGroundAI) continue;
                try {
                    EnhancedGroundAI ai = new EnhancedGroundAI();
                    ai.unit(u);
                    ai.init();
                    u.controller(ai);
                    bound++;
                } catch (Exception ex) {
                    Log.err("[RedTeamAI] 接管失败: " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            Log.err("[RedTeamAI] takeOverAll: " + ex.getMessage());
        }
        return bound;
    }
}
