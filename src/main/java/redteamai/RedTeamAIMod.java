package redteamai;

import arc.Events;
import arc.util.Log;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.mod.Mod;

public class RedTeamAIMod extends Mod {

    @Override
    public void init() {
        Log.info("[RedTeamAI] Mod 初始化");

        // 世界加载完成，接管场上所有红队地面单位
        Events.on(WorldLoadEvent.class, e -> {
            Log.info("[RedTeamAI] 世界加载完成，开始接管红队单位...");
            takeOverAllCruxGroundUnits();
        });

        // 工厂/重建机生产的单位
        Events.on(UnitCreateEvent.class, e -> {
            Unit u = e.unit;
            if (u == null || u.dead) return;
            if (u.team != Team.crux || u.isFlying()) return;
            replaceAI(u);
        });

        Log.info("[RedTeamAI] 监听器注册完成");
    }

    private void takeOverAllCruxGroundUnits() {
        int count = 0;
        for (Unit u : Groups.unit) {
            if (u == null || u.dead) continue;
            if (u.team != Team.crux) continue;
            if (u.isFlying()) continue;
            replaceAI(u);
            count++;
        }
        Log.info("[RedTeamAI] >>> 本波接管了 " + count + " 个红队地面单位");
    }

    private void replaceAI(Unit u) {
        // 已经是自定义AI就跳过，避免重复替换
        if (u.controller() instanceof EnhancedGroundAI) return;
        
        try {
            EnhancedGroundAI ai = new EnhancedGroundAI();
            u.controller(ai);
            ai.unit(u);
            ai.init();
            Log.info("[RedTeamAI] ✅ 替换成功: " + u.type.name);
        } catch (Exception ex) {
            Log.err("[RedTeamAI] ❌ 替换失败: " + u.type.name + " - " + ex.getMessage());
        }
    }
}
