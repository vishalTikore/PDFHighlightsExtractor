package com.tikscorp.pdfhighlightextractor.highlight;

import com.tikscorp.pdfhighlightextractor.highlight.HighLightedObject;

import java.util.List;

class OutputPage {

    public List<HighLightedObject> getHighLightedObjectList() {
        return highLightedObjectList;
    }

    public int getSourcePageNumber() {
        return sourcePageNumber;
    }

    private List<HighLightedObject> highLightedObjectList;
    private int sourcePageNumber;

    public OutputPage(int pageNum, List<HighLightedObject> highLightedObjectList) {
        this.sourcePageNumber = pageNum;
        this.highLightedObjectList = highLightedObjectList;
    }
}
