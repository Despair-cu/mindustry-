package redteamai;

import arc.math.geom.Vec2;
import arc.util.Log;
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
    private static final float RALLY_RADIUS = 80f;   // 集合点判定半径
    private static final float SIDE_BIAS = 90f;

    // 全局进攻标志：一旦全军开始进攻，所有兵都不再回集合点
    private static boolean globalAttacking = false;
    private int frameCount = 0;

    @Override
    public void updateUnit() {
        try {
            if (unit == null || unit.dead()) return;
            frameCount++;

            if (target == null || isTargetDead(target)) {
                target = findBestTarget();
            }
            updateTargeting();
            if (target == null) { super.updateUnit(); return; }

            float tx = target.x(), ty = target.y();
            float dist = unit.dst(target);
            float range = unit.range();
            float retreatAt = range - RETREAT_DIST * TILE;

            // 敌进我退（任何时候都生效）
            if (dist < retreatAt) {
                float dx = unit.x - tx, dy = unit.y - ty;
                float len = (float)Math.sqrt(dx*dx + dy*dy);
                if (len > 0.01f) {
                    moveTo(new Vec2(unit.x + (dx/len)*60f, unit.y + (dy/len)*60f), 0);
                }
                unit.lookAt(tx, ty); updateWeapons(); updateVisuals(); updateMovement();
                return;
            }

            Vec2 rally = rallyPoint();

            // ===== 集结阶段 =====
            if (!globalAttacking) {
                // 还没全军进攻：先去核心集合
                if (rally != null && unit.dst(rally) > RALLY_RADIUS) {
                    // 朝核心移动（挤过去集合）
                    moveTo(new Vec2(rally.x, rally.y), RALLY_RADIUS * 0.4f);
                    if (frameCount % 60 == 0)
                        Log.info("[RedTeamAI] " + unit.type.name + " 集合 " + RedTeamAIMod.cruxGroundCount() + "/" + rushThreshold());
                    unit.lookAt(tx, ty); updateWeapons(); updateVisuals(); updateMovement();
                    return;
                }

                // 已在核心旁：检查人数是否达标
                int friends = RedTeamAIMod.cruxGroundCount();
                if (friends >= rushThreshold()) {
                    globalAttacking = true;
                    Log.info("[RedTeamAI] >>> 全军进攻! 人数=" + friends);
                } else {
                    // 人数不够且在集合点旁：原地待命
                    unit.moveAt(Vec2.ZERO);
                    if (frameCount % 60 == 0)
                        Log.info("[RedTeamAI] " + unit.type.name + " 等待 " + friends + "/" + rushThreshold());
                    unit.lookAt(tx, ty); updateWeapons(); updateVisuals();
                    return;
                }
            }

            // ===== 进攻阶段（挤成一团朝目标推进）=====
            boolean isUnitTarget = (target instanceof Unit);

            if (isUnitTarget) {
                float desired = retreatAt * 0.85f;
                if (dist > desired + 12f) {
                    moveTo(new Vec2(tx, ty), desired);
                } else if (dist < desired - 12f) {
                    float dx = unit.x - tx, dy = unit.y - ty;
                    float len = (float)Math.sqrt(dx*dx + dy*dy);
                    if (len > 0.01f) moveTo(new Vec2(unit.x + (dx/len)*50f, unit.y + (dy/len)*50f), 0);
                }
                if (frameCount % 60 == 0)
                    Log.info("[RedTeamAI] " + unit.type.name + " 进攻单位目标 dist=" + (int)dist);
            } else {
                float turretThreat = threatAt(tx, ty);
                int hotspot = RedTeamAIMod.threatAtPoint(tx, ty);
                float totalThreat = turretThreat + hotspot * 2f;

                if (totalThreat >= 6f) {
                    // 高威胁绕侧
                    float dx = tx - unit.x, dy = ty - unit.y;
                    float len = (float)Math.sqrt(dx*dx + dy*dy);
                    if (len > 0.01f) {
                        float lx = -dy/len, ly = dx/len;
                        float rx = dy/len, ry = -dx/len;
                        int lHot = RedTeamAIMod.threatAtPoint(unit.x+lx*SIDE_BIAS, unit.y+ly*SIDE_BIAS);
                        int rHot = RedTeamAIMod.threatAtPoint(unit.x+rx*SIDE_BIAS, unit.y+ry*SIDE_BIAS);
                        float mdx = (lHot <= rHot) ? lx : rx;
                        float mdy = (lHot <= rHot) ? ly : ry;
                        moveTo(new Vec2(unit.x + mdx*SIDE_BIAS, unit.y + mdy*SIDE_BIAS), 0);
                        if (frameCount % 60 == 0)
                            Log.info("[RedTeamAI] " + unit.type.name + " 绕侧 threat=" + (int)totalThreat);
                    }
                } else {
                    // 低威胁：直冲卡射程（挤成一团一起冲）
                    float desired = range * 0.8f;
                    if (dist > desired + 12f) {
                        moveTo(new Vec2(tx, ty), desired);
                    }
                    if (frameCount % 60 == 0)
                        Log.info("[RedTeamAI] " + unit.type.name + " 直冲建筑 dist=" + (int)dist + " threat=" + (int)totalThreat);
                }
            }

            unit.lookAt(tx, ty);
            updateWeapons(); updateVisuals();
            updateMovement();

        } catch (Exception ex) {
            Log.err("[RedTeamAI] 异常: " + ex.getMessage());
        }
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
