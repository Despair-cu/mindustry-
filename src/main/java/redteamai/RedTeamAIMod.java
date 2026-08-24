package redteamai;

import arc.Events;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.mod.Mod;

public class RedTeamAIMod extends Mod {

    @Override
    public void init() {
        Vars.log.info("[RedTeamAI] Mod 初始化");

        Events.run(WorldLoadEvent.class, () -> {
            Vars.log.info("[RedTeamAI] 世界加载，执行首次全量接管...");
            takeOverAll();
        });

        // 使用 Events.run 注册每帧轮询
        Events.run(Trigger.update, () -> {
            pollAndReplace();
        });

        Vars.log.info("[RedTeamAI] 监听器注册完成 (使用轮询模式)");
    }

    private void pollAndReplace() {
        // 每 10 帧检查一次（Time.time 是游戏时间，每帧 +1）
        if (Time.time % 10 != 0) return;

        int replaced = 0;
        for (Unit u : Groups.unit) {
            if (u == null || u.dead) continue;

            // 只处理红队和玩家队，跳过飞行单位
            if (u.team != Team.crux && u.team != Team.sharded) continue;
            if (u.isFlying()) continue;

            // 如果已经是自定义 AI，跳过
            if (u.controller() instanceof EnhancedGroundAI) continue;

            try {
                EnhancedGroundAI ai = new EnhancedGroundAI();
                u.controller(ai);
                ai.unit(u);
                ai.init();
                replaced++;

                if (replaced % 5 == 0) {
                    Vars.log.info("[RedTeamAI] 本轮已替换: " + replaced + " 个");
                }
            } catch (Exception ex) {
                Vars.log.err("[RedTeamAI] 替换失败: " + u.type.name + " - " + ex.getMessage());
            }
        }

        if (replaced > 0) {
            Vars.log.info("[RedTeamAI] ✅ 轮询接管完成，本次替换了 " + replaced + " 个单位");
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
                Vars.log.err("[RedTeamAI] 初始替换失败: " + ex.getMessage());
            }
        }
        Vars.log.info("[RedTeamAI] >>> 初始接管了 " + count + " 个地面单位");
    }
}
