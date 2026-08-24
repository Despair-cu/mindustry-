package redteamai;

import arc.math.geom.Vec2;
import mindustry.ai.types.GroundAI;
import mindustry.gen.Building;
import mindustry.gen.Unit;

public class EnhancedGroundAI extends GroundAI {

    @Override
    public void updateUnit() {
        try {
            if (unit == null || unit.dead) return;

            // 1. 刷新目标（原版逻辑）
            updateTargeting();

            if (target == null) {
                super.updateUnit();
                return;
            }

            // 2. 目标存活检查
            boolean targetAlive = false;
            if (target instanceof Unit) {
                targetAlive = !((Unit) target).dead();
            } else if (target instanceof Building) {
                targetAlive = !((Building) target).dead();
            }

            if (!targetAlive) {
                target = null;
                super.updateUnit();
                return;
            }

            // ===== 核心：让单位转向目标 =====
            unit.lookAt(target.x(), target.y());

            // 3. 距离判断
            float dist = unit.dst(target);
            float range = unit.range();
            float desired = range * 0.85f;

            if (dist > desired + 15f) {
                moveTo(new Vec2(target.x(), target.y()), desired);
            } else {
                unit.moveAt(Vec2.ZERO); // 站桩输出
            }

            // 4. 开火 + 动画
            updateWeapons();
            updateVisuals();

        } catch (Exception ex) {
            // 静默，避免刷屏
        }
    }
}
