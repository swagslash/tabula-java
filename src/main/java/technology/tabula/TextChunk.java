package technology.tabula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.text.Normalizer;

@SuppressWarnings("serial")
public class TextChunk extends RectangularTextContainer<TextElement> {
    public static final TextChunk EMPTY = new TextChunk(0, 0, 0, 0);
    private static final float AVERAGE_CHAR_TOLERANCE = 0.3f;

    public TextChunk(float top, float left, float width, float height) {
        super(top, left, width, height);
    }

    public TextChunk(TextElement textElement) {
        super(textElement.y, textElement.x, textElement.width, textElement.height);
        this.add(textElement);
    }

    public TextChunk(List<TextElement> textElements) {
        this(textElements.get(0));
        for (int i = 1; i < textElements.size(); i++) {
            this.add(textElements.get(i));
        }
    }

    private enum DirectionalityOptions {
        LTR, NONE, RTL
    }

    // I hate Java so bad.
    // we're making this HashMap static! which requires really funky initialization per http://stackoverflow.com/questions/6802483/how-to-directly-initialize-a-hashmap-in-a-literal-way/6802502#6802502
    private static final HashMap<Byte, DirectionalityOptions> directionalities;

    static {
        directionalities = new HashMap<>();
        // BCT = bidirectional character type
        directionalities.put(java.lang.Character.DIRECTIONALITY_ARABIC_NUMBER, DirectionalityOptions.LTR);               // Weak BCT    "AN" in the Unicode specification.
        directionalities.put(java.lang.Character.DIRECTIONALITY_BOUNDARY_NEUTRAL, DirectionalityOptions.NONE);            // Weak BCT    "BN" in the Unicode specification.
        directionalities.put(java.lang.Character.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR, DirectionalityOptions.LTR);     // Weak BCT    "CS" in the Unicode specification.
        directionalities.put(java.lang.Character.DIRECTIONALITY_EUROPEAN_NUMBER, DirectionalityOptions.LTR);             // Weak BCT    "EN" in the Unicode specification.
        directionalities.put(java.lang.Character.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR, DirectionalityOptions.LTR);   // Weak BCT    "ES" in the Unicode specification.
        directionalities.put(java.lang.Character.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR, DirectionalityOptions.LTR);  // Weak BCT    "ET" in the Unicode specification.
        directionalities.put(java.lang.Character.DIRECTIONALITY_LEFT_TO_RIGHT, DirectionalityOptions.LTR);              // Strong BCT  "L" in the Unicode specification.
        directionalities.put(java.lang.Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING, DirectionalityOptions.LTR);     // Strong BCT  "LRE" in the Unicode specification.
        directionalities.put(java.lang.Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE, DirectionalityOptions.LTR);      // Strong BCT  "LRO" in the Unicode specification.
        directionalities.put(java.lang.Character.DIRECTIONALITY_NONSPACING_MARK, DirectionalityOptions.NONE);             // Weak BCT    "NSM" in the Unicode specification.
        directionalities.put(java.lang.Character.DIRECTIONALITY_OTHER_NEUTRALS, DirectionalityOptions.NONE);              // Neutral BCT "ON" in the Unicode specification.
        directionalities.put(java.lang.Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR, DirectionalityOptions.NONE);         // Neutral BCT "B" in the Unicode specification.
        directionalities.put(java.lang.Character.DIRECTIONALITY_POP_DIRECTIONAL_FORMAT, DirectionalityOptions.NONE);      // Weak BCT    "PDF" in the Unicode specification.
        directionalities.put(java.lang.Character.DIRECTIONALITY_RIGHT_TO_LEFT, DirectionalityOptions.RTL);              // Strong BCT  "R" in the Unicode specification.
        directionalities.put(java.lang.Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC, DirectionalityOptions.RTL);       // Strong BCT  "AL" in the Unicode specification.
        directionalities.put(java.lang.Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING, DirectionalityOptions.RTL);    // Strong BCT  "RLE" in the Unicode specification.
        directionalities.put(java.lang.Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE, DirectionalityOptions.RTL);     // Strong BCT  "RLO" in the Unicode specification.
        directionalities.put(java.lang.Character.DIRECTIONALITY_SEGMENT_SEPARATOR, DirectionalityOptions.RTL);          // Neutral BCT "S" in the Unicode specification.
        directionalities.put(java.lang.Character.DIRECTIONALITY_UNDEFINED, DirectionalityOptions.NONE);                   // Undefined BCT.
        directionalities.put(java.lang.Character.DIRECTIONALITY_WHITESPACE, DirectionalityOptions.NONE);                  // Neutral BCT "WS" in the Unicode specification.
    }

