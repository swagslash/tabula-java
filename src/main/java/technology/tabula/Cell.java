package technology.tabula;

import java.awt.geom.Point2D;

public class Cell extends RectangularTextContainer<TextChunk> {

    private boolean spanning;
    private boolean placeholder;

    public Cell(float top, float left, float width, float height) {
        super(top, left, width, height);
        this.setPlaceholder(false);
        this.setSpanning(false);
    }
    public Cell(Point2D topLeft, Point2D bottomRight) {
        this(
                (float) topLeft.getY(),
                (float) topLeft.getX(),
                (float) (bottomRight.getX() - topLeft.getX()),
                (float) (bottomRight.getY() - topLeft.getY())
        );
    }

    @Override
    public String getText(boolean useLineReturns) {
        if (this.textElements.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        this.textElements.sort(Rectangle.ILL_DEFINED_ORDER);
        double curTop = this.textElements.get(0).getTop();
        for (TextChunk tc : this.textElements) {
            if (useLineReturns && tc.getTop() > curTop) {
                sb.append('\r');
            }
            sb.append(tc.getText());
            curTop = tc.getTop();
        }
        return sb.toString().trim();
    }

    @Override
    public String getText() {
        return getText(true);
    }

    public boolean isSpanning() {
        return spanning;
    }

    public void setSpanning(boolean spanning) {
        this.spanning = spanning;
    }

    public boolean isPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(boolean placeholder) {
        this.placeholder = placeholder;
    }
}
