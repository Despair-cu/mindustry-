package com.example.pulsarmod;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import mindustry.gen.Layer;

public class PulsarUnitType extends UnitType {

    // 颜色和脉冲参数
    public Color coreColor = Color.valueOf("ffd37f");  // 脉冲星核心色（金黄）
    public Color outerColor = Color.valueOf("ff9d00");  // 外发光（橙）
    public float pulseSpeed = 40f;   // 脉冲速度（帧）
    public float baseRadius = 12f;   // 基础半径

    public PulsarUnitType(String name) {
        super(name);

        // 基础属性
        health = 450;
        speed = 1.4f;
        rotateSpeed = 8f;
        hitSize = baseRadius * 2f;
        constructor = Unit::create; // v8 标准单位实体构造器

        // 禁用默认武器和贴图相关逻辑
        weapon = null;
        outlineColor = Color.valueOf("00000000"); // 透明轮廓
    }

    @Override
    public void draw(Unit unit) {
        // 不调用 super.draw()，完全用特效替代贴图渲染

        float x = unit.x;
        float y = unit.y;
        float time = unit.time;

        // 1. 计算脉冲缩放（正弦波动）
        float pulse = Mathf.sin(time, pulseSpeed, 1f);
        float radius = baseRadius + pulse * 3f;

        // 2. 绘制向外扩散的能量波（波纹）
        float waveProgress = (time % pulseSpeed) / pulseSpeed;
        float waveRadius = waveProgress * baseRadius * 3.5f;
        float waveAlpha = 1f - waveProgress;

        Draw.z(Layer.effect);
        Draw.color(coreColor, waveAlpha * 0.4f);
        Lines.stroke(2f + pulse);
        Lines.circle(x, y, waveRadius);

        // 3. 绘制外发光层
        Draw.z(Layer.flyingUnitLow);
        Draw.color(outerColor, 0.25f + pulse * 0.1f);
        Fill.circle(x, y, radius * 1.6f);

        // 4. 绘制核心
        Draw.color(coreColor);
        Fill.circle(x, y, radius * 0.7f);

        // 5. 绘制白色高光中心
        Draw.color(Color.white, 0.8f);
        Fill.circle(x, y, radius * 0.35f);

        // 6. 绘制环绕旋转的能量节点
        for (int i = 0; i < 3; i++) {
            float angle = time * (25f + i * 10f) + (i * 120f);
            float dist = radius * 0.5f;
            float px = x + Angles.trnsx(angle, dist);
            float py = y + Angles.trnsy(angle, dist);

            Draw.color(Color.white, coreColor, 0.5f + Mathf.sin(time + i * 60f, 10f, 0.5f));
            Fill.circle(px, py, 2.5f + pulse * 1.2f);
        }

        // 7. 随机星火粒子（使用unit.id作为随机种子保持稳定）
        Mathf.rand.setSeed(unit.id);
        for (int i = 0; i < 4; i++) {
            float sparkAngle = Mathf.rand.random(360f);
            float sparkDist = Mathf.rand.random(radius * 0.8f, radius * 2.2f);
            float sx = x + Angles.trnsx(sparkAngle, sparkDist);
            float sy = y + Angles.trnsy(sparkAngle, sparkDist);
            float alpha = Mathf.rand.random(0.2f, 0.5f);

            Draw.color(coreColor, alpha);
            Fill.circle(sx, sy, Mathf.rand.random(1f, 2.5f));
        }
        Mathf.rand.setSeed(0); // 重置种子

        // 8. 添加动态光照
        Drawf.light(x, y, radius * 3.5f, coreColor, 0.7f);

        Draw.reset();
    }
}
