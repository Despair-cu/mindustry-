package redteamai;

import arc.math.geom.Vec2;
import mindustry.ai.types.GroundAI;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.gen.Building;

public class EnhancedGroundAI extends GroundAI {

    @Override
    public void updateMovement() {
        // 检查目标是否有效（Teamc 没有 dead() 方法，需要按实际类型判断）
        if (target == null) {
            super.updateMovement();
            return;
        }
        
        boolean targetAlive = false;
        if (target instanceof Unit) {
            targetAlive = !((Unit) target).dead();
        } else if (target instanceof Building) {
            targetAlive = !((Building) target).dead();
        }
        if (!targetAlive) {
            super.updateMovement();
            return;
        }

        float tx = target.x();
        float ty = target.y();
        float range = unit.range();
        float desired = range * 0.95f;
        float dist = unit.dst(target);

        // 手动统计目标附近 180 范围内的敌方炮塔数量
        int turretCount = 0;
        for (var u : Groups.unit) {
            if (u == null || u.dead || u.team == unit.team) continue;
            if (u.buildOn() != null && u.buildOn().block.hasTurret) {
                float dx = u.x - tx;
                float dy = u.y - ty;
                if (dx * dx + dy * dy <= 180f * 180f) {
                    turretCount++;
                }
            }
        }

        if (turretCount >= 3) {
            // 绕道：垂直偏移 60 像素
            float dx = tx - unit.x;
            float dy = ty - unit.y;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 0.01f) {
                float perpX = -dy / len * 60f;
                float perpY = dx / len * 60f;
                moveTo(new Vec2(tx + perpX, ty + perpY), 0);
            }
        } else {
            // 卡射程：距离大于 desired+10 就前进，否则站定
            if (dist > desired + 10f) {
                moveTo(new Vec2(tx, ty), 0);
            }
        }
    }
}
