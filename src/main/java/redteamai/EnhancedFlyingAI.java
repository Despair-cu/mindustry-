package redteamai;

import mindustry.ai.types.FlyingAI;
import mindustry.content.Blocks;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.world.blocks.defense.turrets.Turret;
import java.util.ArrayList;
import java.util.List;

public class EnhancedFlyingAI extends FlyingAI {
    private static final float DETECT_DIST = 180f;
    private static final float SAFE_DIST = 40f;
    private static final float EVADE_DIST = 120f;

    private float evadeX = Float.NaN;
    private float evadeY = Float.NaN;
    private boolean evading = false;

    @Override
    public void updateUnit() {
        if (unit == null || unit.dead()) return;

        if (target == null || isTargetDead(target)) target = findTarget();
        updateTargeting();
        
        if (target == null) { 
            super.updateUnit(); 
            return; 
        }

        float tx = target.x(), ty = target.y();
        float dx = tx - unit.x, dy = ty - unit.y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) return;

        List<Building> threats = new ArrayList<>();
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

            if (perpDist < t.range + SAFE_DIST && unit.dst(b) < t.range + SAFE_DIST) {
                threats.add(b);
            }
        }

        if (!threats.isEmpty()) {
            if (Float.isNaN(evadeX)) {
                float repelX = 0, repelY = 0;
                for (Building b : threats) {
                    float tdx = unit.x - b.x, tdy = unit.y - b.y;
                    float d = (float) Math.sqrt(tdx * tdx + tdy * tdy);
                    if (d > 0.01f) {
                        Turret t = (Turret) b.block;
                        float safeRange = t.range + SAFE_DIST;
                        // 加入位置偏置打破双炮塔对称抵消
                        float weight = safeRange / Math.max(d, 1f) + (unit.id % 10f);
                        repelX += (tdx / d) * weight;
                        repelY += (tdy / d) * weight;
                    }
                }
                
                float rlen = (float) Math.sqrt(repelX * repelX + repelY * repelY);
                float nx, ny;
                if (rlen > 0.01f) {
                    nx = repelX / rlen;
                    ny = repelY / rlen;
                } else {
                    nx = -dy / len;
                    ny = dx / len;
                }
                
                // 结合目标方向，往侧前方脱离
                float forwardX = dx / len;
                float forwardY = dy / len;
                float finalDirX = nx * 0.7f + forwardX * 0.3f;
                float finalDirY = ny * 0.7f + forwardY * 0.3f;
                
                float flen = (float) Math.sqrt(finalDirX * finalDirX + finalDirY * finalDirY);
                if (flen > 0.01f) {
                    evadeX = unit.x + (finalDirX / flen) * EVADE_DIST;
                    evadeY = unit.y + (finalDirY / flen) * EVADE_DIST;
                }
                evading = true;
            }

            float edx = evadeX - unit.x, edy = evadeY - unit.y;
            float edist = (float) Math.sqrt(edx * edx + edy * edy);

            // 增大防抖阈值，避免反复重算
            if (edist < 40f) {
                boolean stillThreatened = false;
                for (Building b : threats) {
                    if (unit.dst(b) < ((Turret) b.block).range + SAFE_DIST * 0.5f) {
                        stillThreatened = true;
                        break;
                    }
                }
                if (stillThreatened) {
                    evadeX = Float.NaN; 
                    evadeY = Float.NaN;
                } else {
                    clearEvade();
                }
            }

            if (evading) {
                float elen = (float) Math.sqrt(edx * edx + edy * edy);
                if (elen > 0.01f) {
                    float sp = unit.speed() * 0.9f;
                    unit.move((edx / elen) * sp, (edy / elen) * sp);
                }
                unit.lookAt(evadeX, evadeY);
                unit.isShooting(false);
            } else {
                if (len > 0.01f) {
                    float sp = unit.speed();
                    unit.move((dx / len) * sp, (dy / len) * sp);
                }
                unit.lookAt(tx, ty);
                unit.isShooting(true);
            }
        } else {
            clearEvade();
            if (len > 0.01f) {
                float sp = unit.speed();
                unit.move((dx / len) * sp, (dy / len) * sp);
            }
            unit.lookAt(tx, ty);
            unit.isShooting(true);
        }

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
