package redteamai;

import arc.Events;
import arc.util.Log;
import mindustry.game.EventType.UnitCreateEvent;
import mindustry.game.Team;
import mindustry.mod.Mod;
import mindustry.gen.Unit;

public class RedTeamAIMod extends Mod {

    public RedTeamAIMod() {
        Log.info("[RedTeamAI] ===== 构造函数被调用！Mod 类已加载 =====");
    }

    @Override
    public void init() {
        Log.info("[RedTeamAI] ===== init() 被调用！Mod 初始化完成 =====");

        Events.on(UnitCreateEvent.class, event -> {
            Unit u = event.unit;
            if (u == null) {
                Log.info("[RedTeamAI] [事件] UnitCreateEvent 触发，但 unit 为 null");
                return;
            }

            Log.info("[RedTeamAI] [事件] 单位生成: " + u.type.name
                + " | 队伍: " + u.team
                + " | 是否飞行: " + u.isFlying()
                + " | 是否死亡: " + u.dead
                + " | 当前Controller: " + u.controller().getClass().getSimpleName()
            );

            // 只处理红队（Crux）的地面单位
            if (u.team != Team.crux) {
                Log.info("[RedTeamAI] [跳过] 不是红队(crux)，实际队伍=" + u.team);
                return;
            }
            if (u.isFlying()) {
                Log.info("[RedTeamAI] [跳过] 飞行单位不处理");
                return;
            }

            // 替换为自定义 AI
            Log.info("[RedTeamAI] >>> 开始替换 AI: " + u.type.name + " <<<");
            EnhancedGroundAI ai = new EnhancedGroundAI();
            u.controller(ai);
            ai.unit(u);
            ai.init();
            Log.info("[RedTeamAI] >>> 替换完成！新Controller: " + u.controller().getClass().getSimpleName() + " <<<");
        });

        Log.info("[RedTeamAI] ===== UnitCreateEvent 监听器已注册 =====");
    }

    @Override
    public void loadContent() {
        Log.info("[RedTeamAI] loadContent() 被调用");
        super.loadContent();
    }
}