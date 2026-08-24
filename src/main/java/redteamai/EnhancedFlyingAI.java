package redteamai;

import mindustry.ai.types.FlyingAI;
import mindustry.content.Blocks;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.world.blocks.defense.turrets.Turret;

public class EnhancedFlyingAI extends FlyingAI {

    private static final float DETECT_DIST = 180f;
    private static final float SAFE_DIST = 50f;

    private float evadeX = Float.NaN, evadeY = Float.NaN;
    private boolean evading = false;

    @Override
    public void updateUnit() {
        if (unit == null || unit.dead()) return;

        if (target == null || isTargetDead(target)) {
            target = findTarget();
            clearEvade();
        }
        updateTargeting();
        if (target == null) { super.updateUnit(); return; }

        float tx = target.x(), ty = target.y();
        float dx = tx - unit.x, dy = ty - unit.y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) return;

        // 收集前方所有威胁炮塔
        java.util.ArrayList<Building> threats = new java.util.ArrayList<>();
        for (Building b : Groups.build) {
            if (b == null || b.dead) continue;
            if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;
            if (!(b.block instanceof Turret)) continue;
            Turret t = (Turret) b.block;
            if (!t.targetAir) continue;
            if (b.power != null && b.power.status < 0.1f) continue;

            float tdx = b.x - unit.x, tdy = b.y - unit.y;
            float proj = (tdx * dx + tdy * dy) / len;
            if (proj < 0 || proj > DETECT_DIST) continue;

            float perpX = tdx - (proj * dx / len);
            float perpY = tdy - (proj * dy / len);
            float perpDist = (float) Math.sqrt(perpX * perpX + perpY * perpY);

            float safeRange = t.range + SAFE_DIST;
            if (perpDist < safeRange && unit.dst(b) < safeRange) {
                threats.add(b);
            }
        }

        if (threats.isEmpty()) {
            // 无威胁：直线飞
            clearEvade();
            float sp = unit.speed();
            unit.move((dx / len) * sp, (dy / len) * sp);
            unit.isShooting(true);
        } else {
            // 有威胁：计算绕行点
            if (!evading || Float.isNaN(evadeX) || Float.isNaN(evadeY)) {
                // 计算逃离方向：所有威胁炮塔的排斥向量之和
                float repelX = 0, repelY = 0;
                for (Building b : threats) {
                    float tdx = unit.x - b.x, tdy = unit.y - b.y;
                    float d = (float) Math.sqrt(tdx * tdx + tdy * tdy);
                    if (d > 0.01f) {
                        Turret t = (Turret) b.block;
                        float safeRange = t.range + SAFE_DIST + 40f;
                        float weight = safeRange / d; // 越近权重越大
                        repelX += (tdx / d) * weight;
                        repelY += (tdy / d) * weight;
                    }
                }
                float rlen = (float) Math.sqrt(repelX * repelX + repelY * repelY);
                if (rlen > 0.01f) {
                    evadeX = unit.x + (repelX / rlen) * 80f;
                    evadeY = unit.y + (repelY / rlen) * 80f;
                } else {
                    // 兜底：垂直方向
                    evadeX = unit.x - dy / len * 80f;
                    evadeY = unit.y + dx / len * 80f;
                }
                evading = true;
            }

            // 检查是否到达绕行点
            float edx = evadeX - unit.x, edy = evadeY - unit.y;
            float edist = (float) Math.sqrt(edx * edx + edy * edy);
            if (edist < 25f) {
                // 到了绕行点，检查是否还被威胁
                boolean stillThreatened = false;
                for (Building b : threats) {
                    if (unit.dst(b) < ((Turret) b.block).range + SAFE_DIST * 0.5f) {
                        stillThreatened = true;
                        break;
                    }
                }
                if (!stillThreatened) {
                    clearEvade();
                } else {
                    // 还在威胁内：重新计算绕行点
                    evadeX = Float.NaN; evadeY = Float.NaN;
                }
            }

            if (evading) {
                // 飞向绕行点，关武器
                float elen = (float) Math.sqrt(edx * edx + edy * edy);
                if (elen > 0.01f) {
                    float sp = unit.speed() * 0.85f;
                    unit.move((edx / elen) * sp, (edy / elen) * sp);
                }
                unit.isShooting(false);
            } else {
                // 脱离成功，直线飞
                float sp = unit.speed();
                unit.move((dx / len) * sp, (dy / len) * sp);
                unit.isShooting(true);
            }
        }

        unit.lookAt(tx, ty);
        updateWeapons();
        updateVisuals();
    }

    private void clearEvade() {
        evadeX = Float.NaN;
        evadeY = Float.NaN;
        evading = false;
    }

    private boolean isTargetDead(Teamc t) {
        if (t == null) return true;
        if (t instanceof Unit) return ((Unit) t).dead();
        if (t instanceof Building) return ((Building) t).dead;
        return true;
    }

    private Building findTarget() {
        for (Building b : Groups.build) {
            if (b == null || b.dead) continue;
            if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;
            if (b.block == Blocks.coreShard || b.block == Blocks.coreFoundation || b.block == Blocks.coreNucleus)
                return b;
        }
        for (Building b : Groups.build) {
            if (b == null || b.dead) continue;
            if (b.team == unit.team || b.team == mindustry.game.Team.derelict) continue;
            return b;
        }
        return null;
    }
}
