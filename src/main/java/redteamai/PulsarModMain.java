    // ===== 黑洞：纯黑核心 + 小椭圆黄蓝吸积盘 + 上下狂暴射线 =====
    public static class BlackHoleUnitType extends UnitType {
        public BlackHoleUnitType(String name) {
            super(name);
            this.speed = 0f;
            this.health = 999999f;
            this.hitSize = 18f;
            this.drag = 0f;
            // 关键：把默认贴图置空，彻底干掉原版引擎贴图！
            this.region = Core.atlas.find("clear"); 
        }

        @Override
        public void draw(Unit unit) {
            float x = unit.x, y = unit.y, time = unit.time;

            // 1. 绘制双向狂暴射线（强制上下：90度 和 -90度）
            drawViolentJets(x, y, time);

            // 2. 绘制小椭圆黄蓝吸积盘
            drawEllipticalDisk(x, y, time);

            // 3. 绘制纯黑核心（提权到最顶层，遮挡一切）
            Draw.z(110f);
            Draw.color(Color.black);
            Fill.circle(x, y, 14f);
            
            // 极小黄点高光
            Draw.color(Color.yellow);
            Fill.circle(x, y, 2f);
        }

        // 覆写身体、护盾等绘制，防止引擎画原版底色
        @Override
        public void drawBody(Unit unit) {}
        @Override
        public void drawCell(Unit unit) {}
        @Override
        public void drawControl(Unit unit) {}
        @Override
        public void drawShadow(Unit unit) {}

        private void drawViolentJets(float x, float y, float time) {
            Draw.z(85f);
            Color jetColor = Color.valueOf("00b3ff");
            int particleCount = 320;
            float particleSpeed = 22f;
            float length = 45f;

            for (int sign : new int[]{1, -1}) {
                float angle = 90f * sign; // 强制锁死上下喷射
                float spacing = length / particleCount;
                float travel = time * particleSpeed;

                Mathf.rand.setSeed(sign == 1 ? 123 : 456);
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

        private void drawEllipticalDisk(float x, float y, float time) {
            Draw.z(95f);
            Color inner = Color.valueOf("fff200"); // 亮黄
            Color mid = Color.valueOf("ffae00");   // 橙黄
            Color outer = Color.valueOf("00b3ff");  // 蓝
            
            Mathf.rand.setSeed(777);
            for (int i = 0; i < 150; i++) {
                float t = Mathf.rand.random(0f, 1f);
                float angle = time * 15f * (1f + (1f - t) * 1.5f) + t * 360f * 2f;

                // 压扁椭圆
                float rx = 20f * (0.3f + t * 0.7f);
                float ry = 7f * (0.3f + t * 0.7f);

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
