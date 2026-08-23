package redteamai;

import mindustry.ai.types.GroundAI;
import mindustry.entities.Units;
import mindustry.gen.Unit;

/**
 * 自定义地面 AI：
 * - 目标附近炮塔密集(>=3)时横向偏移绕道
 * - 否则卡在武器射程 95% 位置站桩输出
 */
public class EnhancedGroundAI extends GroundAI {

    @Override
    public void updateMovement() {
        Unit u = unit;
        if (u == null || !u.isValid()) return;

        if (target == null || !target.isValid()) {
            super.updateMovement();
            return;
        }

        float tx = target.x(), ty = target.y();
        float dist = u.dst(target);
        float range = u.range();
        float desired = range * 0.95f;

        // 统计目标 180 范围内敌方(对单位而言是"对家")炮塔类建筑数量
        int turretCount = Units.getEnemyUnits(u.team, tx, ty, 180f,
            e -> e.buildOn() != null && e.buildOn().block.hasTurret).size;

        if (turretCount >= 3) {
            // 炮塔密集：垂直偏移绕道
            float dx = tx - u.x, dy = ty - u.y;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 0) {
                float perpX = -dy / len * 60f;
                float perpY =  dx / len * 60f;
                moveTo(tx + perpX, ty + perpY, 0);
                return;
            }
        }

        // 卡射程：过远则推进，够近则站定由 updateWeapons 开火
        if (dist > desired + 10f) {
            moveTo(tx, ty, 0);
        }
    }
}
