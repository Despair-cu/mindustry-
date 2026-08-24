package redteamai;

import arc.math.geom.Vec2;
import arc.util.Log;
import mindustry.ai.types.FlyingAI;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.blocks.units.UnitFactory;

public class EnhancedFlyingAI extends FlyingAI {

    private static final float RETREAT_DIST = 2f;
    private static final float DETECT_RANGE = 120f;   // 前方探测炮塔距离
    private static final float DETECT_WIDTH = 60f;    // 探测宽度（两侧）
    private static final float EVADE_DIST = 80f;      // 绕道偏移量
    private int frameCount = 0;

    @Override
    public void updateUnit() {
        try {
            if (unit == null || unit.dead()) return;
            frameCount++;

            // 空军目标：优先敌方核心，其次是发电机/工厂
            if (target == null || isTargetDead(target)) target = findBestTarget();
            updateTargeting();
            if (target == null) { super.updateUnit(); return; }

            float tx = target.x(), ty = target.y();
            float dist = unit.dst(target);
            float range = unit.range();
            float retreatAt = range - RETREAT_DIST * 8f;

            // 敌进我退
            if (dist < retreatAt) {
                float dx = unit.x - tx, dy = unit.y - ty;
                float len = (float)Math.sqrt(dx*dx + dy*dy);
                if (len > 0.01f) {
                    float bx = unit.x + (dx/len)*60f;
                    float by = unit.y + (dy/len)*60f;
                    moveSmooth(bx, by);
                }
                unit.lookAt(tx, ty); updateWeapons(); updateVisuals();
                return;
            }

            // 探测前方路径上是否有炮塔
            float dx = tx - unit.x, dy = ty - unit.y;
            float len = (float)Math.sqrt(dx*dx + dy*dy);
            boolean hasTurretAhead = false;

            if (len > 0.01f) {
                // 沿飞行方向前方 DETECT_RANGE 范围内搜索炮塔
                for (Building b : Groups.build) {
                    if (b == null || b.dead) continue;
                    if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;
                    if (!(b.block instanceof Turret)) continue;
                    if (b.power != null && b.power.status < 0.1f) continue;

                    // 计算炮塔是否在飞行路径上
                    float tdx = b.x - unit.x, tdy = b.y - unit.y;
                    float proj = (tdx * dx + tdy * dy) / len; // 投影到飞行方向
                    if (proj > 0 && proj < DETECT_RANGE) {
                        // 垂直距离
                        float perpX = tdx - (proj * dx / len);
                        float perpY = tdy - (proj * dy / len);
                        float perpDist = (float)Math.sqrt(perpX*perpX + perpY*perpY);
                        if (perpDist < DETECT_WIDTH) {
                            hasTurretAhead = true;
                            break;
                        }
                    }
                }
            }

            if (hasTurretAhead) {
                // 前方有炮塔：绕道（向侧方偏移）
                float nx = -dy/len, ny = dx/len; // 法线方向
                // 选一侧绕（简单起见固定选右侧）
                float evadeX = unit.x + nx * EVADE_DIST;
                float evadeY = unit.y + ny * EVADE_DIST;
                moveSmooth(evadeX, evadeY);
                if (frameCount % 60 == 0)
                    Log.info("[RedTeamAI][空] " + unit.type.name + " 绕炮塔飞行 -> (" + (int)evadeX + "," + (int)evadeY + ")");
            } else {
                // 前方无炮塔：直线飞向目标
                if (len > 0.01f) {
                    float sp = unit.speed();
                    unit.move((dx/len) * sp, (dy/len) * sp);
                }
                if (frameCount % 60 == 0)
                    Log.info("[RedTeamAI][空] " + unit.type.name + " 直飞目标 dist=" + (int)dist);
            }

            unit.lookAt(tx, ty);
            updateWeapons(); updateVisuals();

        } catch (Exception ex) { Log.err("[RedTeamAI][空] 异常: " + ex.getMessage()); }
    }

    /** 平滑移动（避免瞬移感） */
    private void moveSmooth(float x, float y) {
        float dx = x - unit.x, dy = y - unit.y;
        float sp = unit.speed() * 0.8f;
        float len = (float)Math.sqrt(dx*dx + dy*dy);
        if (len > 0.01f) {
            unit.move((dx/len) * sp, (dy/len) * sp);
        }
    }

    private boolean isTargetDead(Teamc t) {
        if (t == null) return true;
        if (t instanceof Unit) return ((Unit)t).dead();
        if (t instanceof Building) return ((Building)t).dead;
        return true;
    }

    /** 空军目标选取：优先敌方核心，其次发电机/工厂 */
    private Building findBestTarget() {
        // 先找敌方核心
        for (mindustry.game.TeamData td : mindustry.Vars.state.teams.getActive()) {
            if (td == null || td.team == unit.team) continue;
            Building core = td.core();
            if (core != null && !core.dead) return core;
        }

        // 核心没有就找发电机/工厂
        Building best = null; float bestD = Float.MAX_VALUE; int bestP = 0;
        for (Building b : Groups.build) {
            if (b == null || b.dead) continue;
            if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;
            int p = priority(b); if (p == 0) continue;
            float d = unit.dst(b);
            if (p > bestP || (p == bestP && d < bestD)) { bestP = p; bestD = d; best = b; }
        }
        return best;
    }

    private int priority(Building b) {
        if (b.block instanceof PowerGenerator) return 3;
        if (b.block instanceof UnitFactory) return 3;
        return 1;
    }
}
