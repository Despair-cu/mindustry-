package retpackage redteamai; // 注意改成你自己的包名

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Angles;
import arc.math.Mathf;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.Unit;
import mindustry.type.UnitType;

public class CustomUnits {
    
    // ===== 黑洞 =====
    public static class BlackHoleUnitType extends UnitType {
        private int particleCount = 320;
        private float particleSpeed = 22f;
        private float length = 60f;
        private Color jetColor = Color.valueOf("00b3ff");
        
        private int diskParticles = 150;
        private float diskSpeed = 15f;
        private float diskRx = 20f;
        private float diskRy = 7f;

        public BlackHoleUnitType(String name) {
            super(name);
            this.region = Core.atlas.find("clear"); // 彻底干掉原版贴图
            this.drawBody = false;
            this.drawCell = false;
            this.drawControl = false;
            this.drawSoftShadow = false;
            this.flying = true;
            this.speed = 0f; 
            this.hitSize = 8f; // 碰撞体积缩小
        }

        @Override
        public void update(Unit unit) {
            super.update(unit);
            float time = unit.time();
            
            // 双向射线伤害
            for (int sign : new int[]{1, -1}) {
                float angle = 90f * sign; // 锁死上下喷
                float rad = Angles.toRad(angle);
                float ex = unit.x + Angles.trnsx(angle, length);
                float ey = unit.y + Angles.trnsy(angle, length);
                applyDamageAlongLine(unit, unit.x, unit.y, ex, ey, 4f, 200f);
            }
            
            // 狂暴引力吸入
            Units.nearbyEnemies(unit.team, unit.x, unit.y, 50f, u -> {
                if (u != unit && !u.dead) {
                    u.damage(3000000f * Time.delta);
                    // 简易吸入逻辑
                    float ang = Angles.angle(u.x, u.y, unit.x, unit.y);
                    u.impulse(Angles.trnsx(ang, 5f), Angles.trnsy(ang, 5f));
                }
            });
        }

        @Override
        public void draw(Unit unit) {
            super.draw(unit);
            float time = unit.time();
            
            Draw.z(85f);
            drawViolentJets(unit.x, unit.y, time);
            
            Draw.z(95f);
            drawEllipticalDisk(unit.x, unit.y, time);
            
            // 纯黑核心提权
            Draw.z(110f);
            Draw.color(Color.black);
            Fill.circle(unit.x, unit.y, 6f);
            Draw.color(Color.yellow); // 极小黄点高光
            Fill.circle(unit.x, unit.y, 1f);
        }

        private void drawViolentJets(float x, float y, float time) {
            float spacing = length / particleCount;
            float travel = time * particleSpeed;

            Mathf.rand.setSeed(0);
            for (int sign : new int[]{1, -1}) {
                float angle = 90f * sign; // 锁死上下喷
                
                for (int i = 0; i < particleCount; i++) {
                    float dist = (travel + i * spacing) % length;
                    float t = dist / length;

                    float spread = t * 3f;
                    float offset = Mathf.rand.random(-spread, spread);
                    float finalAngle = angle + offset;

                    float px = x + Angles.trnsx(finalAngle, dist);
                    float py = y + Angles.trnsy(finalAngle, dist);

                    Color c = (t < 0.2f) ? Color.white.lerp(jetColor, t / 0.2f) : jetColor;
                    float flicker = (Mathf.sin(dist * 0.2f - time * 0.5f) + 1f) / 2f;
                    float alpha = (1f - t * 0.8f) * (0.5f + flicker * 0.5f);

                    float size = (0.8f - t * 0.6f) * Mathf.rand.random(0.5f, 1.0f);
                    size = Math.max(size, 0.15f);

                    Draw.color(c, alpha);
                    Fill.circle(px, py, size);

                    if (Mathf.rand.chance(0.08f)) {
                        float sprayAngle = finalAngle + Mathf.rand.range(15f);
                        float sprayDist = dist + Mathf.rand.random(2f, 8f);
                        Draw.color(c, alpha * 0.4f);
                        Fill.circle(x + Angles.trnsx(sprayAngle, sprayDist), 
                                     y + Angles.trnsy(sprayAngle, sprayDist), size * 0.5f);
                    }
                }
            }
            Mathf.rand.setSeed(0);
        }

