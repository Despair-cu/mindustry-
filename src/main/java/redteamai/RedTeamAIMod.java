package redteamai;

import arc.Events;
import arc.util.Log;
import mindustry.game.EventType.UnitCreateEvent;
import mindustry.gen.Unit;
import mindustry.game.Team;

/**
 * Mod 入口。实现 mindustry.mod.ModInterface，
 * 游戏加载 Mod 时会实例化并调用 init()。
 */
public class RedTeamAIMod implements mindustry.mod.ModInterface {
    @Override
    public void init() {
        Log.info("[RedTeamAI] Mod loaded. Replacing Crux ground-unit AI...");

        // 每生成一个单位，若是红队(Crux)地面单位则替换成自定义 AI
        Events.on(UnitCreateEvent.class, event -> {
            Unit u = event.unit;
            if (u == null || u.dead) return;
            if (u.team != Team.crux) return;   // 只处理红队(Crux)
            if (u.isFlying()) return;          // 飞行单位暂不处理

            EnhancedGroundAI ai = new EnhancedGroundAI();
            u.controller(ai);
            ai.unit(u);
            ai.init();
        });
    }
}
