package redteamai;

import arc.math.geom.Vec2;
import mindustry.ai.types.GroundAI;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.world.blocks.defense.turrets.Turret;

public class EnhancedGroundAI extends GroundAI {

    @Override
    public void updateUnit() {
        // 无目标或目标死亡 → 走原版
        if (target == null) {
            super.updateUnit();
            return;
        }
        boolean alive = false;
        if (target instanceof Unit) alive = !((Unit) target).dead();
        else if (target instanceof Building) alive = !((Building) target).dead();
        if (!alive) {
            super.updateUnit();
            return;
        }

        float tx = target.x();
        float ty = target.y();
        float range = unit.range();
        float desired = range * 0.9f;
        float dist = unit.dst(target);

        // 统计目标附近180范围内敌方炮塔数量
        int turretCount = 0;
        for (Unit u : Groups.unit) {
            if (u == null || u.dead) continue;
            if (u.team == unit.team || u.team == mindustry.game.Team.derelict) continue;
            Building b = u.buildOn();
            if (b != null && b.block instanceof Turret) {
                float dx = u.x - tx;
                float dy = u.y - ty;
                if (dx * dx + dy * dy <= 32400f) { // 180^2
                    turretCount++;
                }
            }
        }

        if (turretCount >= 3) {
            // 绕道：垂直偏移60像素
            float dx = tx - unit.x;
            float dy = ty - unit.y;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 0.01f) {
                float perpX = -dy / len * 60f;
                float perpY = dx / len * 60f;
                moveTo(new Vec2(tx + perpX, ty + perpY), 0);
            }
        } else {
            // 卡射程：走近后站定
            if (dist > desired + 10f) {
                moveTo(new Vec2(tx, ty), 0);
            } else {
                unit.moveAt(Vec2.ZERO);
            }
        }

        // 原版处理瞄准和开火（不碰 shouldShoot，不扭脖子）
        updateTargeting();
        updateWeapons();
        updateVisuals();
    }
}
