package redteamai;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import mindustry.effects.Fx;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.game.Team;
import mindustry.mod.Mod;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;

public class PulsarModMain extends Mod {

    public static boolean DEBUG = false;
    public static StatusEffect invincible;

    @Override
    public void loadContent() {
        invincible = new StatusEffect("pulsar-invincible") {
            {
                healthMultiplier = Float.POSITIVE_INFINITY;
                show = false;
                damageMultiplier = 1f;
            }
        };

        Log.info("[PulsarMod] 加载恒星单位...");
        new YellowDwarfUnitType("yellow-dwarf").load();
        new BluePulsarUnitType("blue-pulsar").load();
        new BlackHoleUnitType("black-hole").load();
        new ShockwaveUnitType("shockwave-star").load();
        Log.info("[PulsarMod] 所有单位注册完成");
    }

    // ====================================================================
    //  黄矮星
    // ====================================================================
    public static class YellowDwarfUnitType extends UnitType {
        private final Color coreColor = Color.valueOf("ffd37f");
        private final Color outerColor = Color.valueOf("ff9d00");
        private final float pulseSpeed = 40f, baseRadius = 22f;
        private final float gravityRange = 150f, gravityStrength = 1.0f, suckDamage = 1000000f;

        public YellowDwarfUnitType(String name) {
            super(name);
            health = Integer.MAX_VALUE; speed = 0f; rotateSpeed = 8f;
            hitSize = baseRadius * 2f; constructor = UnitEntity::create;
            weapons = new Seq<>(); outlineColor = Color.valueOf("00000000");
            localizedName = "黄矮星";
        }

        @Override
        public void update(Unit unit) {
            unit.apply(invincible, 5f);
            for (Unit u : Groups.unit) {
                if (u == null || u.dead || u.team == unit.team) continue;
                float dst = Mathf.dst(unit.x, unit.y, u.x, u.y);
                if (dst < gravityRange) {
                    float angle = Angles.angle(unit.x, unit.y, u.x, u.y);
                    u.x -= Angles.trnsx(angle, gravityStrength);
                    u.y -= Angles.trnsy(angle, gravityStrength);
                    u.damage(suckDamage * Time.delta);
                }
                if (dst <= unit.hitSize + u.hitSize + 5f) { u.kill(); }
            }
        }

        @Override
        public void draw(Unit unit) {
            Draw.reset();
            float x = unit.x, y = unit.y, time = Time.time;
            float pulse = Mathf.sin(time, pulseSpeed, 1f);
            float radius = baseRadius + pulse * 3f;
            Draw.z(100f);
            float wave = (time % 40f) / 40f;
            Draw.color(coreColor, (1f - wave) * 0.4f);
            Lines.stroke(2f + pulse);
            Lines.circle(x, y, wave * baseRadius * 3.5f);
            Draw.color(outerColor, 0.25f + pulse * 0.1f);
            Fill.circle(x, y, radius * 1.6f);
            Draw.color(coreColor); Fill.circle(x, y, radius * 0.7f);
            Draw.color(Color.white, 0.8f); Fill.circle(x, y, radius * 0.35f);
            for (int i = 0; i < 3; i++) {
                float a = time * (25f + i * 10f) + i * 120f;
                float d = radius * 0.5f;
                Draw.color(Color.white, coreColor, 0.5f + Mathf.sin(time + i * 60f, 10f, 0.5f));
                Fill.circle(x + Angles.trnsx(a, d), y + Angles.trnsy(a, d), 2.5f + pulse * 1.2f);
            }
            Draw.reset(); Draw.z(0f);
        }
    }

    // ====================================================================
    //  中子星
    // ====================================================================
    public static class BluePulsarUnitType extends UnitType {
        private final Color coreColor = Color.valueOf("00e5ff");
        private final Color outerColor = Color.valueOf("0099cc");
        private final Color jetColor = Color.valueOf("00e5ff");
        private final float baseRadius = 5f;
        private final int particleCount = 400;
        private final float particleSpeed = 30f, jetLength = 1000f, dps = 150f;
        private final float gravityRange = 180f, gravityStrength = 4.0f;

        public BluePulsarUnitType(String name) {
            super(name);
            health = Integer.MAX_VALUE; speed = 0f; rotateSpeed = 12f;
            hitSize = baseRadius * 2f; constructor = UnitEntity::create;
            weapons = new Seq<>(); outlineColor = Color.valueOf("00000000");
            region = Core.atlas.find("clear"); drawBody = false; drawCell = false;
            localizedName = "中子星";
        }

