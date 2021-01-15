package technology.tabula;

import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.font.PDFont;

@SuppressWarnings("serial")
public class TextElement extends Rectangle implements HasText {

    private final String text;
    private final PDFont font;
    private float fontSize;
    private float widthOfSpace, dir;

    public TextElement(float y, float x, float width, float height,
                       PDFont font, float fontSize, String c, float widthOfSpace) {
        this(y, x, width, height, font, fontSize, c, widthOfSpace, 0f);
    }

    public TextElement(float y, float x, float width, float height,
                       PDFont font, float fontSize, String c, float widthOfSpace, float dir) {
        super();
        this.setRect(x, y, width, height);
        this.text = c;
        this.widthOfSpace = widthOfSpace;
        this.fontSize = fontSize;
        this.font = font;
        this.dir = dir;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String getText(boolean useLineReturns) {
        return text;
    }

    public float getDirection() {
        return dir;
    }

    public float getWidthOfSpace() {
        return widthOfSpace;
    }

    public PDFont getFont() {
        return font;
    }

    public float getFontSize() {
        return fontSize;
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        String s = super.toString();
        sb.append(s.substring(0, s.length() - 1));
        sb.append(String.format(",text=\"%s\"]", this.getText()));
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + java.lang.Float.floatToIntBits(dir);
        result = prime * result + ((font == null) ? 0 : font.hashCode());
        result = prime * result + java.lang.Float.floatToIntBits(fontSize);
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        result = prime * result + java.lang.Float.floatToIntBits(widthOfSpace);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        TextElement other = (TextElement) obj;
        if (java.lang.Float.floatToIntBits(dir) != java.lang.Float
                .floatToIntBits(other.dir))
            return false;
        if (font == null) {
            if (other.font != null)
                return false;
        } else if (!font.equals(other.font))
            return false;
        if (java.lang.Float.floatToIntBits(fontSize) != java.lang.Float
                .floatToIntBits(other.fontSize))
            return false;
        if (text == null) {
            if (other.text != null)
                return false;
        } else if (!text.equals(other.text))
            return false;
        return java.lang.Float.floatToIntBits(widthOfSpace) == java.lang.Float
                .floatToIntBits(other.widthOfSpace);
    }

}
