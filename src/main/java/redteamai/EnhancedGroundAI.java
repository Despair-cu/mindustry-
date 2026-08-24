package redteamai;

import arc.math.geom.Vec2;
import mindustry.ai.types.GroundAI;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.power.PowerGenerator;
import mindustry.world.blocks.units.UnitFactory;

public class EnhancedGroundAI extends GroundAI {

    private static final float TILE = 8f;
    private static final float RETREAT_DIST = 2f;
    private static final float RALLY_RADIUS = 60f;
    private static final float SIDE_BIAS = 90f;
    private static final float RETARGET = 24f; // 目标点变化超24像素才重新寻路

    private boolean attacking = false;
    private float lastMx = Float.NaN, lastMy = Float.NaN;

    @Override
    public void updateUnit() {
        try {
            if (unit == null || unit.dead()) return;

            if (target == null || isTargetDead(target)) target = findBestTarget();
            updateTargeting();
            if (target == null) { super.updateUnit(); return; }

            float tx = target.x(), ty = target.y();
            float dist = unit.dst(target);
            float range = unit.range();
            float retreatAt = range - RETREAT_DIST * TILE;

            // ===== 敌进我退 =====
            if (dist < retreatAt) {
                float dx = unit.x - tx, dy = unit.y - ty;
                float len = (float) Math.sqrt(dx*dx + dy*dy);
                if (len > 0.01f) {
                    // 后退目标点：远离目标 60 像素
                    float bx = unit.x + (dx/len) * 60f;
                    float by = unit.y + (dy/len) * 60f;
                    doMoveTo(bx, by, 0);
                }
                unit.lookAt(tx, ty);
                updateWeapons(); updateVisuals();
                return;
            }

            // ===== 聚兵判定 =====
            int friends = RedTeamAIMod.cruxGroundCount();
            int threshold = rushThreshold();
            Vec2 rally = rallyPoint();

            if (!attacking) {
                if (rally != null && unit.dst(rally) > RALLY_RADIUS) {
                    // 去集合点（寻路）
                    doMoveTo(rally.x, rally.y, RALLY_RADIUS * 0.5f);
                    unit.lookAt(tx, ty); updateWeapons(); updateVisuals();
                    return;
                }
                if (friends >= threshold) {
                    attacking = true;
                } else {
                    // 已在集合点附近，原地待命（不移动）
                    unit.moveAt(Vec2.ZERO);
                    unit.lookAt(tx, ty); updateWeapons(); updateVisuals();
                    return;
                }
            }

            // ===== 进攻阶段 =====
            boolean isUnitTarget = (target instanceof Unit);

            if (isUnitTarget) {
                // 对单位：保持拉扯距离（寻路靠近/后退）
                float desired = retreatAt * 0.85f;
                if (dist > desired + 12f) {
                    doMoveTo(tx, ty, desired);
                } else if (dist < desired - 12f) {
                    // 太近就后退（寻路远离）
                    float dx = unit.x - tx, dy = unit.y - ty;
                    float len = (float)Math.sqrt(dx*dx + dy*dy);
                    if (len > 0.01f) {
                        doMoveTo(unit.x + (dx/len)*50f, unit.y + (dy/len)*50f, 0);
                    }
                }
            } else {
                // 对建筑：威胁度决定绕道 or 直冲
                float turretThreat = threatAt(tx, ty);
                int hotspot = RedTeamAIMod.threatAtPoint(tx, ty);
                float totalThreat = turretThreat + hotspot * 2f;

                if (totalThreat >= 6f) {
                    // 绕侧：选热点少的一侧偏移
                    float dx = tx - unit.x, dy = ty - unit.y;
                    float len = (float)Math.sqrt(dx*dx + dy*dy);
                    if (len > 0.01f) {
                        float lx = -dy/len, ly = dx/len;
                        float rx = dy/len, ry = -dx/len;
                        int lHot = RedTeamAIMod.threatAtPoint(unit.x+lx*SIDE_BIAS, unit.y+ly*SIDE_BIAS);
                        int rHot = RedTeamAIMod.threatAtPoint(unit.x+rx*SIDE_BIAS, unit.y+ry*SIDE_BIAS);
                        float mdx, mdy;
                        if (lHot <= rHot) { mdx = lx; mdy = ly; }
                        else { mdx = rx; mdy = ry; }
                        doMoveTo(unit.x + mdx*SIDE_BIAS, unit.y + mdy*SIDE_BIAS, 0);
                    }
                } else {
                    // 低威胁：寻路卡射程
                    float desired = range * 0.8f;
                    if (dist > desired + 12f) {
                        doMoveTo(tx, ty, desired);
                    }
                    // 到了射程内：停（moveTo 的 radius 会自然停）
                }
            }

            unit.lookAt(tx, ty);
            updateWeapons(); updateVisuals();

        } catch (Exception ex) { /* silent */ }
    }

    // ===== 统一寻路移动入口（带缓存，避免每帧重寻路抖动）=====
    private void doMoveTo(float x, float y, float radius) {
        if (Float.isNaN(lastMx) || Float.isNaN(lastMy)) {
            moveTo(new Vec2(x, y), radius);
            lastMx = x; lastMy = y; return;
        }
        float dx = x - lastMx, dy = y - lastMy;
        if (dx*dx + dy*dy > RETARGET*RETARGET) {
            moveTo(new Vec2(x, y), radius);
            lastMx = x; lastMy = y;
        }
        // 否则沿用上一次 moveTo 的路径（不重复调用）
    }

    private int rushThreshold() {
        int base = 4;
        int threat = RedTeamAIMod.totalThreat();
        return Math.max(base, Math.min(4 + threat, 12));
    }

    private Vec2 rallyPoint() {
        Building core = mindustry.Vars.state.teams.get(mindustry.game.Team.crux).core();
        if (core != null && !core.dead) return new Vec2(core.x, core.y);
        return null;
    }

    private boolean isTargetDead(Teamc t) {
        if (t == null) return true;
        if (t instanceof Unit) return ((Unit)t).dead();
        if (t instanceof Building) return ((Building)t).dead;
        return true;
    }

    private float threatAt(float tx, float ty) {
        float r2 = 160f*160f, sum = 0f;
        for (Building b : Groups.build) {
            if (b == null || b.dead) continue;
            if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;
            if (!(b.block instanceof Turret)) continue;
            if (b.power != null && b.power.status < 0.1f) continue;
            float dx = b.x-tx, dy = b.y-ty;
            if (dx*dx+dy*dy > r2) continue;
            int size = b.block.size; sum += size*size;
        }
        return sum;
    }

    private Building findBestTarget() {
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
        if (b.block instanceof Turret) return 0;
        return 1;
    }
}