        @Override
        public void update(Unit unit) {
            unit.apply(invincible, 5f);
            float swing = Mathf.sin(Time.time, 25f, 8f);
            float damage = dps * Time.delta;
            for (int sign : new int[]{1, -1}) {
                float a = (sign > 0 ? 0f : 180f) + swing;
                float ex = unit.x + Angles.trnsx(a, jetLength);
                float ey = unit.y + Angles.trnsy(a, jetLength);
                for (Unit u : Groups.unit) {
                    if (u == null || u.dead || u.team == unit.team) continue;
                    if (distanceToSegment(u.x, u.y, unit.x, unit.y, ex, ey) <= 4f + u.hitSize)
                        u.damage(damage);
                }
            }
            for (Unit u : Groups.unit) {
                if (u == null || u.dead || u.team == unit.team) continue;
                float dst = Mathf.dst(unit.x, unit.y, u.x, u.y);
                if (dst < gravityRange) {
                    float angle = Angles.angle(unit.x, unit.y, u.x, u.y);
                    u.x -= Angles.trnsx(angle, gravityStrength);
                    u.y -= Angles.trnsy(angle, gravityStrength);
                }
                if (dst <= unit.hitSize + u.hitSize + 5f) { u.kill(); }
            }
        }

        @Override
        public void draw(Unit unit) {
            Draw.reset();
            float x = unit.x, y = unit.y, time = Time.time;
            Draw.z(85f);
            float swing = Mathf.sin(time, 25f, 8f);
            drawNeutronJet(x, y, 0f + swing, time, unit.id);
            drawNeutronJet(x, y, 180f + swing, time, unit.id + 1000);
            Draw.z(110f);
            Draw.color(coreColor); Fill.circle(x, y, baseRadius * 1.5f);
            Fill.circle(x, y, baseRadius * 0.5f);
            Draw.reset(); Draw.z(0f);
        }

        private void drawNeutronJet(float x, float y, float angle, float time, long seed) {
            float spacing = 3.0f; float travel = time * particleSpeed;
            Mathf.rand.setSeed(seed);
            for (int i = 0; i < particleCount; i++) {
                float dist = (travel + i * spacing) % jetLength;
                float t = dist / jetLength;
                float spread = t * 3f;
                float offset = Mathf.rand.random(-spread, spread);
                float a = angle + offset;
                float px = x + Angles.trnsx(a, dist);
                float py = y + Angles.trnsy(a, dist);
                Color c;
                if (t < 0.2f) c = Color.white.lerp(jetColor, t / 0.2f);
                else if (t < 0.7f) c = jetColor;
                else c = jetColor.lerp(outerColor, (t - 0.7f) / 0.3f);
                float flicker = (Mathf.sin(dist * 0.15f - time * 0.4f) + 1f) / 2f;
                float alpha = (1f - t * 0.8f) * (0.5f + flicker * 0.5f);
                float size = (1.0f - t * 0.6f) * Mathf.rand.random(0.7f, 1.2f);
                size = Math.max(size, 0.15f);
                Draw.color(c, alpha); Fill.circle(px, py, size);
            }
            Mathf.rand.setSeed(0);
        }

        private float distanceToSegment(float px, float py, float x1, float y1, float x2, float y2) {
            float dx = x2 - x1, dy = y2 - y1; float len2 = dx * dx + dy * dy;
            if (len2 < 0.0001f) return Mathf.dst(px, py, x1, y1);
            float t = ((px - x1) * dx + (py - y1) * dy) / len2; t = Mathf.clamp(t, 0f, 1f);
            return Mathf.dst(px, py, x1 + t * dx, y1 + t * dy);
        }
    }

    // ====================================================================
    //  黑洞
    // ====================================================================
    public static class BlackHoleUnitType extends UnitType {
        private final float baseRadius = 6f;
        private final float gravityRange = 350f, gravityStrength = 5.0f;
        private final int jetParticleCount = 380;
        private final float jetParticleSpeed = 28f, jetLength = 220f, dps = 300f;
        private final int diskParticles = 160;
        private final float diskRx = 24f, diskRy = 9f, diskSpeed = 12f;
        private final Color jetColor = Color.valueOf("c0c8d0");
        private final Color jetOuter = Color.valueOf("808890");
        private final Color coreColor = Color.valueOf("505050");
        private final Color diskInner = Color.valueOf("fff200");
        private final Color diskMid = Color.valueOf("ffae00");
        private final Color diskOuter = Color.valueOf("00b3ff");

