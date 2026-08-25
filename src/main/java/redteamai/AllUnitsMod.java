package redteamai;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import mindustry.entities.abilities.ForceFieldAbility;
import mindustry.entities.abilities.RepairFieldAbility;
import mindustry.entities.abilities.ShieldRegenFieldAbility;
import mindustry.entities.abilities.SpawnerAbility;
import mindustry.entities.bullet.Bullets;
import mindustry.entities.bullet.MissileBulletType;
import mindustry.entities.bullet.RailBulletType;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.mod.Mod;

import static mindustry.Vars.content;

/**
 * 说明：
 * 不同 Arc 版本的 Pixmap 绘制 API 签名不一致（fillRect/fillRectangle、drawPixel 等），
 * 本文件完全不直接调用 Pixmap 的绘制方法，而是用一个 int[] 像素缓冲区（Raster）自己绘制，
 * 最后用稳定存在的 new Pixmap(w, h) + Pixmap.setRaw(x, y, rgba8888) 逐像素写入来生成贴图。
 * 已验证在 Anuken/Arc 官方源码（Pixmaps.noise/scale）中使用相同 API。
 */
public class AllUnitsMod extends Mod {

    public static UnitType greenGroundUnit, blueFlyUnit, redFactoryUnit;
    public static UnitType purpleNavalUnit, yellowMinerUnit;
    public static UnitType cyanSupportUnit, graySiegeUnit;
    public static UnitType whiteAntiAirUnit, pinkEWarUnit;
    public static UnitType orangeSuicideUnit, blueSubmarineUnit;
    public static UnitType brownTransportUnit, blackReaperUnit;
    public static UnitType redSniperUnit, greenHiveUnit;
    public static UnitType goldPaladinUnit, silverRailgunUnit;
    public static UnitType crimsonDemonUnit, cobaltShielderUnit;
    public static UnitType rainbowTitanUnit, emeraldCommanderUnit;

    private static final int S = 32; // 贴图尺寸

    // ==================== 像素缓冲区 ====================

    /** 一个 32x32 的 RGBA8888 像素缓冲区 */
    static class Raster {
        final int[] px = new int[S * S];
        final int w = S, h = S;

        void fill(int rgba) {
            for (int i = 0; i < px.length; i++) px[i] = rgba;
        }

        void set(int x, int y, int rgba) {
            if (x < 0 || y < 0 || x >= w || y >= h) return;
            px[y * w + x] = rgba;
        }

        int get(int x, int y) {
            if (x < 0 || y < 0 || x >= w || y >= h) return 0;
            return px[y * w + x];
        }

        /** 用当前颜色画一个像素（带裁剪） */
        void dot(int x, int y, int rgba) {
            set(x, y, rgba);
        }

