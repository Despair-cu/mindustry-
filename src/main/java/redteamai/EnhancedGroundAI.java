package redteamai;

import arc.math.geom.Vec2;
import mindustry.ai.types.GroundAI;
import mindustry.entities.Units;
import mindustry.gen.Unit;

public class EnhancedGroundAI extends GroundAI {

    @Override
    public void updateMovement() {
        if (target == null || target.dead()) {
            super.updateMovement();
            return;
        }

        float tx = target.x();
        float ty = target.y();
        float range = unit.range();
        float desired = range * 0.95f;
        float dist = unit.dst(target);

        // 统计目标附近 180 范围内的敌方炮塔数量
        int turretCount = Units.getEnemyUnits(unit.team, tx, ty, 180f,
            e -> e.buildOn() != null && e.buildOn().block.hasTurret
        ).size;

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