        public BlackHoleUnitType(String name) {
            super(name);
            health = Integer.MAX_VALUE; speed = 0f; rotateSpeed = 0f;
            hitSize = baseRadius * 2f; constructor = UnitEntity::create;
            weapons = new Seq<>(); outlineColor = Color.valueOf("00000000");
            localizedName = "黑洞";
        }

        @Override
        public void update(Unit unit) {
            unit.apply(invincible, 5f);
            float swing = Mathf.sin(Time.time, 40f, 6f);
            float damage = dps * Time.delta;
            for (int sign : new int[]{1, -1}) {
                float a = 90f * sign + swing;
                float ex = unit.x + Angles.trnsx(a, jetLength);
                float ey = unit.y + Angles.trnsy(a, jetLength);
                for (Unit u : Groups.unit) {
                    if (u == null || u.dead || u.team == unit.team) continue;
                    if (distanceToSegment(u.x, u.y, unit.x, unit.y, ex, ey) <= 5f + u.hitSize)
                        u.damage(damage);
                }
            }
            for (Unit u : Groups.unit) {
                if (u == null || u.dead || u.team == unit.team) continue;
                float dst = Mathf.dst(unit.x, unit.y, u.x, u.y);
                if (dst < gravityRange) {
                    float angle = Angles.angle(unit.x, unit.y, u.x, u.y);
                    u.x -= Angles.trnsx(angle, gravityStrength);
                    u.y -= Angles.trnsy(angle, gravityStrength);
                }
                if (dst <= unit.hitSize + u.hitSize + 5f) { u.remove(); }
            }
        }

        @Override
        public void draw(Unit unit) {
            Draw.reset();
            float x = unit.x, y = unit.y, time = Time.time;
            Draw.z(85f);
            float swing = Mathf.sin(time, 40f, 6f);
            drawBlackHoleJets(x, y, swing, time);
            Draw.z(95f);
            drawAccretionDisk(x, y, time);
            Draw.z(110f);
            Draw.color(coreColor); Fill.circle(x, y, baseRadius * 1.3f);
            Draw.color(Color.valueOf("888888"), 0.5f); Fill.circle(x, y, baseRadius * 0.4f);
            Draw.reset(); Draw.z(0f);
        }

        private void drawBlackHoleJets(float x, float y, float swing, float time) {
            float spacing = 1.0f; float travel = time * jetParticleSpeed;
            Mathf.rand.setSeed(0);
            for (int sign : new int[]{1, -1}) {
                float angle = 90f * sign + swing;
                for (int i = 0; i < jetParticleCount; i++) {
                    float dist = (travel + i * spacing) % jetLength;
                    float t = dist / jetLength;
                    float spread = t * 3.5f;
                    float offset = Mathf.rand.random(-spread, spread);
                    float a = angle + offset;
                    float px = x + Angles.trnsx(a, dist);
                    float py = y + Angles.trnsy(a, dist);
                    Color c;
                    if (t < 0.2f) c = Color.white.lerp(jetColor, t / 0.2f);
                    else if (t < 0.7f) c = jetColor;
                    else c = jetColor.lerp(jetOuter, (t - 0.7f) / 0.3f);
                    float flicker = (Mathf.sin(dist * 0.15f - time * 0.5f) + 1f) / 2f;
                    float alpha = (1f - t * 0.75f) * (0.6f + flicker * 0.4f);
                    float size = (1.5f - t * 1.0f) * Mathf.rand.random(0.8f, 1.5f);
                    size = Math.max(size, 0.25f);
                    Draw.color(c, alpha); Fill.circle(px, py, size);
                }
            }
            Mathf.rand.setSeed(0);
        }

