package rlbot.commons.render;

import rlbot.flat.ColorT;

/**
 * An immutable color with red, green, blue, and alpha components.
 * <p>
 * Each component is an integer in the range [0, 255]. Colors are used to specify
 * the color of the various {@link Renderer} draw calls.
 */
public final class Color {

    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    public static final Color BLACK = new Color(0, 0, 0, 255);
    public static final Color WHITE = new Color(255, 255, 255, 255);
    public static final Color GREY = new Color(128, 128, 128, 255);
    public static final Color GRAY = GREY;
    public static final Color BLUE = new Color(0, 0, 255, 255);
    public static final Color RED = new Color(255, 0, 0, 255);
    public static final Color GREEN = new Color(0, 128, 0, 255);
    public static final Color LIME = new Color(0, 255, 0, 255);
    public static final Color YELLOW = new Color(255, 255, 0, 255);
    public static final Color ORANGE = new Color(225, 128, 0, 255);
    public static final Color CYAN = new Color(0, 255, 255, 255);
    public static final Color PINK = new Color(255, 0, 255, 255);
    public static final Color PURPLE = new Color(128, 0, 128, 255);
    public static final Color TEAL = new Color(0, 128, 128, 255);

    private final int r;
    private final int g;
    private final int b;
    private final int a;

    /**
     * Creates a new fully opaque color.
     *
     * @param r the red component in the range [0, 255].
     * @param g the green component in the range [0, 255].
     * @param b the blue component in the range [0, 255].
     */
    public Color(int r, int g, int b) {
        this(r, g, b, 255);
    }

    /**
     * Creates a new color.
     *
     * @param r the red component in the range [0, 255].
     * @param g the green component in the range [0, 255].
     * @param b the blue component in the range [0, 255].
     * @param a the alpha component in the range [0, 255].
     */
    public Color(int r, int g, int b, int a) {
        this.r = clamp(r);
        this.g = clamp(g);
        this.b = clamp(b);
        this.a = clamp(a);
    }

    /**
     * Creates a {@link Color} from a flatbuffer {@link ColorT}.
     *
     * @param color the flatbuffer color to convert.
     * @return the resulting color.
     */
    public static Color fromColorT(ColorT color) {
        return new Color(color.getR(), color.getG(), color.getB(), color.getA());
    }

    /**
     * Creates a fully opaque color from hue, saturation, and value components.
     * Hue is measured as a fraction of the color wheel in the range [0, 1],
     * where 0 is red, 1/6 is yellow, 1/3 is green, 1/2 is cyan, 2/3 is blue, and 5/6 is magenta.
     * Saturation and value are in the range [0, 1].
     *
     * @param hue        the hue of the color in the range [0, 1].
     * @param saturation the saturation of the color in the range [0, 1].
     * @param value      the value (brightness) of the color in the range [0, 1].
     * @param alpha      the alpha component in the range [0, 255].
     * @return the resulting color.
     */
    public static Color fromHSV(float hue, float saturation, float value, int alpha) {
        int i = (int) Math.floor(hue * 6);
        float f = hue * 6 - i;
        float p = value * (1 - saturation);
        float q = value * (1 - f * saturation);
        float t = value * (1 - (1 - f) * saturation);

        float r;
        float g;
        float b;
        switch (Math.floorMod(i, 6)) {
            case 0 -> {
                r = value;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = value;
                b = p;
            }
            case 2 -> {
                r = p;
                g = value;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = value;
            }
            case 4 -> {
                r = t;
                g = p;
                b = value;
            }
            default -> {
                r = value;
                g = p;
                b = q;
            }
        }

        return new Color((int) Math.floor(r * 255), (int) Math.floor(g * 255), (int) Math.floor(b * 255), alpha);
    }

    /**
     * Returns the color of the given team (blue or orange).
     * <p>
     * Returns white for any other team.
     *
     * @param team the team index. 0 is blue, 1 is orange.
     * @return the color of the given team.
     */
    public static Color teamColor(int team) {
        return teamColor(team, false);
    }

    /**
     * Returns the color of the given team, or a secondary color if {@code altColor} is true.
     * <p>
     * Returns gray for any other team.
     *
     * @param team     the team index. 0 is blue, 1 is orange.
     * @param altColor whether to use the secondary color of the team.
     * @return the color of the given team.
     */
    public static Color teamColor(int team, boolean altColor) {
        if (team == 0) {
            return altColor ? CYAN : BLUE;
        } else if (team == 1) {
            return altColor ? RED : ORANGE;
        }
        return altColor ? GRAY : WHITE;
    }

    public int getR() {
        return r;
    }

    public int getG() {
        return g;
    }

    public int getB() {
        return b;
    }

    public int getA() {
        return a;
    }

    /**
     * @return this color as a flatbuffer {@link ColorT}.
     */
    public ColorT toColorT() {
        var color = new ColorT();
        color.setR(r);
        color.setG(g);
        color.setB(b);
        color.setA(a);
        return color;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
