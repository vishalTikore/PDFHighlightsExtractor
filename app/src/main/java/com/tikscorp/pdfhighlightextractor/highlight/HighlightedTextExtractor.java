package com.tikscorp.pdfhighlightextractor.highlight;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.os.AsyncTask;

import android.os.Environment;
import android.os.ParcelFileDescriptor;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.tikscorp.pdfhighlightextractor.constants.Constants.OUT_FOLDER;

public class HighlightedTextExtractor extends AsyncTask<Void, Void, Void> {


    private final ParcelFileDescriptor fd;
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

    public HighlightedTextExtractor(InputStream in, File out, int start, int finish, String outputPath, ProgressBar progressBar, Context context, TextView textViewPercentage, ParcelFileDescriptor fileDescriptor) {
        this.in = in;
        this.out = out;
        this.start = start;
        this.finish = finish;
        this.outputPath = outputPath;
        this.progressBar = progressBar;
        this.context = context;
        this.textViewPercentage = textViewPercentage;
        this.fd = fileDescriptor;

    }


    public void run() {
        PDDocument sourceDocument = null;
        stopped = false;
        try {
            Thread.sleep(1000);
            new Runnable() {
                @Override
                public void run() {
                    setProgressBar(1);
                }
            };
            PDDocument outPutDocument = new PDDocument();
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
            PdfRenderer pdfRenderer = new PdfRenderer(fd);
            for (PDPage page : selectedPages) {

                ++pageNo;
                if (stopped)
                    break;

                List<PDAnnotation> annotationList = page.getAnnotations();

                List<HighLightedObject> highLightedObjectList = new LinkedList<>();
                for (PDAnnotation annotation : annotationList) {
                    if (annotation instanceof PDAnnotationTextMarkup) {
                        highLightedObjectList = processHighlightedText(pageNo, sourceDocument, (PDAnnotationTextMarkup) annotation, page,highLightedObjectList, pdfRenderer);
                    }
                }
                if (!highLightedObjectList.isEmpty()) {
                    Collections.sort(highLightedObjectList, new Comparator<HighLightedObject>() {
                        @Override
                        public int compare(HighLightedObject h1, HighLightedObject h2) {
                            float h1x = h1.getHighlight().getRectangle().getLowerLeftX();
                            float h1y = h1.getHighlight().getRectangle().getLowerLeftY();
                            float h2x = h2.getHighlight().getRectangle().getLowerLeftX();
                            float h2y = h2.getHighlight().getRectangle().getLowerLeftY();
                            return (int) (h1y == h2y ? (h2x - h1x) : (h2y - h1y));
                        }
                    });
                    outputPageList.add(new OutputPage(pageNo, highLightedObjectList));
                }
                progress = progress+(100/selectedPages.size());
                System.out.println(progress);
                if(progress<=100) {

                    setProgressBar(progress);

                }
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



    private List<HighLightedObject> processHighlightedText(int pageNo, PDDocument pddDocument, PDAnnotationTextMarkup highlight, PDPage page, List<HighLightedObject> highLightedObjectList, PdfRenderer pdfRenderer) throws IOException {
        System.out.print("Running...\n");
        PDRectangle rectangle = highlight.getRectangle();

        page.setCropBox(rectangle);


       // page.setAnnotations(null);// Here you draw a rectangle around the area you want to specify
       // PDFRenderer renderer = new PDFRenderer(pddDocument);
       //google lib code
        PdfRenderer.Page pdfPage = pdfRenderer.openPage(pageNo-1);

        int lowerLeftX = (int) rectangle.getLowerLeftX();
        int lowerLeftY = (int) rectangle.getLowerLeftY();
        int upperRightX = (int) rectangle.getUpperRightX();
        int upperRightY = (int) rectangle.getUpperRightY();

        Bitmap image = Bitmap.createBitmap( 3*pdfPage.getWidth(), 3*pdfPage.getHeight(), Bitmap.Config.ARGB_8888);
        (new Canvas(image)).drawColor(-1);
        pdfPage.render(image, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
       // Bitmap image = renderer.renderImage(pageNo-1, 3f, Bitmap.Config.RGB_565);
        writeToImage(pageNo, image, "img_x");

        Bitmap image1 = Bitmap.createBitmap(image, 3*lowerLeftX,image.getHeight()-3*upperRightY, 3*(upperRightX-lowerLeftX),3*(upperRightY-lowerLeftY));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        image1.compress(Bitmap.CompressFormat.JPEG, 100 , bos);

      //  writeToImage(pageNo, image1, "img_y");
        PDImageXObject pdImage = new PDImageXObject(pddDocument, new ByteArrayInputStream(bos.toByteArray()),
                COSName.DCT_DECODE, image1.getWidth(), image1.getHeight(),
                8,
                PDDeviceRGB.INSTANCE);
        highLightedObjectList.add(new HighLightedObject(pdImage,highlight));
        pdfPage.close();
        //Draw Image
        return  highLightedObjectList;
    }

    private void writeToImage(int pageNo, Bitmap image, String prefix) throws FileNotFoundException {
        File lawfulStorage = new File(Environment.getExternalStorageDirectory(), OUT_FOLDER);
        File imageFile = new File(lawfulStorage, prefix + (pageNo-1) + ".png");
        FileOutputStream imageOutStream = new FileOutputStream(imageFile);
        image.compress(Bitmap.CompressFormat.JPEG, 100, imageOutStream);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        run();
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {

        if(!isAnnotationFound) {
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK| Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(Constants.VAR_SHOW_NO_HIGH_LIGHT_FOUND_MESSAGE,true);
            context.startActivity(intent);
        }else
            new ProgressBarActivity().startCompleteMessageActivity(out);

    }

    public void setProgressBar(final int progress) {
        ProgressBarActivity progressBarActivity = (ProgressBarActivity) context;
        progressBarActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress(progress);
                textViewPercentage.setText(String.valueOf(progress)+"%");
            }
        });


    }



}