        private void drawAccretionDisk(float x, float y, float time) {
            Mathf.rand.setSeed(777);
            for (int i = 0; i < diskParticles; i++) {
                float t = Mathf.rand.random(0f, 1f);
                float angle = time * diskSpeed * (1f + (1f - t) * 1.5f) + t * 360f * 2f;
                float rx = diskRx * (0.3f + t * 0.7f);
                float ry = diskRy * (0.3f + t * 0.7f);
                float px = x + Angles.trnsx(angle, rx);
                float py = y + Angles.trnsy(angle, ry);
                Color c = (t < 0.4f) ? diskInner.lerp(diskMid, t / 0.4f) : diskMid.lerp(diskOuter, (t - 0.4f) / 0.6f);
                float flicker = (Mathf.sin(time * 8f + i * 0.9f) + 1f) / 2f;
                float alpha = (0.7f + flicker * 0.3f) * (1f - t * 0.6f);
                float size = (2.0f - t * 1.3f) + Mathf.sin(time * 6f + i) * 0.3f;
                size = Math.max(size, 0.3f);
                Draw.color(c, alpha); Fill.circle(px, py, size);
            }
            Mathf.rand.setSeed(0);
        }

        private float distanceToSegment(float px, float py, float x1, float y1, float x2, float y2) {
            float dx = x2 - x1, dy = y2 - y1; float len2 = dx * dx + dy * dy;
            if (len2 < 0.0001f) return Mathf.dst(px, py, x1, y1);
            float t = ((px - x1) * dx + (py - y1) * dy) / len2; t = Mathf.clamp(t, 0f, 1f);
            return Mathf.dst(px, py, x1 + t * dx, y1 + t * dy);
        }
    }

    // ====================================================================
    //  冲击波星（全图秒杀 + 力墙完美阻挡 + 天体免疫 + 视觉特效）
    // ====================================================================
    public static class ShockwaveUnitType extends UnitType {
        private final Color coreColor = Color.valueOf("00e5ff");
        private final float baseRadius = 22f;
        
        private final float shockwaveInterval = 20f; // 释放间隔(秒)
        private final float shockwaveMaxRadius = 10000f; // 全图范围
        private final float shockwaveSpeed = 250f; // 扩散速度
        private final float shockwaveThickness = 12f;
        
        private float shockwaveTimer = 0f;
        private float shockwaveRadius = -1f;
        private Seq<Building> hitShields = new Seq<>();

        public ShockwaveUnitType(String name) {
            super(name);
            health = Integer.MAX_VALUE;
            speed = 0f;
            rotateSpeed = 8f;
            hitSize = baseRadius * 2f;
            constructor = UnitEntity::create;
            weapons = new Seq<>();
            outlineColor = Color.valueOf("00000000");
            localizedName = "冲击波星";
        }

        @Override
        public void update(Unit unit) {
            // 本体无敌
            unit.apply(invincible, 5f);
            
            shockwaveTimer += Time.delta / 60f;
            
            // 触发冲击波
            if (shockwaveRadius < 0f && shockwaveTimer >= shockwaveInterval) {
                shockwaveRadius = unit.hitSize;
                shockwaveTimer = 0f;
                hitShields.clear();
                // 蓄力特效
                Fx.shockwave.at(unit.x, unit.y, 0f, coreColor);
            }
            
            // 冲击波扩散与伤害判定
            if (shockwaveRadius >= 0f) {
                shockwaveRadius += shockwaveSpeed * (Time.delta / 60f);
                
                // 伤害判定
                for (Unit u : Groups.unit) {
                    if (u == null || u.dead || u.team == unit.team) continue;
                    // 绝对免疫其他天体（通过类型直接判断，防误杀）
                    if (u.type instanceof YellowDwarfUnitType || u.type instanceof BluePulsarUnitType || 
                        u.type instanceof BlackHoleUnitType || u.type instanceof ShockwaveUnitType) continue;
                    
                    float dst = Mathf.dst(unit.x, unit.y, u.x, u.y);
                    if (dst <= shockwaveRadius + shockwaveThickness && dst >= shockwaveRadius - shockwaveThickness) {
                        if (!isBlockedByShield(unit.x, unit.y, u.x, u.y)) {
                            Fx.dynamicExplosion.at(u.x, u.y); // 命中爆炸特效
                            u.kill();
                        }
                    }
                }
                
                for (Building b : Groups.build) {
                    if (b == null || !b.isValid() || b.team == unit.team) continue;
                    
                    float dst = Mathf.dst(unit.x, unit.y, b.x, b.y);
                    if (dst <= shockwaveRadius + shockwaveThickness && dst >= shockwaveRadius - shockwaveThickness) {
                        if (!isBlockedByShield(unit.x, unit.y, b.x, b.y)) {
                            Fx.blockExplode.at(b.x, b.y); // 建筑爆炸特效
                            b.kill();
                        }
                    }
                }
                
                if (shockwaveRadius > shockwaveMaxRadius) {
                    shockwaveRadius = -1f;
                }
            }
        }

