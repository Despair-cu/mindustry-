package redteamai;

import arc.Events;
import arc.util.Time;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.mod.Mod;

public class RedTeamAIMod extends Mod {

    @Override
    public void init() {
        System.out.println("[RedTeamAI] Mod 初始化");

        Events.run(WorldLoadEvent.class, () -> {
            System.out.println("[RedTeamAI] 世界加载，执行首次全量接管...");
            takeOverAll();
        });

        Events.run(Trigger.update, () -> {
            pollAndReplace();
        });

        System.out.println("[RedTeamAI] 监听器注册完成 (使用轮询模式)");
    }

    private void pollAndReplace() {
        if (Time.time % 10 != 0) return;

        int replaced = 0;
        for (Unit u : Groups.unit) {
            if (u == null || u.dead) continue;
            if (u.team != Team.crux && u.team != Team.sharded) continue;
            if (u.isFlying()) continue;
            if (u.controller() instanceof EnhancedGroundAI) continue;

            try {
                EnhancedGroundAI ai = new EnhancedGroundAI();
                u.controller(ai);
                ai.unit(u);
                ai.init();
                replaced++;
                if (replaced % 5 == 0) {
                    System.out.println("[RedTeamAI] 本轮已替换: " + replaced + " 个");
                }
            } catch (Exception ex) {
                System.out.println("[RedTeamAI] 替换失败: " + u.type.name + " - " + ex.getMessage());
            }
        }

        if (replaced > 0) {
            System.out.println("[RedTeamAI] ✅ 轮询接管完成，本次替换了 " + replaced + " 个单位");
        }
    }

    private void takeOverAll() {
        int count = 0;
        for (Unit u : Groups.unit) {
            if (u == null || u.dead) continue;
            if (u.team != Team.crux && u.team != Team.sharded) continue;
            if (u.isFlying()) continue;

            try {
                if (!(u.controller() instanceof EnhancedGroundAI)) {
                    EnhancedGroundAI ai = new EnhancedGroundAI();
                    u.controller(ai);
                    ai.unit(u);
                    ai.init();
                    count++;
                }
            } catch (Exception ex) {
                System.out.println("[RedTeamAI] 初始替换失败: " + ex.getMessage());
            }
        }
        System.out.println("[RedTeamAI] >>> 初始接管了 " + count + " 个地面单位");
    }
}
