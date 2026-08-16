package rlbot.commons.render;

import rlbot.commons.math.Vector2;
import rlbot.commons.math.Vector3;
import rlbot.commons.protocol.RLBotInterface;
import rlbot.commons.protocol.RLBotListenerAdapter;
import rlbot.flat.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A wrapper for RLBot's debug rendering features.
 * Note that debug rendering must be enabled in the match configuration for debug rendering to show up.
 */
public class Renderer extends RLBotListenerAdapter {

    /**
     * Name of the default render group.
     */
    private static final String DEFAULT_GROUP = "DEFAULT";

    private final RLBotInterface rlbot;

    private float screenWidthScale = 1f;
    private float screenHeightScale = 1f;

    /**
     * True if rendering is enabled. Note the Renderer will still send render messages if
     * draw methods are called, but RLBot will likely ignore the messages.
     */
    private boolean canRender = true;

    /**
     * True, if a construction of a render group is in progress.
     * See {@link #end()} to finish the group.
     */
    private boolean isRendering = false;

    /**
     * Name of the most recently begun render group.
     */
    private String currentGroupId = DEFAULT_GROUP;

    /**
     * The color used if no other color is specified in draw method calls.
     */
    private Color color = Color.WHITE;

    private final List<RenderMessageT> groupContent = new ArrayList<>();

    /**
     * All active render groups. Render groups persist until overridden or cleared.
     */
    private final HashSet<String> activeGroupIds = new HashSet<>();

    public Renderer(RLBotInterface rlbot) {
        this(rlbot, 1920, 1080);
    }

    public Renderer(RLBotInterface rlbot, int screenWidth, int screenHeight) {
        this.rlbot = rlbot;
        rlbot.addListener(this);
        assumeScreenSize(screenWidth, screenHeight);
    }

    @Override
    public void onMatchConfig(MatchConfigurationT config) {
        canRender = config.getEnableRendering() == DebugRendering.OnByDefault;
    }

    @Override
    public void onRenderingStatus(RenderingStatusT status) {
        canRender = status.getStatus();
    }

    /**
     * @return true if rendering is enabled.
     */
    public boolean canRender() {
        return canRender;
    }

    /**
     * @return true if a construction of a render group is in progress.
     */
    public boolean isRendering() {
        return isRendering;
    }

    /**
     * @return the name of the most recently begun render group.
     */
    public String getCurrentGroupId() {
        return currentGroupId;
    }

    /**
     * @return the color used if no other color is specified in draw method calls.
     */
    public Color getColor() {
        return color;
    }

    /**
     * Sets the color used if no other color is specified in draw method calls.
     *
     * @param color the new default color.
     */
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * @return all active render groups. Render groups persist until overridden or cleared.
     */
    public Set<String> getActiveGroupIds() {
        return new HashSet<>(activeGroupIds);
    }

    /**
     * Set the presumed screen size when scaling 2D rendering to screen-space coordinates.
     *
     * @param width  the presumed screen width in pixels.
     * @param height the presumed screen height in pixels.
     */
    public void assumeScreenSize(int width, int height) {
        screenWidthScale = 1f / width;
        screenHeightScale = 1f / height;
    }

    /**
     * Begin a new render group using the default group id.
     * Subsequent draw method calls will queue render messages to this group.
     * End the render group by calling {@link #end()}.
     * <p>
     * A render group is a collection of render messages that persist until overridden or cleared.
     */
    public void begin() {
        begin(DEFAULT_GROUP, null);
    }

    /**
     * Begin a new render group.
     * Subsequent draw method calls will queue render messages to this group.
     * End the render group by calling {@link #end()}.
     * <p>
     * A render group is a collection of render messages that persist until overridden or cleared.
     *
     * @param groupId name of render group.
     */
    public void begin(String groupId) {
        begin(groupId, null);
    }

    /**
     * Begin a new render group.
     * Subsequent draw method calls will queue render messages to this group.
     * End the render group by calling {@link #end()}.
     * <p>
     * A render group is a collection of render messages that persist until overridden or cleared.
     *
     * @param groupId name of render group.
     * @param color   optional, new default color. Can also be changed any time by calling {@link #setColor(Color)}.
     */
    public void begin(String groupId, Color color) {
        assert !isRendering : "begin() was called twice without end() in between";
        groupContent.clear();
        currentGroupId = groupId;
        if (color != null) {
            this.color = color;
        }
        isRendering = true;
    }