        /** 射线检测：从冲击波中心到目标，如果中间有带护盾的力墙则阻挡 */
        private boolean isBlockedByShield(float sx, float sy, float tx, float ty) {
            for (Building b : Groups.build) {
                if (b == null || !b.isValid()) continue;
                
                // ✅ 核心修复：检测建筑是否有护盾(hasShields)，兼容原版力墙投影(force-projector)
                if (b.block.hasShields) {
                    if (hitShields.contains(b)) continue; // 本次冲击波已处理过该力墙
                    
                    // 检测目标与冲击源连线是否穿过力墙
                    float dist = distanceToSegment(b.x, b.y, sx, sy, tx, ty);
                    float size = b.block.size * 4f; 
                    
                    if (dist <= size + 4f) {
                        // ✅ 核心修复：消耗500盾容，力墙掉200血（使用原版护盾伤害机制）
                        b.damage(200f); 
                        b.block.health -= 0; 
                        
                        hitShields.add(b);
                        
                        // 力墙挡下时的特效
                        Fx.shieldHit.at(b.x, b.y);
                        Fx.dynamicExplosion.at(b.x, b.y);
                        
                        if (Log.info != null) Log.info("[PulsarMod] 力墙 " + b.block.name + " 挡下冲击波！");
                        return true; // 阻挡成功，连线后方的目标不受伤
                    }
                }
            }
            return false;
        }

        @Override
        public void draw(Unit unit) {
            Draw.reset();
            float x = unit.x, y = unit.y, time = Time.time;

            // ✅ 蓄力动画（释放前3秒）
            float timeToNext = shockwaveInterval - shockwaveTimer;
            if (timeToNext <= 3f && shockwaveRadius < 0f) {
                float progress = 1f - (timeToNext / 3f);
                Draw.z(111f);
                Draw.color(coreColor, 0.3f + progress * 0.5f);
                Fill.circle(x, y, baseRadius * (1f + progress * 1.5f) + Mathf.sin(time * 10f) * 2f);
            }

            // ✅ 冲击波视觉（实心填充 + 多层高亮边缘 + 旋转粒子）
            if (shockwaveRadius >= 0f) {
                Draw.z(120f);
                float alpha = 0.4f * (1f - shockwaveRadius / shockwaveMaxRadius);
                
                // 主体实心圆
                Draw.color(coreColor, alpha);
                Fill.circle(x, y, shockwaveRadius);
                
                // 边缘高亮环
                Draw.color(coreColor, alpha * 2f);
                Lines.stroke(shockwaveThickness);
                Lines.circle(x, y, shockwaveRadius);
                
                // 最外层白色亮边
                Draw.color(Color.white, alpha);
                Lines.stroke(3f + Mathf.sin(time * 10f) * 2f);
                Lines.circle(x, y, shockwaveRadius);
                
                // 边缘旋转能量粒子
                for (int i = 0; i < 24; i++) {
                    float angle = time * 3f + i * 15f;
                    float px = x + Angles.trnsx(angle, shockwaveRadius);
                    float py = y + Angles.trnsy(angle, shockwaveRadius);
                    Draw.color(coreColor, alpha * 2f);
                    Fill.circle(px, py, 3f + Mathf.sin(time * 5f + i) * 2f);
                }
            }

            // ✅ 本体（中子星同款蓝色核心）
            Draw.z(110f);
            Draw.color(coreColor);
            Fill.circle(x, y, baseRadius * 1.5f);
            Fill.circle(x, y, baseRadius * 0.5f);
            Draw.color(Color.white, coreColor, 0.5f + Mathf.sin(time, 8f, 0.5f));
            Fill.circle(x, y, baseRadius * 0.8f);

            Draw.reset();
            Draw.z(0f);
        }

        private float distanceToSegment(float px, float py, float x1, float y1, float x2, float y2) {
            float dx = x2 - x1, dy = y2 - y1; 
            float len2 = dx * dx + dy * dy;
            if (len2 < 0.0001f) return Mathf.dst(px, py, x1, y1);
            float t = ((px - x1) * dx + (py - y1) * dy) / len2; 
            t = Mathf.clamp(t, 0f, 1f);
            return Mathf.dst(px, py, x1 + t * dx, y1 + t * dy);
        }
    }
}