    /**
     * Splits a TextChunk into N TextChunks, where each chunk is of a single directionality, and
     * then reverse the RTL ones.
     * what we're doing here is *reversing* the Unicode bidi algorithm
     * in the language of that algorithm, each chunk is a (maximal) directional run.
     * We attach whitespace to the beginning of non-RTL
     **/
    public TextChunk groupByDirectionality(Boolean isLtrDominant) {
        if (this.getTextElements().isEmpty()) {
            throw new IllegalArgumentException();
        }

        ArrayList<ArrayList<TextElement>> chunks = new ArrayList<>();
        ArrayList<TextElement> buff = new ArrayList<>();
        DirectionalityOptions buffDirectionality = DirectionalityOptions.NONE; // the directionality of the characters in buff

        for (TextElement te : this.getTextElements()) {
            //TODO: we need to loop over the textelement characters
            //      because it is possible for a textelement to contain multiple characters?

            if (buff.isEmpty()) {
                buff.add(te);
                buffDirectionality = directionalities.get(Character.getDirectionality(te.getText().charAt(0)));
            } else {
                if (buffDirectionality == DirectionalityOptions.NONE) {
                    buffDirectionality = directionalities.get(Character.getDirectionality(te.getText().charAt(0)));
                }
                DirectionalityOptions teDirectionality = directionalities.get(Character.getDirectionality(te.getText().charAt(0)));

                if (teDirectionality == buffDirectionality || teDirectionality == DirectionalityOptions.NONE) {
                    if (Character.getDirectionality(te.getText().charAt(0)) == java.lang.Character.DIRECTIONALITY_WHITESPACE && (buffDirectionality == (isLtrDominant ? DirectionalityOptions.RTL : DirectionalityOptions.LTR))) {
                        buff.add(0, te);
                    } else {
                        buff.add(te);
                    }
                } else {
                    // finish this chunk
                    if (buffDirectionality == DirectionalityOptions.RTL) {
                        Collections.reverse(buff);
                    }
                    chunks.add(buff);

                    // and start a new one
                    buffDirectionality = directionalities.get(Character.getDirectionality(te.getText().charAt(0)));
                    buff = new ArrayList<>();
                    buff.add(te);
                }
            }
        }
        if (buffDirectionality == DirectionalityOptions.RTL) {
            Collections.reverse(buff);
        }
        chunks.add(buff);
        ArrayList<TextElement> everything = new ArrayList<>();
        if (!isLtrDominant) {
            Collections.reverse(chunks);
        }
        for (ArrayList<TextElement> group : chunks) {
            everything.addAll(group);
        }
        return new TextChunk(everything);
    }

    @Override public int isLtrDominant() {
        int ltrCnt = 0;
        int rtlCnt = 0;
        for (int i = 0; i < this.getTextElements().size(); i++) {
            String elementText = this.getTextElements().get(i).getText();
            for (int j = 0; j < elementText.length(); j++) {
                byte dir = Character.getDirectionality(elementText.charAt(j));
                if ((dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT) ||
                        (dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING) ||
                        (dir == Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE)) {
                    ltrCnt++;
                } else if ((dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT) ||
                        (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) ||
                        (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING) ||
                        (dir == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE)) {
                    rtlCnt++;
                }
            }
        }
        return java.lang.Integer.compare(ltrCnt, rtlCnt); // 1 is LTR, 0 is neutral, -1 is RTL
    }

