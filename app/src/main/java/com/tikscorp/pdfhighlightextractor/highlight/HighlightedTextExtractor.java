package com.tikscorp.pdfhighlightextractor.highlight;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import android.widget.ProgressBar;
import android.widget.TextView;


import com.tikscorp.pdfhighlightextractor.activities.MainActivity;
import com.tikscorp.pdfhighlightextractor.activities.ProgressBarActivity;
import com.tikscorp.pdfhighlightextractor.constants.Constants;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import com.tom_roush.pdfbox.rendering.PDFRenderer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HighlightedTextExtractor extends AsyncTask<Void, Void, Void> {


    private ProgressBar progressBar;
    private String outputPath;
    private InputStream in;
    private File out;
    private boolean stopped;
    private int start;
    private int finish;
    private boolean isAnnotationFound = true;
    private TextView textViewPercentage = null;

    public static final int FINISH = -1;
    private Logger logger = Logger.getLogger(HighlightedTextExtractor.class.getName());
    private Context context;

    public HighlightedTextExtractor(InputStream in, File out, int start, int finish, String outputPath, ProgressBar progressBar, Context context, TextView textViewPercentage) {
        this.in = in;
        this.out = out;
        this.start = start;
        this.finish = finish;
        this.outputPath = outputPath;
        this.progressBar = progressBar;
        this.context = context;
        this.textViewPercentage = textViewPercentage;

    }


    public void run() {
        PDDocument sourceDocument = null;
        stopped = false;
        try {
            Thread.sleep(1000);
            setProgressBar(1);
            final PDDocument outPutDocument = new PDDocument();
            sourceDocument = PDDocument.load(in);
            List<PDPage> allPages = new ArrayList<PDPage>();
            Iterator<PDPage> iterator = sourceDocument.getDocumentCatalog().getPages().iterator();
            while (iterator.hasNext()) {
                allPages.add(iterator.next());
            }
            int progress = 0;
            finish = finish == FINISH ? allPages.size() : finish;
            List<PDPage> selectedPages = allPages.subList(start - 1, finish);
            List<OutputPage> outputPageList = new LinkedList<>();
            int pageNo = 0;
            for (PDPage page : selectedPages) {

                ++pageNo;
                if (stopped)
                    break;
                List<PDAnnotation> annotationList = page.getAnnotations();

                List<HighLightedObject> highLightedObjectList = new LinkedList<>();
                for (PDAnnotation annotation : annotationList) {
                    if (annotation instanceof PDAnnotationTextMarkup) {
                        highLightedObjectList = processHighlightedText(pageNo, sourceDocument, (PDAnnotationTextMarkup) annotation, page,highLightedObjectList);
                    }
                }

                if(!highLightedObjectList.isEmpty())
                    outputPageList.add(new OutputPage(pageNo,highLightedObjectList));

                progress = progress+(100/selectedPages.size());
                System.out.println(progress);
                if(progress<=100)
                    setProgressBar(progress);


            }
            if(outputPageList.isEmpty()) {
                isAnnotationFound = false;
            }else {
                for (OutputPage outputPage : outputPageList) {
                    addPageNoToPageAndDrawImage(outPutDocument, outputPage);
                }
            }
            sourceDocument.close();
            outPutDocument.save(out);
            outPutDocument.close();

        } catch (Exception ex) {
            logger.log(Level.ALL,"Error while extracting highlighted text from pdf",ex);
        }

    }

    private void addPageNoToPageAndDrawImage(PDDocument outDocument, OutputPage outputPage) throws IOException {
        int height = 0;
        int width = 0;
        for (HighLightedObject highLightedObject :
                outputPage.getHighLightedObjectList()) {
            height += highLightedObject.getHighlight().getRectangle().getHeight()+5;
            if(width< highLightedObject.getHighlight().getRectangle().getWidth())
                width = (int) highLightedObject.getHighlight().getRectangle().getWidth() ;
        }
        outDocument.addPage(new PDPage(new PDRectangle(width+1, height+30)));
        int page_index = outDocument.getDocumentCatalog().getPages().getCount();
        PDPage ipage = outDocument.getPage(page_index-1);
        PDPageContentStream contentStream = new PDPageContentStream(outDocument, ipage,true,true);

        contentStream.beginText();//Setting the font to the Content stream
        contentStream.setFont(PDType1Font.HELVETICA, 11);
        //Setting the position for the line
        contentStream.newLineAtOffset(0, 5);
        //Adding text in the form of string
        contentStream.showText("P." + outputPage.getSourcePageNumber());
        //Ending the content stream
        contentStream.endText();
        int imageYLoc = height+30;
        for (HighLightedObject highLightedObject :outputPage.getHighLightedObjectList()) {
            imageYLoc -= highLightedObject.getHighlight().getRectangle().getHeight()+5;
            contentStream.drawImage(highLightedObject.getPdImage(), 0, imageYLoc, highLightedObject.getHighlight().getRectangle().getWidth(), highLightedObject.getHighlight().getRectangle().getHeight());
        }
        setProgressBar(100);
        contentStream.close();
    }



    private List<HighLightedObject> processHighlightedText(int pageNo, PDDocument pddDocument, PDAnnotationTextMarkup highlight, PDPage page, List<HighLightedObject> highLightedObjectList) throws IOException {
        System.out.print("Running...\n");
        PDRectangle rectangle = highlight.getRectangle();
        page.setCropBox(rectangle);
        page.setAnnotations(null);// Here you draw a rectangle around the area you want to specify
        PDFRenderer renderer = new PDFRenderer(pddDocument);
        Bitmap image = renderer.renderImage(pageNo-1, 3f, Bitmap.Config.RGB_565);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100 , bos);
        PDImageXObject pdImage = new PDImageXObject(pddDocument, new ByteArrayInputStream(bos.toByteArray()),
                COSName.DCT_DECODE, image.getWidth(), image.getHeight(),
                8,
                PDDeviceRGB.INSTANCE);
        highLightedObjectList.add(new HighLightedObject(pdImage,highlight));
        //Draw Image
        return  highLightedObjectList;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        run();
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
       new ProgressBarActivity().startCompleteMessageActivity(out);

        if(!isAnnotationFound) {
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra(Constants.VAR_SHOW_NO_HIGH_LIGHT_FOUND_MESSAGE,true);
            context.startActivity(intent);
        }
    }

    public void setProgressBar(int progress) {
        progressBar.setProgress(progress);
        textViewPercentage.setText(String.valueOf(progress)+"%");

    }



}