        /** 用 scanline 填充三角形 */
        void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int col) {
            int minX = Math.max(0, Math.min(x1, Math.min(x2, x3)));
            int maxX = Math.min(w - 1, Math.max(x1, Math.max(x2, x3)));
            int minY = Math.max(0, Math.min(y1, Math.min(y2, y3)));
            int maxY = Math.min(h - 1, Math.max(y1, Math.max(y2, y3)));
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    if (pointInTri(x, y, x1, y1, x2, y2, x3, y3)) dot(x, y, col);
                }
            }
        }

        void fillRect(int x, int y, int width, int height, int col) {
            for (int yy = y; yy < y + height; yy++)
                for (int xx = x; xx < x + width; xx++)
                    set(xx, yy, col);
        }

        void drawRect(int x, int y, int width, int height, int col) {
            for (int xx = x; xx < x + width; xx++) { set(xx, y, col); set(xx, y + height - 1, col); }
            for (int yy = y; yy < y + height; yy++) { set(x, yy, col); set(x + width - 1, yy, col); }
        }

        void fillCircle(int cx, int cy, int r, int col) {
            for (int y = Math.max(0, cy - r); y <= Math.min(h - 1, cy + r); y++) {
                for (int x = Math.max(0, cx - r); x <= Math.min(w - 1, cx + r); x++) {
                    int dx = x - cx, dy = y - cy;
                    if (dx * dx + dy * dy <= r * r) set(x, y, col);
                }
            }
        }

        void drawCircle(int cx, int cy, int r, int col) {
            int r2 = r * r;
            for (int y = Math.max(0, cy - r - 1); y <= Math.min(h - 1, cy + r + 1); y++) {
                for (int x = Math.max(0, cx - r - 1); x <= Math.min(w - 1, cx + r + 1); x++) {
                    int dx = x - cx, dy = y - cy;
                    int d = dx * dx + dy * dy;
                    if (d >= r2 - r && d <= r2 + r) set(x, y, col);
                }
            }
        }

        void fillEllipse(int x, int y, int width, int height, int col) {
            int xc = x + width / 2, yc = y + height / 2;
            int a = width / 2, b = height / 2;
            if (a <= 0 || b <= 0) return;
            long a2 = (long) a * a, b2 = (long) b * b;
            for (int yy = Math.max(0, y); yy <= Math.min(h - 1, y + height); yy++) {
                for (int xx = Math.max(0, x); xx <= Math.min(w - 1, x + width); xx++) {
                    long dx = xx - xc, dy = yy - yc;
                    if (dx * dx * b2 + dy * dy * a2 <= a2 * b2) set(xx, yy, col);
                }
            }
        }

        private boolean pointInTri(int px, int py, int x1, int y1, int x2, int y2, int x3, int y3) {
            int d1 = sign(px, py, x1, y1, x2, y2);
            int d2 = sign(px, py, x2, y2, x3, y3);
            int d3 = sign(px, py, x3, y3, x1, y1);
            return ((d1 < 0) == (d2 < 0)) && ((d2 < 0) == (d3 < 0));
        }
        private int sign(int px, int py, int x1, int y1, int x2, int y2) {
            return (px - x2) * (y1 - y2) - (x1 - x2) * (py - y2);
        }
    }

    // ==================== 颜色工具 ====================

    /** Color -> RGBA8888 int（与 Pixmap 的 RGBA8888 布局一致） */
    static int rgba(Color c) {
        int r = (int)(c.r * 255), g = (int)(c.g * 255), b = (int)(c.b * 255), a = (int)(c.a * 255);
        return ((r & 0xFF) << 24) | ((g & 0xFF) << 16) | ((b & 0xFF) << 8) | (a & 0xFF);
    }

    static int rgba(String hex) { return rgba(Color.valueOf(hex)); }
    static int rgba(int hex) { return rgba(new Color(hex)); }

    // ==================== 贴图生成 ====================

    private TextureRegion makeRegion(String name, Raster r) {
        // 用两参数构造函数（Arc 默认即为 RGBA8888，与官方 Pixmaps.scale/noise 一致），
        // 再用稳定存在的 setRaw(x, y, rgba8888) 逐像素写入。
        Pixmap pix = new Pixmap(S, S);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                pix.setRaw(x, y, r.get(x, y));
            }
        }
        TextureRegion region = new TextureRegion(new Texture(pix));
        Core.atlas.addRegion(name, region);
        pix.dispose();
        return region;
    }

    private TextureRegion generateSprite(String name, int mainColor, int accentColor, boolean isFly) {
        Raster r = new Raster();
        r.fill(rgba("2a2a2a"));
        int mc = rgba(mainColor), ac = rgba(accentColor);

        if (isFly) {
            r.fillTriangle(4, 28, 16, 4, 28, 28, mc);
            r.fillTriangle(10, 26, 16, 10, 22, 26, ac);
        } else {
            r.fillRect(4, 4, 24, 24, mc);
            r.fillRect(10, 10, 12, 12, ac);
            if (!name.contains("factory")) {
                r.drawRect(4, 4, 24, 24, rgba("888888"));
            }
        }
        r.drawRect(0, 0, S, S, ac);
        return makeRegion(name, r);
    }

    private TextureRegion generateSpecialSprite(String name, int mainColor, int accentColor, int type) {
        Raster r = new Raster();
        r.fill(rgba("2a2a2a"));
        int mc = rgba(mainColor), ac = rgba(accentColor);

        switch (type) {
            case 0:
                r.fillTriangle(2, 30, 16, 4, 30, 30, mc);
                r.fillRect(2, 20, 28, 10, mc);
                r.fillRect(12, 8, 8, 12, ac);
                break;
            case 1:
                r.fillRect(2, 2, 28, 28, mc);
                r.fillRect(8, 8, 16, 16, ac);
                r.fillTriangle(14, 4, 18, 4, 16, 12, rgba("AAAAAA"));
                break;
            case 2:
                r.fillCircle(16, 16, 14, mc);
                r.fillRect(13, 6, 6, 20, ac);
                r.fillRect(6, 13, 20, 6, ac);
                break;
            case 3:
                r.fillRect(2, 8, 28, 16, mc);
                r.fillRect(8, 2, 16, 8, ac);
                r.fillRect(22, 0, 8, 6, ac);
                r.fillRect(28, 1, 4, 4, rgba("FF5555"));
                break;
            case 4:
                r.fillCircle(16, 16, 12, mc);
                r.fillRect(14, 4, 4, 24, ac);
                r.fillRect(4, 14, 24, 4, ac);
                break;
            case 5:
                r.fillTriangle(16, 2, 30, 16, 16, 30, mc);
                r.fillTriangle(16, 2, 2, 16, 16, 30, mc);
                r.fillCircle(16, 16, 4, ac);
                break;
            case 6:
                r.fillCircle(16, 16, 14, mc);
                r.fillCircle(16, 16, 8, ac);
                r.fillCircle(10, 12, 3, rgba("FF0000"));
                r.fillCircle(22, 12, 3, rgba("FF0000"));
                r.fillRect(14, 18, 4, 2, rgba("FF0000"));
                break;
            case 7:
                r.fillEllipse(4, 8, 24, 16, mc);
                r.fillRect(0, 14, 6, 4, ac);
                r.fillRect(26, 14, 6, 4, ac);
                r.fillRect(14, 12, 4, 8, mc);
                break;
            case 8:
                r.fillRect(2, 6, 28, 20, mc);
                r.fillRect(6, 10, 20, 12, ac);
                r.fillRect(4, 8, 4, 4, rgba("FFCC00"));
                r.fillRect(24, 8, 4, 4, rgba("FFCC00"));
                break;
            case 9:
                r.fillCircle(16, 18, 10, mc);
                r.fillTriangle(16, 2, 20, 8, 12, 8, ac);
                r.fillRect(14, 8, 4, 16, ac);
                r.fillCircle(12, 16, 2, rgba("FF0000"));
                r.fillCircle(20, 16, 2, rgba("FF0000"));
                break;
            case 10:
                r.fillRect(4, 12, 24, 8, mc);
                r.fillRect(14, 2, 4, 20, ac);
                r.fillRect(2, 14, 28, 4, ac);
                r.fillCircle(16, 16, 2, rgba("FF0000"));
                r.fillRect(28, 14, 4, 4, rgba("FF0000"));
                break;
            case 11:
                r.fillCircle(16, 16, 14, mc);
                r.fillTriangle(16, 4, 24, 10, 24, 22, ac);
                r.fillTriangle(16, 4, 8, 10, 8, 22, ac);
                for (int i = 0; i < 5; i++) {
                    int cx = 10 + (i % 3) * 6;
                    int cy = 8 + (i / 3) * 8;
                    r.fillCircle(cx, cy, 2, rgba("FFAA00"));
                }
                break;
            case 12:
                r.fillRect(4, 4, 12, 24, mc);
                r.fillRect(6, 6, 8, 20, ac);
                r.fillRect(20, 8, 4, 16, rgba("CCCCCC"));
                r.fillTriangle(18, 8, 26, 8, 22, 2, rgba("CCCCCC"));
                r.fillRect(18, 24, 8, 4, rgba("CCCCCC"));
                break;
            case 13:
                r.fillRect(2, 12, 28, 8, mc);
                r.fillRect(26, 10, 6, 12, ac);
                r.fillCircle(16, 16, 4, rgba("00FFFF"));
                r.fillRect(6, 14, 8, 4, rgba("00FFFF"));
                break;
            case 14:
                r.fillCircle(16, 16, 12, mc);
                r.fillTriangle(16, 2, 22, 10, 10, 10, ac);
                for (int i = 0; i < 6; i++) {
                    float angle = i * 60f + 30;
                    int cx = (int)(16 + 14 * Math.cos(angle * Math.PI / 180));
                    int cy = (int)(16 + 14 * Math.sin(angle * Math.PI / 180));
                    r.fillCircle(cx, cy, 2, rgba("FF5500"));
                }
                break;
            case 15:
                r.fillCircle(16, 16, 14, mc);
                r.fillCircle(16, 16, 10, ac);
                r.drawCircle(16, 16, 6, rgba("88CCFF"));
                r.drawCircle(16, 16, 10, rgba("88CCFF"));
                break;
            case 16:
                r.fillRect(2, 8, 28, 20, mc);
                r.fillTriangle(4, 8, 8, 2, 12, 8, ac);
                r.fillTriangle(12, 8, 16, 2, 20, 8, ac);
                r.fillTriangle(20, 8, 24, 2, 28, 8, ac);
                r.fillRect(6, 14, 20, 4, ac);
                r.fillRect(10, 18, 12, 6, ac);
                break;
            case 17:
                r.fillCircle(16, 16, 14, mc);
                r.drawCircle(16, 16, 10, ac);
                r.drawCircle(16, 16, 6, ac);
                r.fillCircle(16, 16, 3, rgba("00FFAA"));
                for (int i = 0; i < 8; i++) {
                    float angle = i * 45f;
                    int cx = (int)(16 + 12 * Math.cos(angle * Math.PI / 180));
                    int cy = (int)(16 + 12 * Math.sin(angle * Math.PI / 180));
                    r.fillCircle(cx, cy, 2, rgba("00FFAA"));
                }
                break;
        }
        r.drawRect(0, 0, S, S, ac);
        return makeRegion(name, r);
    }

    // ==================== 单位注册 ====================

    @Override
    public void loadContent() {
        greenGroundUnit = new UnitType("green-tank") {{
            health = 240f; speed = 1.2f; hitSize = 10f; armor = 2f;
            region = generateSprite("green-tank", 0xFF4488AA, 0xFFFFFFFF, false);
            weapons.add(new Weapon() {{ reload = 20f; bullet = Bullets.standardCopper; x = 4f; y = 0f; }});
            weapons.add(new Weapon() {{ reload = 40f; bullet = Bullets.standardDagger; x = -4f; y = 0f; rotate = true; }});
            playerControllable = true;
        }};
        blueFlyUnit = new UnitType("blue-flyer") {{
            health = 120f; speed = 3.8f; hitSize = 7f; flying = true;
            region = generateSprite("blue-flyer", 0xFF3399FF, 0xFFFFFFFF, true);
            weapons.add(new Weapon() {{ reload = 10f; bullet = Bullets.standardDagger; x = 0f; y = 2f; }});
            playerControllable = true;
        }};
        redFactoryUnit = new UnitType("red-factory") {{
            health = 400f; speed = 0f; hitSize = 18f; isFactory = true; buildSpeed = 2.5f;
            region = generateSprite("red-factory", 0xFFFF5544, 0xFFFFFF00, false);
            weapons.add(new Weapon() {{ reload = 30f; bullet = Bullets.standardCopper; x = 0f; y = -4f; }});
            playerControllable = true;
        }};

        purpleNavalUnit = new UnitType("purple-naval") {{
            health = 180f; speed = 2.5f; hitSize = 12f; naval = true;
            region = generateSpecialSprite("purple-naval", 0xFF9944FF, 0xFFDD88FF, 0);
            weapons.add(new Weapon() {{
                reload = 35f;
                bullet = new MissileBulletType(3.5f, 15f) {{ homingPower = 0.05f; trailLength = 5; lifetime = 60f; }};
                x = 6f; y = 0f; mirror = true;
            }});
            playerControllable = true;
        }};
        yellowMinerUnit = new UnitType("yellow-miner") {{
            health = 80f; speed = 0.8f; hitSize = 9f; miningSpeed = 3.0f;
            region = generateSpecialSprite("yellow-miner", 0xFFFFCC00, 0xFFFFFF66, 1);
            weapons.add(new Weapon() {{ reload = 50f; bullet = Bullets.standardCopper; x = 0f; y = 0f; }});
            playerControllable = true;
        }};

        cyanSupportUnit = new UnitType("cyan-support") {{
            health = 100f; speed = 4.2f; hitSize = 8f; flying = true;
            region = generateSpecialSprite("cyan-support", 0xFF00FFCC, 0xFFFFFFFF, 2);
            abilities.add(new RepairFieldAbility(50f, 20f, 60f));
            weapons.add(new Weapon() {{ reload = 15f; bullet = Bullets.standardCopper; x = 0f; y = -2f; }});
            playerControllable = true;
        }};
        graySiegeUnit = new UnitType("gray-siege") {{
            health = 800f; speed = 0.45f; hitSize = 22f; armor = 6f;
            region = generateSpecialSprite("gray-siege", 0xFF888888, 0xFFFFAA00, 3);
            weapons.add(new Weapon() {{ reload = 80f; bullet = Bullets.artilleryDense; x = 12f; y = 0f; rotate = true; recoil = 6f; shootCone = 15f; }});
            weapons.add(new Weapon() {{ reload = 25f; bullet = Bullets.standardDagger; x = -6f; y = -4f; }});
            playerControllable = true;
        }};

        whiteAntiAirUnit = new UnitType("white-anti-air") {{
            health = 150f; speed = 1.5f; hitSize = 11f; armor = 1f;
            region = generateSpecialSprite("white-anti-air", 0xFFFFFFFF, 0xFF888888, 4);
            weapons.add(new Weapon() {{ reload = 5f; bullet = Bullets.flakScrap; x = 4f; y = 0f; mirror = true; }});
            playerControllable = true;
        }};
        pinkEWarUnit = new UnitType("pink-e-war") {{
            health = 90f; speed = 3.5f; hitSize = 6f; flying = true;
            region = generateSpecialSprite("pink-e-war", 0xFFFF66FF, 0xFFFFFFFF, 5);
            weapons.add(new Weapon() {{ reload = 30f; bullet = Bullets.standardDagger; x = 0f; y = 0f; }});
            abilities.add(new ForceFieldAbility(40f, 0.5f, 100f, 400f));
            playerControllable = true;
        }};

        orangeSuicideUnit = new UnitType("orange-suicide") {{
            health = 60f; speed = 5.5f; hitSize = 6f; flying = true;
            region = generateSpecialSprite("orange-suicide", 0xFFFF8800, 0xFFFFFFFF, 6);
            damage = 80f;
            abilities.add(new SpawnerAbility(Bullets.explosive, 3, 0f, 0f));
            playerControllable = true;
        }};
        blueSubmarineUnit = new UnitType("blue-submarine") {{
            health = 160f; speed = 2.8f; hitSize = 10f; naval = true;
            region = generateSpecialSprite("blue-submarine", 0xFF0044AA, 0xFF88CCFF, 7);
            armor = 0.5f; targetPriority = -1f;
            weapons.add(new Weapon() {{
                reload = 45f;
                bullet = new MissileBulletType(4.5f, 20f) {{ homingPower = 0.1f; trailLength = 8; lifetime = 80f; }};
                x = 8f; y = 0f; mirror = true;
            }});
            playerControllable = true;
        }};

        brownTransportUnit = new UnitType("brown-transport") {{
            health = 200f; speed = 1.8f; hitSize = 14f; armor = 3f;
            itemCapacity = 50;
            region = generateSpecialSprite("brown-transport", 0xFF8B4513, 0xFFDEB887, 8);
            weapons.add(new Weapon() {{ reload = 30f; bullet = Bullets.standardCopper; x = 5f; y = 0f; mirror = true; }});
            playerControllable = true;
        }};
        blackReaperUnit = new UnitType("black-reaper") {{
            health = 350f; speed = 1.0f; hitSize = 13f; armor = 4f;
            region = generateSpecialSprite("black-reaper", 0xFF1A1A1A, 0xFF666666, 9);
            weapons.add(new Weapon() {{ reload = 60f; bullet = Bullets.standardThorium; x = 0f; y = 4f; shootCone = 30f; }});
            weapons.add(new Weapon() {{ reload = 15f; bullet = Bullets.standardDagger; x = 6f; y = -2f; mirror = true; }});
            abilities.add(new ShieldRegenFieldAbility(20f, 60f, 40f, 300f));
            playerControllable = true;
        }};

        redSniperUnit = new UnitType("red-sniper") {{
            health = 80f; speed = 0.6f; hitSize = 10f; armor = 0f;
            region = generateSpecialSprite("red-sniper", 0xFFFF0000, 0xFFFFFFFF, 10);
            weapons.add(new Weapon() {{ reload = 120f; bullet = Bullets.artilleryDense; x = 8f; y = 0f; rotate = true; recoil = 8f; shootCone = 2f; range = 30f; }});
            playerControllable = true;
        }};
        greenHiveUnit = new UnitType("green-hive") {{
            health = 300f; speed = 1.2f; hitSize = 18f; flying = true;
            region = generateSpecialSprite("green-hive", 0xFF00AA44, 0xFFFFFF00, 11);
            weapons.add(new Weapon() {{ reload = 2f; bullet = Bullets.standardDagger; x = 0f; y = 0f; shootCone = 360f; mirror = false; alternate = false; shots = 3; }});
            abilities.add(new SpawnerAbility(Bullets.explosive, 2, 60f, 0f));
            playerControllable = true;
        }};

        goldPaladinUnit = new UnitType("gold-paladin") {{
            health = 500f; speed = 0.8f; hitSize = 16f; armor = 8f;
            region = generateSpecialSprite("gold-paladin", 0xFFFFD700, 0xFFFFFFFF, 12);
            weapons.add(new Weapon() {{ reload = 45f; bullet = Bullets.standardThorium; x = 0f; y = 6f; shootCone = 20f; }});
            abilities.add(new ForceFieldAbility(60f, 1.0f, 150f, 600f));
            playerControllable = true;
        }};
        silverRailgunUnit = new UnitType("silver-railgun") {{
            health = 200f; speed = 0.9f; hitSize = 12f; armor = 3f;
            region = generateSpecialSprite("silver-railgun", 0xFFC0C0C0, 0xFF00FFFF, 13);
            weapons.add(new Weapon() {{
                reload = 90f;
                bullet = new RailBulletType() {{ damage = 150f; length = 200f; pierce = true; }};
                x = 8f; y = 0f; rotate = true; recoil = 10f; shootCone = 1f; range = 40f;
            }});
            playerControllable = true;
        }};

        crimsonDemonUnit = new UnitType("crimson-demon") {{
            health = 180f; speed = 3.2f; hitSize = 9f; flying = true;
            region = generateSpecialSprite("crimson-demon", 0xFFCC0000, 0xFFFF8800, 14);
            weapons.add(new Weapon() {{ reload = 20f; bullet = Bullets.incendiary; x = 0f; y = 2f; shots = 2; spread = 10f; }});
            abilities.add(new SpawnerAbility(Bullets.incendiary, 1, 10f, 0f));
            playerControllable = true;
        }};
        cobaltShielderUnit = new UnitType("cobalt-shielder") {{
            health = 280f; speed = 0.9f; hitSize = 14f; armor = 5f;
            region = generateSpecialSprite("cobalt-shielder", 0xFF0044CC, 0xFFFFFFFF, 15);
            weapons.add(new Weapon() {{ reload = 25f; bullet = Bullets.standardCopper; x = 5f; y = 0f; mirror = true; }});
            abilities.add(new ShieldRegenFieldAbility(50f, 20f, 100f, 400f));
            abilities.add(new ForceFieldAbility(50f, 0.5f, 120f, 500f));
            playerControllable = true;
        }};

        rainbowTitanUnit = new UnitType("rainbow-titan") {{
            health = 5000f; speed = 0.5f; hitSize = 28f; armor = 12f;
            region = generateSpecialSprite("rainbow-titan", 0xFFFF00FF, 0xFFFFFFFF, 16);
            weapons.add(new Weapon() {{ reload = 60f; bullet = Bullets.artilleryDense; x = 14f; y = 0f; rotate = true; recoil = 12f; shootCone = 10f; range = 35f; }});
            weapons.add(new Weapon() {{ reload = 15f; bullet = Bullets.standardThorium; x = 8f; y = -6f; mirror = true; shootCone = 30f; }});
            weapons.add(new Weapon() {{ reload = 8f; bullet = Bullets.flakScrap; x = 0f; y = 8f; shootCone = 360f; mirror = false; }});
            abilities.add(new RepairFieldAbility(80f, 30f, 80f));
            abilities.add(new ShieldRegenFieldAbility(40f, 80f, 60f, 500f));
            abilities.add(new ForceFieldAbility(80f, 1.5f, 200f, 800f));
            playerControllable = true;
        }};
        emeraldCommanderUnit = new UnitType("emerald-commander") {{
            health = 600f; speed = 0.7f; hitSize = 20f; armor = 5f;
            region = generateSpecialSprite("emerald-commander", 0xFF00C853, 0xFF69F0AE, 17);
            weapons.add(new Weapon() {{ reload = 40f; bullet = Bullets.standardCopper; x = 6f; y = 0f; mirror = true; }});
            abilities.add(new ShieldRegenFieldAbility(60f, 25f, 120f, 600f));
            abilities.add(new RepairFieldAbility(60f, 15f, 100f));
            playerControllable = true;
        }};

        content.register(greenGroundUnit);
        content.register(blueFlyUnit);
        content.register(redFactoryUnit);
        content.register(purpleNavalUnit);
        content.register(yellowMinerUnit);
        content.register(cyanSupportUnit);
        content.register(graySiegeUnit);
        content.register(whiteAntiAirUnit);
        content.register(pinkEWarUnit);
        content.register(orangeSuicideUnit);
        content.register(blueSubmarineUnit);
        content.register(brownTransportUnit);
        content.register(blackReaperUnit);
        content.register(redSniperUnit);
        content.register(greenHiveUnit);
        content.register(goldPaladinUnit);
        content.register(silverRailgunUnit);
        content.register(crimsonDemonUnit);
        content.register(cobaltShielderUnit);
        content.register(rainbowTitanUnit);
        content.register(emeraldCommanderUnit);
    }
}
