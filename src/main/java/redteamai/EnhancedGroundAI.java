package redteamai;

import arc.math.geom.Vec2;
import mindustry.ai.types.GroundAI;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.world.blocks.defense.turrets.Turret;

public class EnhancedGroundAI extends GroundAI {

    @Override
    public void updateUnit() {
        // 如果没有目标，走原版逻辑（原版会自动找目标）
        if (target == null) {
            super.updateUnit();
            return;
        }

        // 目标有效性检查
        boolean targetAlive = false;
        if (target instanceof Unit) {
            targetAlive = !((Unit) target).dead();
        } else if (target instanceof Building) {
            targetAlive = !((Building) target).dead();
        }
        
        if (!targetAlive) {
            super.updateUnit();
            return;
        }

        // --- 以下是自定义移动逻辑 ---
        float tx = target.x();
        float ty = target.y();
        float range = unit.range();
        float desired = range * 0.9f;  // 卡射程留余量
        float dist = unit.dst(target);

        // 统计目标附近炮塔密度（绕道判定）
        int turretCount = 0;
        for (Unit u : mindustry.Vars.state.teams.enemiesOf(unit.team)) {
            if (u == null || u.dead || u.buildOn() == null) continue;
            if (u.buildOn().block instanceof Turret) {
                float dx = u.x - tx;
                float dy = u.y - ty;
                if (dx * dx + dy * dy <= 180f * 180f) {
                    turretCount++;
                }
            }
        }

        if (turretCount >= 3) {
            // 绕道：垂直偏移
            float dx = tx - unit.x;
            float dy = ty - unit.y;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 0.01f) {
                float perpX = -dy / len * 60f;
                float perpY = dx / len * 60f;
                moveTo(new Vec2(tx + perpX, ty + perpY), 0);
            }
        } else {
            // 卡射程逻辑
            if (dist > desired + 10f) {
                moveTo(new Vec2(tx, ty), 0); // 走近
            } else {
                unit.moveAt(Vec2.ZERO); // 站定开火
            }
        }

        // --- 关键：不碰转向和开火，让原版机制自己处理 ---
        // 调用原版的 updateTargeting() 和 updateWeapons()
        // 原版 updateWeapons() 内部会用 shouldShoot() 判定，保证炮管转到位才开火
        updateTargeting();
        updateWeapons();
        updateVisuals();
    }
}
