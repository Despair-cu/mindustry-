package redteamai;

import arc.Events;
import arc.util.Log;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.game.Team;
import mindustry.gen.Unit;
import mindustry.mod.Mod;

public class RedTeamAIMod extends Mod {

    private int tickCounter = 0;
    private int totalBound = 0;

    @Override
    public void init() {
        Log.info("[RedTeamAI] === Mod init() 开始 ===");

        // 世界加载完成后立即接管一波
        Events.run(WorldLoadEvent.class, () -> {
            Log.info("[RedTeamAI] 收到 WorldLoadEvent，开始初始接管...");
            int n = takeOverAll();
            totalBound += n;
            Log.info("[RedTeamAI] >>> 初始接管完成，本次绑定 " + n + " 个，累计 " + totalBound + " 个");
        });

        // ★ 就改了这里：Events.on → Events.run ★
        Events.run(Trigger.update, () -> {
            tickCounter++;
            if (tickCounter % 30 == 0) {
                int n = takeOverAll();
                if (n > 0) {
                    totalBound += n;
                    Log.info("[RedTeamAI] >>> 轮询接管 " + n + " 个，累计绑定 " + totalBound + " 个");
                }
            }
        });

        Log.info("[RedTeamAI] === Mod init() 完成，监听器已注册 ===");
    }

    /**
     * 遍历红队所有单位，把地面单位换成 EnhancedGroundAI
     */
    private int takeOverAll() {
        try {
            var teamData = mindustry.Vars.state.teams.get(Team.crux);
            if (teamData == null || teamData.units == null) {
                Log.warn("[RedTeamAI] teamData 或 units 为 null");
                return 0;
            }

            int scanned = 0;
            int bound = 0;

            for (Unit u : teamData.units) {
                if (u == null || u.dead) continue;
                scanned++;

                if (u.isFlying()) continue;
                if (u.controller() instanceof EnhancedGroundAI) continue;

                try {
                    EnhancedGroundAI ai = new EnhancedGroundAI();
                    ai.unit(u);
                    ai.init();
                    u.controller(ai);
                    bound++;
                    Log.info("[RedTeamAI] 已接管单位: " + u.type + " (id=" + u.id + ")");
                } catch (Exception ex) {
                    Log.err("[RedTeamAI] 接管单位失败: " + ex.getMessage());
                }
            }

            if (scanned > 0) {
                Log.info("[RedTeamAI] 本轮扫描 " + scanned + " 个红队单位，新绑定 " + bound + " 个");
            }
            return bound;

        } catch (Exception ex) {
            Log.err("[RedTeamAI] takeOverAll 异常: " + ex.getMessage());
            return 0;
        }
    }
}
