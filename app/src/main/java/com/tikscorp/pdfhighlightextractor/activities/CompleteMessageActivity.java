package com.tikscorp.pdfhighlightextractor.activities;

import android.app.ActionBar;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;

import com.tikscorp.pdfhighlightextractor.BuildConfig;
import com.tikscorp.pdfhighlightextractor.R;
import com.tikscorp.pdfhighlightextractor.constants.Constants;

import java.io.File;

public class CompleteMessageActivity extends AppCompatActivity {

    File outPath = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_message);
        Intent intent = getIntent();
        outPath = new File(intent.getStringExtra("OutFilePath"));

    }


    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(Constants.SHOW_EXTRACTED_PDF_BUTTON,true);
        startActivity(intent);
        finish();
        super.onBackPressed();
    }

    protected void openOutputFile(File out) {
        MimeTypeMap map = MimeTypeMap.getSingleton();
        String ext = MimeTypeMap.getFileExtensionFromUrl(out.getName());
        String type = map.getMimeTypeFromExtension(ext);
        if (type == null)
            type = "application/pdf";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri data = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", out);
        intent.setDataAndType(data, type);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    public void openOutFileOnClick(View view) {
        openOutputFile(outPath);
    }


    public void startMainActivity(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(Constants.SHOW_EXTRACTED_PDF_BUTTON,true);
        startActivity(intent);
    }
}