    /**
     * End the current render group and send the render messages to RLBot. {@link #begin()} must be called first.
     * The render group will contain all render messages queued between the two calls.
     */
    public void end() {
        assert isRendering : "end() was called without a call to begin() first";
        var group = new RenderGroupT();
        group.setId(currentGroupId.hashCode());
        group.setRenderMessages(groupContent.toArray(new RenderMessageT[0]));
        rlbot.sendRenderGroup(group);
        activeGroupIds.add(currentGroupId);
        isRendering = false;
    }

    /**
     * Clear the default render group.
     *
     * @return true, if the group existed.
     */
    public boolean clear() {
        return clear(DEFAULT_GROUP);
    }

    /**
     * Clear a render group.
     *
     * @param groupId name of render group to clear.
     * @return true, if the group existed.
     */
    public boolean clear(String groupId) {
        var remove = new RemoveRenderGroupT();
        remove.setId(groupId.hashCode());
        rlbot.sendRemoveRenderGroup(remove);
        return activeGroupIds.remove(groupId);
    }

    /**
     * Clear all active render groups.
     */
    public void clearAll() {
        for (String id : activeGroupIds) {
            var remove = new RemoveRenderGroupT();
            remove.setId(id.hashCode());
            rlbot.sendRemoveRenderGroup(remove);
        }
        activeGroupIds.clear();
    }

    /**
     * Add a render message to the current render group.
     * It is typically more convenient to use the other draw methods.
     * 2D positions and sizes must be in screen-space, e.g. x=0.1 is 10% of screen width.
     *
     * @param msg the render message to add.
     */
    public void draw(RenderMessageT msg) {
        assert isRendering : "Attempting to draw without a render group. Please call begin() first and end() afterwards.";
        groupContent.add(msg);
    }

    /**
     * Draw a line in 3D space between two anchors.
     *
     * @param start one end of the line.
     * @param end   other end of the line.
     */
    public void drawLine3D(RenderAnchorT start, RenderAnchorT end) {
        drawLine3D(start, end, color);
    }

    /**
     * Draw a line in 3D space between two anchors.
     *
     * @param start one end of the line.
     * @param end   other end of the line.
     * @param color color of the line. Using the default color if null.
     */
    public void drawLine3D(RenderAnchorT start, RenderAnchorT end, Color color) {
        var line = new Line3DT();
        line.setStart(start);
        line.setEnd(end);
        line.setColor((color != null ? color : this.color).toColorT());
        draw(message(RenderType.Line3D, line));
    }

    /**
     * Draw a line in 3D space between two anchors.
     *
     * @param start one end of the line.
     * @param end   other end of the line.
     * @param color color of the line.
     */
    public void drawLine3D(RenderAnchorT start, RenderAnchorT end, ColorT color) {
        drawLine3D(start, end, Color.fromColorT(color));
    }

    /**
     * Draw a line in 3D space between two points.
     *
     * @param start one end of the line.
     * @param end   other end of the line.
     */
    public void drawLine3D(Vector3 start, Vector3 end) {
        drawLine3D(start, end, color);
    }

    /**
     * Draw a line in 3D space between two points.
     *
     * @param start one end of the line.
     * @param end   other end of the line.
     * @param color color of the line. Using the default color if null.
     */
    public void drawLine3D(Vector3 start, Vector3 end, Color color) {
        drawLine3D(anchor(start), anchor(end), color);
    }

    /**
     * Draw a line in 3D space going through the provided points.
     *
     * @param points the sequence of points the line will go through.
     */
    public void drawPolyLine3D(Vector3T[] points) {
        drawPolyLine3D(points, color);
    }

    /**
     * Draw a line in 3D space going through the provided points.
     *
     * @param points the sequence of points the line will go through.
     * @param color  color of the line. Using the default color if null.
     */
    public void drawPolyLine3D(Vector3T[] points, Color color) {
        var polyLine = new PolyLine3DT();
        polyLine.setPoints(points);
        polyLine.setColor((color != null ? color : this.color).toColorT());
        draw(message(RenderType.PolyLine3D, polyLine));
    }

    /**
     * Draw a line in 3D space going through the provided points.
     *
     * @param points the sequence of points the line will go through.
     */
    public void drawPolyLine3D(Vector3[] points) {
        drawPolyLine3D(points, color);
    }

    /**
     * Draw a line in 3D space going through the provided points.
     *
     * @param points the sequence of points the line will go through.
     * @param color  color of the line. Using the default color if null.
     */
    public void drawPolyLine3D(Vector3[] points, Color color) {
        drawPolyLine3D(toVector3TArray(points), color);
    }

