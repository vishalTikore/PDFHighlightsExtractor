package com.tikscorp.pdfhighlightextractor.activities;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tikscorp.pdfhighlightextractor.highlight.HighlightedTextExtractor;
import com.tikscorp.pdfhighlightextractor.R;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static com.tikscorp.pdfhighlightextractor.constants.Constants.OUT_FOLDER;

public class ProgressBarActivity extends AppCompatActivity {

    ProgressBar progressBar;
    File outputPath = null;
    static Context context = null;
    HighlightedTextExtractor highlightedTextExtractor;
    TextView textViewPercentage = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress_bar);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setMax(100);
        progressBar.setVisibility(View.VISIBLE);
        textViewPercentage = findViewById(R.id.textPercentage);
        Intent intent = getIntent();
        extractHighlightedTextFromPDF(intent.getData());
        context = this;
    }

    public void startCompleteMessageActivity(File out) {
        Intent completeMessageIntent = new Intent(context, CompleteMessageActivity.class);
        completeMessageIntent.putExtra("OutFilePath",out.getAbsolutePath());
        context.startActivity(completeMessageIntent);
    }

    @Override
    public void onBackPressed() {
        highlightedTextExtractor.cancel(true);
    }

    /**
     * Creates out_folder and call HighlightedTextExtractor's Async run method
      * @param originalUri File Uri
     */
    private void extractHighlightedTextFromPDF(Uri originalUri) {
        File outputFolder = new File(Environment.getExternalStorageDirectory(), OUT_FOLDER);
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        InputStream file = null;
        try {
            file = getContentResolver().openInputStream(originalUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        outputPath = new File(outputFolder,getFileName(originalUri));
        PDFBoxResourceLoader.init(getApplicationContext());
        highlightedTextExtractor = new HighlightedTextExtractor(file, outputPath, 1, -1,outputPath.getPath(),progressBar,this,textViewPercentage);
        highlightedTextExtractor.execute();
    }

    /**
     * Method to returns file name from Uri
     * @param uri Uri
     * @return File name
     */
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try(Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
