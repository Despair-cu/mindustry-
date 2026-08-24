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

    private static final float RETREAT_DIST = 2f;       // 射程-2格后退
    private static final float RALLY_RADIUS = 80f;      // 集合点判定半径
    private static final float SIDE_BIAS = 90f;         // 绕侧偏移
    private static final float RETARGET = 30f;          // 目标点变化超30px才重新寻路

    // 稳定的移动目标点（避免每帧重算导致寻路抖动）
    private float moveX = Float.NaN, moveY = Float.NaN;
    private boolean arrived = false;

    private static boolean globalAttacking = false;
    private int frameCount = 0;

    @Override
    public void updateUnit() {
        try {
            if (unit == null || unit.dead()) return;
            frameCount++;

            // 刷新目标
            if (target == null || isTargetDead(target)) {
                target = findBestTarget();
                moveX = Float.NaN; moveY = Float.NaN; // 目标变了，重置移动点
            }
            updateTargeting();
            if (target == null) { super.updateUnit(); return; }

            float tx = target.x(), ty = target.y();
            float dist = unit.dst(target);
            float range = unit.range();
            float retreatAt = range - RETREAT_DIST * 8f;

            // ===== 敌进我退（稳定后退点，不每帧重算）=====
            if (dist < retreatAt) {
                float dx = unit.x - tx, dy = unit.y - ty;
                float len = (float)Math.sqrt(dx*dx + dy*dy);
                if (len > 0.01f) {
                    float bx = unit.x + (dx/len) * 60f;
                    float by = unit.y + (dy/len) * 60f;
                    // 后退点固定，直到脱离近距离
                    if (Float.isNaN(moveX) || dst2(moveX, moveY, bx, by) > RETARGET*RETARGET) {
                        moveX = bx; moveY = by; arrived = false;
                    }
                }
                unit.lookAt(tx, ty); updateWeapons(); updateVisuals();
                doMove();
                return;
            }

            Vec2 rally = rallyPoint();

            // ===== 集结阶段 =====
            if (!globalAttacking) {
                if (rally != null && unit.dst(rally) > RALLY_RADIUS) {
                    setMove(rally.x, rally.y);
                    if (frameCount % 60 == 0)
                        Log.info("[RedTeamAI][地] " + unit.type.name + " 集合 " + RedTeamAIMod.cruxGroundCount() + "/" + rushThreshold());
                    unit.lookAt(tx, ty); updateWeapons(); updateVisuals();
                    doMove();
                    return;
                }
                if (RedTeamAIMod.cruxGroundCount() >= rushThreshold()) {
                    globalAttacking = true;
                    Log.info("[RedTeamAI][地] >>> 全军进攻! 人数=" + RedTeamAIMod.cruxGroundCount());
                } else {
                    unit.moveAt(Vec2.ZERO);
                    if (frameCount % 60 == 0)
                        Log.info("[RedTeamAI][地] " + unit.type.name + " 等待集结");
                    unit.lookAt(tx, ty); updateWeapons(); updateVisuals();
                    return;
                }
            }

            // ===== 进攻阶段 =====
            boolean isTurret = (target instanceof Building) && (((Building)target).block instanceof Turret);

            if (isTurret || target instanceof Unit) {
                // 炮塔/单位：保持拉扯距离（稳定目标点）
                float desired = retreatAt * 0.85f;
                if (dist > desired + 12f) {
                    setMove(tx, ty);
                    if (frameCount % 60 == 0)
                        Log.info("[RedTeamAI][地] " + unit.type.name + " 接近目标 dist=" + (int)dist);
                } else if (dist < desired - 12f) {
                    float dx = unit.x - tx, dy = unit.y - ty;
                    float len = (float)Math.sqrt(dx*dx + dy*dy);
                    if (len > 0.01f) setMove(unit.x + (dx/len)*50f, unit.y + (dy/len)*50f);
                } else {
                    if (frameCount % 60 == 0)
                        Log.info("[RedTeamAI][地] " + unit.type.name + " 拉扯输出 dist=" + (int)dist);
                }
            } else {
                // 建筑（非炮塔）：直冲卡射程
                float desired = range * 0.8f;
                if (dist > desired + 12f) {
                    setMove(tx, ty);
                    if (frameCount % 60 == 0)
                        Log.info("[RedTeamAI][地] " + unit.type.name + " 直冲建筑 dist=" + (int)dist);
                } else {
                    arrived = true; // 在射程内，停
                    if (frameCount % 60 == 0)
                        Log.info("[RedTeamAI][地] " + unit.type.name + " 建筑在射程内，站桩");
                }
            }

            unit.lookAt(tx, ty);
            updateWeapons(); updateVisuals();
            doMove();

        }