    /**
     * Draw text in 2D space. x and y uses pixel coordinates based on the declared screen size.
     * The characters of the font are 20 pixels wide and 10 pixels tall when scale is 1.
     *
     * @param text the text to draw.
     * @param x    x position of text in pixel coordinates.
     * @param y    y position of text in pixel coordinates.
     */
    public void drawText2D(String text, float x, float y) {
        drawText2D(text, x, y, 1f, color, null, TextHAlign.Left, TextVAlign.Top);
    }

    /**
     * Draw text in 2D space. x and y uses pixel coordinates based on the declared screen size.
     * The characters of the font are 20 pixels wide and 10 pixels tall when scale is 1.
     *
     * @param text  the text to draw.
     * @param x     x position of text in pixel coordinates.
     * @param y     y position of text in pixel coordinates.
     * @param scale scale of text.
     */
    public void drawText2D(String text, float x, float y, float scale) {
        drawText2D(text, x, y, scale, color, null, TextHAlign.Left, TextVAlign.Top);
    }

    /**
     * Draw text in 2D space. x and y uses pixel coordinates based on the declared screen size.
     * The characters of the font are 20 pixels wide and 10 pixels tall when scale is 1.
     *
     * @param text       the text to draw.
     * @param x          x position of text in pixel coordinates.
     * @param y          y position of text in pixel coordinates.
     * @param scale      scale of text.
     * @param foreground color of text. Uses the default color if null.
     * @param background color of background for the text. Uses transparent if null.
     * @param hAlign     horizontal alignment.
     * @param vAlign     vertical alignment.
     */
    public void drawText2D(String text, float x, float y, float scale, Color foreground, Color background, int hAlign, int vAlign) {
        var string2D = new String2DT();
        string2D.setText(text);
        string2D.setX(x * screenWidthScale);
        string2D.setY(y * screenHeightScale);
        string2D.setScale(scale);
        string2D.setForeground((foreground != null ? foreground : color).toColorT());
        string2D.setBackground((background != null ? background : Color.TRANSPARENT).toColorT());
        string2D.setHAlign(hAlign);
        string2D.setVAlign(vAlign);
        draw(message(RenderType.String2D, string2D));
    }

    /**
     * Draw text in 2D space. Position uses pixel coordinates based on the declared screen size.
     * The characters of the font are 20 pixels wide and 10 pixels tall when scale is 1.
     *
     * @param text     the text to draw.
     * @param position position of text in pixel coordinates.
     */
    public void drawText2D(String text, Vector2 position) {
        drawText2D(text, position.x(), position.y());
    }

    /**
     * Draw text in 2D space. Position uses pixel coordinates based on the declared screen size.
     * The characters of the font are 20 pixels wide and 10 pixels tall when scale is 1.
     *
     * @param text     the text to draw.
     * @param position position of text in pixel coordinates.
     * @param scale    scale of text.
     */
    public void drawText2D(String text, Vector2 position, float scale) {
        drawText2D(text, position.x(), position.y(), scale);
    }

    /**
     * Draw text in 2D space. Position uses pixel coordinates based on the declared screen size.
     * The characters of the font are 20 pixels wide and 10 pixels tall when scale is 1.
     *
     * @param text       the text to draw.
     * @param position   position of text in pixel coordinates.
     * @param scale      scale of text.
     * @param foreground color of text. Uses the default color if null.
     * @param background color of background for the text. Uses transparent if null.
     * @param hAlign     horizontal alignment.
     * @param vAlign     vertical alignment.
     */
    public void drawText2D(String text, Vector2 position, float scale, Color foreground, Color background, int hAlign, int vAlign) {
        drawText2D(text, position.x(), position.y(), scale, foreground, background, hAlign, vAlign);
    }

    /**
     * Draw text in 3D space.
     * Characters of the font are 20 pixels tall and 10 pixels wide when scale is 1.
     *
     * @param text   the text to draw.
     * @param anchor the anchor to draw the text at.
     */
    public void drawText3D(String text, RenderAnchorT anchor) {
        drawText3D(text, anchor, 1f, color, null, TextHAlign.Left, TextVAlign.Top);
    }

    /**
     * Draw text in 3D space.
     * Characters of the font are 20 pixels tall and 10 pixels wide when scale is 1.
     *
     * @param text   the text to draw.
     * @param anchor the anchor to draw the text at.
     * @param scale  the scale of the text.
     */
    public void drawText3D(String text, RenderAnchorT anchor, float scale) {
        drawText3D(text, anchor, scale, color, null, TextHAlign.Left, TextVAlign.Top);
    }

