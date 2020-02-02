package com.tikscorp.pdfhighlightextractor.highlight;

import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;

public class HighLightedObject {
    private HighlightedRectangle highlight;
    private PDImageXObject pdImage;

    public HighlightedRectangle getHighlight() {
        return highlight;
    }

    public PDImageXObject getPdImage() {
        return pdImage;
    }

    public HighLightedObject(PDImageXObject pdImage, HighlightedRectangle highlight) {
    this.pdImage = pdImage;
    this.highlight = highlight;
    }
}
