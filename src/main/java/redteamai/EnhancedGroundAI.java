package redteamai;

import arc.math.geom.Vec2;
import mindustry.ai.types.GroundAI;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.world.blocks.defense.turrets.Turret;

public class EnhancedGroundAI extends GroundAI {

    // ===== 可调参数 =====
    private static final float THREAT_RADIUS = 160f;   // 只算自己寻路周边这么远（像素）
    private static final float HIGH_THREAT = 6f;        // ≥ 此值：绕道
    private static final float LOW_THREAT  = 2f;        // ≤ 此值：直接冲（无视炮塔）
    // =====================

    @Override
    public void updateUnit() {
        try {
            if (unit == null || unit.dead) return;

            // 没目标或目标死了 → 重新选（优先发电机/工厂）
            if (target == null || target.dead()) {
                target = findBestTarget();
            }
            updateTargeting();
            if (target == null) { super.updateUnit(); return; }

            float tx = target.x();
            float ty = target.y();
            float dist = unit.dst(target);
            float range = unit.range();
            float desired = range * 0.85f;

            // 计算目标点附近的"威胁度"（仅局部）
            float threat = threatAt(tx, ty);

            if (threat >= HIGH_THREAT) {
                // 高威胁：绕道（垂直偏移，远离炮塔群）
                float dx = tx - unit.x, dy = ty - unit.y;
                float len = (float) Math.sqrt(dx*dx + dy*dy);
                if (len > 0.01f) {
                    float perpX = -dy/len * 90f;
                    float perpY =  dx/len * 90f;
                    moveTo(new Vec2(tx + perpX, ty + perpY), 0);
                }
            } else if (threat <= LOW_THREAT) {
                // 低威胁：直接冲过去站桩输出
                if (“dist > desired + 15f) {
                    moveTo(new Vec2(tx, ty), desired);
                } else {
                    unit.moveAt(Vec2.ZERO);
                }
            } else {
                // 中等威胁：正常卡射程，不绕道但也不硬冲
                if (dist > desired + 15f) {
                    moveTo(new Vec2(tx, ty), desired);
                } else {
                    unit.moveAt(Vec2.ZERO);
                }
            }

            unit.lookAt(tx, ty);
            updateWeapons();
            updateVisuals();
        } catch (Exception ex) { /* 静默 */ }
    }

    // ===== 威胁度：目标点周边有弹药炮塔的占地大小之和 =====
    private float threatAt(float tx, float ty) {
        float r2 = THREAT_RADIUS * THREAT_RADIUS;
        float sum = 0f;

        // 用局部建筑查询（不遍历全图，省算力）
        for (Building b : mindustry.Vars.indexer.allBuildings()) {
            if (b == null || b.dead) continue;
            if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;
            if (!(b.block instanceof Turret)) continue;

            // 没弹药 / 没电 → 不算威胁
            if (b.ammo < 1) continue;
            if (b.power != null && b.power.status < 0.1f) continue;

            float dx = b.x - tx, dy = b.y - ty;
            if (dx*dx + dy*dy > r2) continue;

            // 占地大小 = block.size（格），按格数加权
            int size = b.block.size;
            sum += size * size; // 面积加权，大炮塔威胁更高
        }
        return sum;
    }

    // ===== 目标选取：优先发电机/工厂（有电有原料），炮塔不打 =====
    private Building findBestTarget() {
        Building best = null;
        float bestDist = Float.MAX_VALUE;
        int bestPri = 0;

        for (Building b : mindustry.Vars.indexer.allBuildings()) {
            if (b == null || b.dead) continue;
            if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;

            int p = priority(b);
            if (p == 0) continue;
            float d = unit.dst(b);
            if (p > bestPri || (p == bestPri && d < bestDist)) {
                bestPri = p; bestDist = d; best = b;
            }
        }
        return best;
    }

    private int priority(Building b) {
        if (b.block instanceof mindustry.world.blocks.power.PowerGenerator) {
            return (b.power != null && b.power.status > 0.1f) ? 3 : 0;
        }
        if (b.block instanceof mindustry.world.blocks.units.UnitFactory) {
            return (b.productionEfficiency > 0) ? 3 : 0;
        }
        if (b.block instanceof Turret) return 0; // 炮塔不打，只当威胁
        return 1;
    }
}