    /**
     * Draw text in 3D space.
     * Characters of the font are 20 pixels tall and 10 pixels wide when scale is 1.
     *
     * @param text       the text to draw.
     * @param anchor     the anchor to draw the text at.
     * @param scale      the scale of the text.
     * @param foreground color of text. Uses the default color if null.
     * @param background color of background for the text. Uses transparent if null.
     * @param hAlign     horizontal alignment.
     * @param vAlign     vertical alignment.
     */
    public void drawText3D(String text, RenderAnchorT anchor, float scale, Color foreground, Color background, int hAlign, int vAlign) {
        var string3D = new String3DT();
        string3D.setText(text);
        string3D.setAnchor(anchor);
        string3D.setScale(scale);
        string3D.setForeground((foreground != null ? foreground : color).toColorT());
        string3D.setBackground((background != null ? background : Color.TRANSPARENT).toColorT());
        string3D.setHAlign(hAlign);
        string3D.setVAlign(vAlign);
        draw(message(RenderType.String3D, string3D));
    }

    /**
     * Draw text in 3D space.
     * Characters of the font are 20 pixels tall and 10 pixels wide when scale is 1.
     *
     * @param text     the text to draw.
     * @param position the position to draw the text at.
     */
    public void drawText3D(String text, Vector3 position) {
        drawText3D(text, anchor(position));
    }

    /**
     * Draw text in 3D space.
     * Characters of the font are 20 pixels tall and 10 pixels wide when scale is 1.
     *
     * @param text     the text to draw.
     * @param position the position to draw the text at.
     * @param scale    the scale of the text.
     */
    public void drawText3D(String text, Vector3 position, float scale) {
        drawText3D(text, anchor(position), scale);
    }

    /**
     * Draw text in 3D space.
     * Characters of the font are 20 pixels tall and 10 pixels wide when scale is 1.
     *
     * @param text       the text to draw.
     * @param position   the position to draw the text at.
     * @param scale      the scale of the text.
     * @param foreground color of text. Uses the default color if null.
     * @param background color of background for the text. Uses transparent if null.
     * @param hAlign     horizontal alignment.
     * @param vAlign     vertical alignment.
     */
    public void drawText3D(String text, Vector3 position, float scale, Color foreground, Color background, int hAlign, int vAlign) {
        drawText3D(text, anchor(position), scale, foreground, background, hAlign, vAlign);
    }

    /**
     * Draw a rectangle in 2D space.
     * X, y, width, and height uses pixel coordinates based on the declared screen size.
     *
     * @param x      x position of the rectangle in pixel coordinates.
     * @param y      y position of the rectangle in pixel coordinates.
     * @param width  width of the rectangle in pixels.
     * @param height height of the rectangle in pixels.
     */
    public void drawRect2D(float x, float y, float width, float height) {
        drawRect2D(x, y, width, height, color, true);
    }

    /**
     * Draw a rectangle in 2D space.
     * X, y, width, and height uses pixel coordinates based on the declared screen size.
     *
     * @param x        x position of the rectangle in pixel coordinates.
     * @param y        y position of the rectangle in pixel coordinates.
     * @param width    width of the rectangle in pixels.
     * @param height   height of the rectangle in pixels.
     * @param color    color of the rectangle. Uses the default color if null.
     * @param centered whether the rectangle should be centered at (x, y), or if (x, y) is the top left corner.
     */
    public void drawRect2D(float x, float y, float width, float height, Color color, boolean centered) {
        var rect = new Rect2DT();
        rect.setX(x * screenWidthScale);
        rect.setY(y * screenHeightScale);
        rect.setWidth(width * screenWidthScale);
        rect.setHeight(height * screenHeightScale);
        rect.setColor((color != null ? color : this.color).toColorT());
        draw(message(RenderType.Rect2D, rect));
    }

    /**
     * Draw a rectangle in 2D space.
     * Position and size uses pixel coordinates based on the declared screen size.
     *
     * @param position position of the rectangle in pixel coordinates.
     * @param size     size of the rectangle in pixels.
     */
    public void drawRect2D(Vector2 position, Vector2 size) {
        drawRect2D(position.x(), position.y(), size.x(), size.y(), color, true);
    }

    /**
     * Draw a rectangle in 2D space.
     * Position and size uses pixel coordinates based on the declared screen size.
     *
     * @param position position of the rectangle in pixel coordinates.
     * @param size     size of the rectangle in pixels.
     * @param color    color of the rectangle. Uses the default color if null.
     * @param centered whether the rectangle should be centered at the position, or if the position is the top left corner.
     */
    public void drawRect2D(Vector2 position, Vector2 size, Color color, boolean centered) {
        drawRect2D(position.x(), position.y(), size.x(), size.y(), color, centered);
    }