    public TextChunk merge(TextChunk other) {
        super.merge(other);
        return this;
    }

    public void add(TextElement textElement) {
        this.textElements.add(textElement);
        this.merge(textElement);
    }

    public void add(List<TextElement> elements) {
        for (TextElement te : elements) {
            this.add(te);
        }
    }

    @Override
    public String getText() {
        if (this.textElements.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (TextElement te : this.textElements) {
            sb.append(te.getText());
        }
        return Normalizer.normalize(sb.toString(), Normalizer.Form.NFKC).trim();
    }

    @Override
    public String getText(boolean useLineReturns) {
        return getText();
    }

    /**
     * Returns true if text contained in this TextChunk is the same repeated character
     */
    public boolean isSameChar(Character c) {
        return isSameChar(new Character[]{c});
    }

    public boolean isSameChar(Character[] c) {
        String s = this.getText();
        List<Character> chars = Arrays.asList(c);
        for (int i = 0; i < s.length(); i++) {
            if (!chars.contains(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Splits a TextChunk in two, at the position of the i-th TextElement
     */
    public TextChunk[] splitAt(int i) {
        if (i < 1 || i >= this.getTextElements().size()) {
            throw new IllegalArgumentException();
        }

        return new TextChunk[] {
                new TextChunk(this.getTextElements().subList(0, i)),
                new TextChunk(this.getTextElements().subList(i, this.getTextElements().size()))
        };
    }

    /**
     * Removes runs of identical TextElements in this TextChunk
     * For example, if the TextChunk contains this string of characters: "1234xxxxx56xx"
     * and c == 'x' and minRunLength == 4, this method will return a list of TextChunk
     * such that: ["1234", "56xx"]
     */
    public List<TextChunk> squeeze(Character c, int minRunLength) {
        Character currentChar;
        Character lastChar = null;
        int subSequenceLength = 0;
        int subSequenceStart = 0;
        TextChunk[] t;
        List<TextChunk> rv = new ArrayList<>();

        for (int i = 0; i < this.getTextElements().size(); i++) {
            TextElement textElement = this.getTextElements().get(i);
            String text = textElement.getText();
            if (text.length() > 1) {
                currentChar = text.trim().charAt(0);
            } else {
                currentChar = text.charAt(0);
            }


            if (lastChar != null && currentChar.equals(c) && lastChar.equals(currentChar)) {
                subSequenceLength++;
            } else {
                if (((lastChar != null && !lastChar.equals(currentChar)) || i + 1 == this.getTextElements().size()) && subSequenceLength >= minRunLength) {

                    if (subSequenceStart == 0 && subSequenceLength <= this.getTextElements().size() - 1) {
                        t = this.splitAt(subSequenceLength);
                    } else {
                        t = this.splitAt(subSequenceStart);
                        rv.add(t[0]);
                    }
                    rv.addAll(t[1].squeeze(c, minRunLength)); // Lo and behold, recursion.
                    break;

                }
                subSequenceLength = 1;
                subSequenceStart = i;
            }
            lastChar = currentChar;
        }

        if (rv.isEmpty()) { // no splits occurred, hence this.squeeze() == [this]
            if (subSequenceLength >= minRunLength && subSequenceLength < this.textElements.size()) {
                TextChunk[] chunks = this.splitAt(subSequenceStart);
                rv.add(chunks[0]);
            } else {
                rv.add(this);
            }
        }

        return rv;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((textElements == null) ? 0 : textElements.hashCode());
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
        TextChunk other = (TextChunk) obj;
        if (textElements == null) {
            return other.textElements == null;
        } else return textElements.equals(other.textElements);
    }

    public static boolean allSameChar(List<TextChunk> textChunks) {
        /* the previous, far more elegant version of this method failed when there was an empty TextChunk in textChunks.
         * so I rewrote it in an ugly way. but it works!
         * it would be good for this to get rewritten eventually
         * the purpose is basically just to return true iff there are 2+ TextChunks and they're identical.
         * -Jeremy 5/13/2016
         */

        if (textChunks.size() == 1) return false;
        boolean hasHadAtLeastOneNonEmptyTextChunk = false;
        char first = '\u0000';
        for (TextChunk tc : textChunks) {
            if (tc.getText().isEmpty()) {
                continue;
            }
            if (first == '\u0000') {
                first = tc.getText().charAt(0);
            } else {
                hasHadAtLeastOneNonEmptyTextChunk = true;
                if (!tc.isSameChar(first)) return false;
            }
        }
        return hasHadAtLeastOneNonEmptyTextChunk;
    }

    public static List<TextChunk> mergeWords(List<TextElement> textElements) {
        return mergeWords(textElements, new ArrayList<>());
    }

    /**
     * heuristically merge a list of TextElement into a list of TextChunk
     * ported from from PDFBox's PDFTextStripper.writePage, with modifications.
     * Here be dragons
     */
    public static List<TextChunk> mergeWords(List<TextElement> textElements, List<Ruling> verticalRulings) {

        List<TextChunk> textChunks = new ArrayList<>();

        if (textElements.isEmpty()) {
            return textChunks;
        }

        // it's a problem that this `remove` is side-effecty
        // other things depend on `textElements` and it can sometimes lead to the first textElement in textElement
        // not appearing in the final output because it's been removed here.
        // https://github.com/tabulapdf/tabula-java/issues/78
        List<TextElement> copyOfTextElements = new ArrayList<>(textElements);
        textChunks.add(new TextChunk(copyOfTextElements.remove(0)));
        TextChunk firstTC = textChunks.get(0);

        float previousAveCharWidth = (float) firstTC.getWidth();
        float endOfLastTextX = firstTC.getRight();
        float maxYForLine = firstTC.getBottom();
        float maxHeightForLine = (float) firstTC.getHeight();
        float minYTopForLine = firstTC.getTop();
        float lastWordSpacing = -1;
        float wordSpacing, deltaSpace, averageCharWidth, deltaCharWidth;
        float expectedStartOfNextWordX, dist;
        TextElement sp, prevChar;
        TextChunk currentChunk;
        boolean sameLine, acrossVerticalRuling;

        for (TextElement chr : copyOfTextElements) {
            currentChunk = textChunks.get(textChunks.size() - 1);
            prevChar = currentChunk.textElements.get(currentChunk.textElements.size() - 1);

            // if same char AND overlapped, skip
            if ((chr.getText().equals(prevChar.getText())) && (prevChar.overlapRatio(chr) > 0.5)) {
                continue;
            }

            // if chr is a space that overlaps with prevChar, skip
            if (chr.getText().equals(" ") && Utils.feq(prevChar.getLeft(), chr.getLeft()) && Utils.feq(prevChar.getTop(), chr.getTop())) {
                continue;
            }

            // Resets the average character width when we see a change in font
            // or a change in the font size
            if ((chr.getFont() != prevChar.getFont()) || !Utils.feq(chr.getFontSize(), prevChar.getFontSize())) {
                previousAveCharWidth = -1;
            }

            // is there any vertical ruling that goes across chr and prevChar?
            acrossVerticalRuling = false;
            for (Ruling r : verticalRulings) {
                if (
                        (verticallyOverlapsRuling(prevChar, r) && verticallyOverlapsRuling(chr, r)) &&
                                (prevChar.x < r.getPosition() && chr.x > r.getPosition()) || (prevChar.x > r.getPosition() && chr.x < r.getPosition())
                ) {
                    acrossVerticalRuling = true;
                    break;
                }
            }

            // Estimate the expected width of the space based on the
            // space character with some margin.
            wordSpacing = chr.getWidthOfSpace();
            if (java.lang.Float.isNaN(wordSpacing) || wordSpacing == 0) {
                deltaSpace = java.lang.Float.MAX_VALUE;
            } else if (lastWordSpacing < 0) {
                deltaSpace = wordSpacing * 0.5f; // 0.5 == spacing tolerance
            } else {
                deltaSpace = ((wordSpacing + lastWordSpacing) / 2.0f) * 0.5f;
            }

            // Estimate the expected width of the space based on the
            // average character width with some margin. This calculation does not
            // make a true average (average of averages) but we found that it gave the
            // best results after numerous experiments. Based on experiments we also found that
            // .3 worked well.
            if (previousAveCharWidth < 0) {
                averageCharWidth = (float) (chr.getWidth() / chr.getText().length());
            } else {
                averageCharWidth = (float) ((previousAveCharWidth + (chr.getWidth() / chr.getText().length())) / 2.0f);
            }
            deltaCharWidth = averageCharWidth * AVERAGE_CHAR_TOLERANCE;

            // Compares the values obtained by the average method and the wordSpacing method and picks
            // the smaller number.
            expectedStartOfNextWordX = -java.lang.Float.MAX_VALUE;

            if (endOfLastTextX != -1) {
                expectedStartOfNextWordX = endOfLastTextX + Math.min(deltaCharWidth, deltaSpace);
            }

            // new line?
            sameLine = true;
            if (!Utils.overlap(chr.getBottom(), chr.height, maxYForLine, maxHeightForLine)) {
                expectedStartOfNextWordX = -java.lang.Float.MAX_VALUE;
                maxYForLine = -java.lang.Float.MAX_VALUE;
                maxHeightForLine = -1;
                minYTopForLine = java.lang.Float.MAX_VALUE;
                sameLine = false;
            }

            endOfLastTextX = chr.getRight();

            // should we add a space?
            if (!acrossVerticalRuling &&
                    sameLine &&
                    expectedStartOfNextWordX < chr.getLeft() &&
                    !prevChar.getText().endsWith(" ")) {

                sp = new TextElement(prevChar.getTop(),
                        prevChar.getLeft(),
                        expectedStartOfNextWordX - prevChar.getLeft(),
                        (float) prevChar.getHeight(),
                        prevChar.getFont(),
                        prevChar.getFontSize(),
                        " ",
                        prevChar.getWidthOfSpace());

                currentChunk.add(sp);
            } else {
                sp = null;
            }

            maxYForLine = Math.max(chr.getBottom(), maxYForLine);
            maxHeightForLine = (float) Math.max(maxHeightForLine, chr.getHeight());
            minYTopForLine = Math.min(minYTopForLine, chr.getTop());

            dist = chr.getLeft() - (sp != null ? sp.getRight() : prevChar.getRight());

            if (!acrossVerticalRuling &&
                    sameLine &&
                    (dist < 0 ? currentChunk.verticallyOverlaps(chr) : dist < wordSpacing)) {
                currentChunk.add(chr);
            } else { // create a new chunk
                textChunks.add(new TextChunk(chr));
            }

            lastWordSpacing = wordSpacing;
            previousAveCharWidth = (float) (sp != null ? (averageCharWidth + sp.getWidth()) / 2.0f : averageCharWidth);
        }


        List<TextChunk> textChunksSeparatedByDirectionality = new ArrayList<>();
        // count up characters by directionality
        for (TextChunk chunk : textChunks) {
            // choose the dominant direction
            boolean isLtrDominant = chunk.isLtrDominant() != -1; // treat neutral as LTR
            TextChunk dirChunk = chunk.groupByDirectionality(isLtrDominant);
            textChunksSeparatedByDirectionality.add(dirChunk);
        }

        return textChunksSeparatedByDirectionality;
    }

    private static boolean verticallyOverlapsRuling(TextElement te, Ruling r) {
        return Math.max(0, Math.min(te.getBottom(), r.getY2()) - Math.max(te.getTop(), r.getY1())) > 0;
    }
}