        // 小椭圆吸积盘：亮黄 → 橙黄 → 蓝
        private void drawEllipticalDisk(float x, float y, float time) {
            Color inner = Color.valueOf("fff200");
            Color mid = Color.valueOf("ffae00");
            Color outer = Color.valueOf("00b3ff");

            Mathf.rand.setSeed(777);
            for (int i = 0; i < diskParticles; i++) {
                float t = Mathf.rand.random(0f, 1f);
                float angle = time * diskSpeed * (1f + (1f - t) * 1.5f) + t * 360f * 2f;

                float rx = diskRx * (0.3f + t * 0.7f);
                float ry = diskRy * (0.3f + t * 0.7f);

                float px = x + Angles.trnsx(angle, rx);
                float py = y + Angles.trnsy(angle, ry);

                Color c = (t < 0.4f) ? inner.lerp(mid, t / 0.4f) : mid.lerp(outer, (t - 0.4f) / 0.6f);
                float flicker = (Mathf.sin(time * 8f + i * 0.9f) + 1f) / 2f;
                float alpha = (0.7f + flicker * 0.3f) * (1f - t * 0.6f);

                float size = (1.8f - t * 1.2f) + Mathf.sin(time * 6f + i) * 0.3f;
                size = Math.max(size, 0.3f);

                Draw.color(c, alpha);
                Fill.circle(px, py, size);
            }
            Mathf.rand.setSeed(0);
        }
    }

    // ===== 中子星 =====
    public static class NeutronStarUnitType extends UnitType {
        private int particleCount = 200;
        private float particleSpeed = 14f;
        private float length = 45f;
        private Color jetColor = Color.valueOf("00ffff");

        public NeutronStarUnitType(String name) {
            super(name);
            this.region = Core.atlas.find("clear"); // 彻底干掉原版贴图和呼吸膨胀
            this.drawBody = false;
            this.drawCell = false;
            this.drawControl = false;
            this.drawSoftShadow = false; // 关掉软阴影
            this.flying = true;
            this.speed = 0f;
            this.hitSize = 6f; // 体积大幅缩小
            this.rotateSpeed = 0f; // 锁死旋转，防止射线歪
        }

        @Override
        public void update(Unit unit) {
            super.update(unit);
            float time = unit.time();
            
            for (int sign : new int[]{1, -1}) {
                float angle = 90f * sign; // 上下喷
                float ex = unit.x + Angles.trnsx(angle, length);
                float ey = unit.y + Angles.trnsy(angle, length);
                applyDamageAlongLine(unit, unit.x, unit.y, ex, ey, 3f, 80f);
            }
        }

        @Override
        public void draw(Unit unit) {
            super.draw(unit);
            float time = unit.time();
            
            // 画粒子射线
            Draw.z(85f);
            float spacing = length / particleCount;
            float travel = time * particleSpeed;
            Mathf.rand.setSeed(0);
            
            for (int sign : new int[]{1, -1}) {
                float angle = 90f * sign;
                for (int i = 0; i < particleCount; i++) {
                    float dist = (travel + i * spacing) % length;
                    float t = dist / length;
                    float spread = t * 2f;
                    float offset = Mathf.rand.random(-spread, spread);
                    float finalAngle = angle + offset;
                    
                    float px = unit.x + Angles.trnsx(finalAngle, dist);
                    float py = unit.y + Angles.trnsy(finalAngle, dist);
                    
                    Color c = (t < 0.2f) ? Color.white.lerp(jetColor, t / 0.2f) : jetColor;
                    float flicker = (Mathf.sin(dist * 0.2f - time * 0.5f) + 1f) / 2f;
                    float alpha = (1f - t * 0.8f) * (0.5f + flicker * 0.5f);
                    
                    float size = (0.6f - t * 0.4f) * Mathf.rand.random(0.5f, 1.0f);
                    size = Math.max(size, 0.1f);
                    
                    Draw.color(c, alpha);
                    Fill.circle(px, py, size);
                }
            }
            Mathf.rand.setSeed(0);
            
            // 画实心核心（没有洞）
            Draw.z(100f);
            Draw.color(Color.valueOf("00ffff"));
            Fill.circle(unit.x, unit.y, 4f);
            Draw.color(Color.white);
            Fill.circle(unit.x, unit.y, 1.5f);
        }
    }

    // ===== 共用伤害辅助 =====
    private static int applyDamageAlongLine(Unit source, float x1, float y1, float x2, float y2, float width, float damage) {
        int hit = 0;
        Team sourceTeam = source.team;
        for (Unit u : Groups.unit) {
            if (u == null || u.dead) continue;
            if (u.team == sourceTeam) continue;
            if (distanceToSegment(u.x, u.y, x1, y1, x2, y2) <= width + u.hitSize) {
                u.damage(damage);
                hit++;
            }
        }
        return hit;
    }

    private static float distanceToSegment(float px, float py, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        float len2 = dx * dx + dy * dy;
        if (len2 < 0.0001f) return Mathf.dst(px, py, x1, y1);
        float t = ((px - x1) * dx + (py - y1) * dy) / len2;
        t = Mathf.clamp(t, 0f, 1f);
        float cx = x1 + t * dx, cy = y1 + t * dy;
        return Mathf.dst(px, py, cx, cy);
    }
}