    /**
     * Draw a rectangle in 3D space.
     * Width and height uses pixel coordinates based on the declared screen size.
     * The size does not change based on distance to the camera.
     *
     * @param anchor the anchor to draw the rectangle at.
     * @param width  the width of the rectangle as a fraction of screen-space width.
     * @param height the height of the rectangle as a fraction of screen-space height.
     */
    public void drawRect3D(RenderAnchorT anchor, float width, float height) {
        drawRect3D(anchor, width, height, color);
    }

    /**
     * Draw a rectangle in 3D space.
     * Width and height uses pixel coordinates based on the declared screen size.
     * The size does not change based on distance to the camera.
     *
     * @param anchor the anchor to draw the rectangle at.
     * @param width  the width of the rectangle as a fraction of screen-space width.
     * @param height the height of the rectangle as a fraction of screen-space height.
     * @param color  color of the rectangle. Uses the default color if null.
     */
    public void drawRect3D(RenderAnchorT anchor, float width, float height, Color color) {
        var rect = new Rect3DT();
        rect.setAnchor(anchor);
        rect.setWidth(width * screenWidthScale);
        rect.setHeight(height * screenHeightScale);
        rect.setColor((color != null ? color : this.color).toColorT());
        draw(message(RenderType.Rect3D, rect));
    }

    /**
     * Draw a rectangle in 3D space.
     * Width and height uses pixel coordinates based on the declared screen size.
     * The size does not change based on distance to the camera.
     *
     * @param anchor the anchor to draw the rectangle at.
     * @param size   the size of the rectangle using fractions of screen-space size.
     */
    public void drawRect3D(RenderAnchorT anchor, Vector2 size) {
        drawRect3D(anchor, size.x(), size.y());
    }

    /**
     * Draw a rectangle in 3D space.
     * Width and height uses pixel coordinates based on the declared screen size.
     * The size does not change based on distance to the camera.
     *
     * @param anchor the anchor to draw the rectangle at.
     * @param size   the size of the rectangle using fractions of screen-space size.
     * @param color  color of the rectangle. Uses the default color if null.
     */
    public void drawRect3D(RenderAnchorT anchor, Vector2 size, Color color) {
        drawRect3D(anchor, size.x(), size.y(), color);
    }

    /**
     * Draw a rectangle in 3D space.
     * Width and height uses pixel coordinates based on the declared screen size.
     * The size does not change based on distance to the camera.
     *
     * @param position the position of the rectangle.
     * @param width    the width of the rectangle as a fraction of screen-space width.
     * @param height   the height of the rectangle as a fraction of screen-space height.
     */
    public void drawRect3D(Vector3 position, float width, float height) {
        drawRect3D(anchor(position), width, height);
    }

    /**
     * Draw a rectangle in 3D space.
     * Width and height uses pixel coordinates based on the declared screen size.
     * The size does not change based on distance to the camera.
     *
     * @param position the position of the rectangle.
     * @param width    the width of the rectangle as a fraction of screen-space width.
     * @param height   the height of the rectangle as a fraction of screen-space height.
     * @param color    color of the rectangle. Uses the default color if null.
     */
    public void drawRect3D(Vector3 position, float width, float height, Color color) {
        drawRect3D(anchor(position), width, height, color);
    }

    /**
     * Draw a rectangle in 3D space.
     * Width and height uses pixel coordinates based on the declared screen size.
     * The size does not change based on distance to the camera.
     *
     * @param position the position of the rectangle.
     * @param size     the size of the rectangle using fractions of screen-space size.
     */
    public void drawRect3D(Vector3 position, Vector2 size) {
        drawRect3D(anchor(position), size.x(), size.y());
    }

    /**
     * Draw a rectangle in 3D space.
     * Width and height uses pixel coordinates based on the declared screen size.
     * The size does not change based on distance to the camera.
     *
     * @param position the position of the rectangle.
     * @param size     the size of the rectangle using fractions of screen-space size.
     * @param color    color of the rectangle. Uses the default color if null.
     */
    public void drawRect3D(Vector3 position, Vector2 size, Color color) {
        drawRect3D(anchor(position), size.x(), size.y(), color);
    }

    private static RenderMessageT message(byte type, Object value) {
        var variety = new RenderTypeUnion();
        variety.setType(type);
        variety.setValue(value);
        var msg = new RenderMessageT();
        msg.setVariety(variety);
        return msg;
    }

    private static RenderAnchorT anchor(Vector3 position) {
        var anchor = new RenderAnchorT();
        anchor.setWorld(position.toVector3T());
        return anchor;
    }

    private static Vector3T[] toVector3TArray(Vector3[] points) {
        var flat = new Vector3T[points.length];
        for (int i = 0; i < points.length; i++) {
            flat[i] = points[i].toVector3T();
        }
        return flat;
    }
}
