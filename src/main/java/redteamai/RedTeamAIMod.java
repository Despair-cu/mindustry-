package redteamai;

import arc.Events;
import arc.util.Log;
import mindustry.game.EventType.UnitCreateEvent;
import mindustry.mod.Mod;

public class RedTeamAIMod extends Mod {

    @Override
    public void init() {
        Log.info("[RedTeamAI] Mod initialized!");

        // 监听单位生成事件
        Events.on(UnitCreateEvent.class, event -> {
            var u = event.unit;
            if (u == null || u.dead) return;

            // 只处理红队（Crux）的地面单位
            if (u.team != mindustry.game.Team.crux) return;
            if (u.isFlying()) return;

            // 替换为自定义 AI
            EnhancedGroundAI ai = new EnhancedGroundAI();
            u.controller(ai);
            ai.unit(u);
            ai.init();
            Log.info("[RedTeamAI] Replaced AI for " + u.type.name);
        });
    }
}
