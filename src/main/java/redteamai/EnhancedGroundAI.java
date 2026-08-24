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
    private static final float RETREAT_DIST = 2f;     // 射程-2格后退
    private static final float RALLY_RADIUS = 60f;    // 距集合点多远算"已集结"
    private static final float SIDE_BIAS = 70f;       // 绕侧偏移量
    private static final int   MAX_WAVES_LOOKAHEAD = 5; // 最多看未来5波

    // 状态：是否已在进攻阶段
    private boolean attacking = false;

    @Override
    public void updateUnit() {
        try {
            if (unit == null || unit.dead()) return;

            // 刷新目标
            if (target == null || isTargetDead(target)) target = findBestTarget();
            updateTargeting();
            if (target == null) { super.updateUnit(); return; }

            float tx = target.x(), ty = target.y();
            float dist = unit.dst(target);
            float range = unit.range();
            float retreatAt = range - RETREAT_DIST * TILE;

            // 敌进我退
            if (dist < retreatAt) {
                float dx = unit.x - tx, dy = unit.y - ty;
                float len = (float) Math.sqrt(dx*dx + dy*dy);
                if (len > 0.01f) { float sp = unit.speed(); unit.move((dx/len)*sp, (dy/len)*sp); }
                unit.lookAt(tx, ty); updateWeapons(); updateVisuals(); return;
            }

            // === 聚兵判定 ===
            int friends = RedTeamAIMod.cruxGroundCount();
            int threshold = rushThreshold();
            Vec2 rally = rallyPoint();

            if (!attacking) {
                // 还没到进攻阈值 -> 先去集合点集结
                if (rally != null && unit.dst(rally) > RALLY_RADIUS) {
                    moveStraight(rally.x, rally.y, 1f);
                    unit.lookAt(tx, ty); updateWeapons(); updateVisuals(); return;
                }
                // 人数够了 -> 转入进攻
                if (friends >= threshold) {
                    attacking = true;
                    Log.info("[RedTeamAI] 聚兵完成("+friends+"/"+threshold+")，开始进攻!");
                } else {
                    // 人数不够且已在集合点附近 -> 原地待命
                    unit.moveAt(Vec2.ZERO);
                    unit.lookAt(tx, ty); updateWeapons(); updateVisuals(); return;
                }
            }

            // === 进攻阶段：绕侧 + 卡射程 + 偏外侧 ===
            boolean isUnitTarget = (target instanceof Unit);

            if (isUnitTarget) {
                // 对单位：保持拉扯
                float desired = retreatAt * 0.85f;
                if (dist > desired + 10f) moveStraight(tx, ty, 1f);
                else if (dist < desired - 10f) moveStraight(tx, ty, -1f);
            } else {
                // 对建筑：先看目标点威胁(炮塔+死亡热点)
                float turretThreat = threatAt(tx, ty);
                int hotspot = RedTeamAIMod.threatAtPoint(tx, ty);
                float totalThreat = turretThreat + hotspot * 2f;

                if (totalThreat >= 6f) {
                    // 高威胁：绕侧(偏向热点少的一侧)
                    float dx = tx - unit.x, dy = ty - unit.y;
                    float len = (float) Math.sqrt(dx*dx + dy*dy);
                    if (len > 0.01f) {
                        // 选左侧或右侧：比较两侧热点，走热点少的那侧
                        float leftX = -dy/len, leftY = dx/len;
                        float rightX = dy/len, rightY = -dx/len;
                        int leftHot = RedTeamAIMod.threatAtPoint(unit.x + leftX*SIDE_BIAS, unit.y + leftY*SIDE_BIAS);
                        int rightHot = RedTeamAIMod.threatAtPoint(unit.x + rightX*SIDE_BIAS, unit.y + rightY*SIDE_BIAS);
                        float bx, by;
                        if (leftHot <= rightHot) { bx = leftX; by = leftY; }
                        else { bx = rightX; by = rightY; }
                        float sp = unit.speed();
                        unit.move(bx*sp, by*sp);
                    }
                } else {
                    // 低威胁：直线卡射程
                    float desired = range * 0.8f;
                    if (dist > desired + 10f) moveStraight(tx, ty, 1f);
                }
            }

            unit.lookAt(tx, ty);
            updateWeapons();
            updateVisuals();

        } catch (Exception ex) { /* silent */ }
    }

    /** 聚兵阈值 = max(基础4, min(全场威胁相关量, 剩余波次出怪总量上限)) */
    private int rushThreshold() {
        int base = 4;
        int threat = RedTeamAIMod.totalThreat();
        int dynamic = Math.min(4 + threat, estimatedRemainingSpawns());
        return Math.max(base, dynamic);
    }

    /** 粗略估算剩余波次出怪总量(上限) */
    private int estimatedRemainingSpawns() {
        try {
            int current = mindustry.Vars.state.wave;
            int totalWaves = mindustry.Vars.state.rules.waves ? MAX_WAVES_LOOKAHEAD : 0;
            // 每波出怪量用当前波已出单位数近似
            int curCount = RedTeamAIMod.cruxGroundCount();
            return Math.max(curCount, totalWaves * Math.max(1, curCount));
        } catch (Exception e) { return 8; }
    }

    /** 集合点：最近己方核心/建筑 */
    private Vec2 rallyPoint() {
        Building core = mindustry.Vars.state.teams.get(mindustry.game.Team.crux).core();
        if (core != null && !core.dead) return new Vec2(core.x, core.y);
        // 退回：己方最近建筑
        for (Building b : Groups.build) {
            if (b != null && !b.dead && b.team == unit.team) return new Vec2(b.x, b.y);
        }
        return null;
    }

    /** 直线朝目标移动(dir=1 靠近, -1 远离) */
    private void moveStraight(float tx, float ty, float dir) {
        float dx = tx - unit.x, dy = ty - unit.y;
        float len = (float) Math.sqrt(dx*dx + dy*dy);
        if (len > 0.01f) { float sp = unit.speed(); unit.move((dx/len)*sp*dir, (dy/len)*sp*dir); }
    }

    private boolean isTargetDead(Teamc t) {
        if (t == null) return true;
        if (t instanceof Unit) return ((Unit)t).dead();
        if (t instanceof Building) return ((Building)t).dead;
        return true;
    }

    /** 目标点附近有电炮塔的占地威胁 */
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

    /** 优先发电机/工厂 */
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
