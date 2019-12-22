package com.tikscorp.pdfhighlightextractor.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tikscorp.pdfhighlightextractor.constants.Constants;
import com.tikscorp.pdfhighlightextractor.R;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.tikscorp.pdfhighlightextractor.constants.Constants.OUT_FOLDER;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private int STORAGE_PERMISSION_CODE = 101;
    private Logger logger = Logger.getLogger(MainActivity.class.getName());
    static Context context = null;
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setContext(this);
        checkPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                STORAGE_PERMISSION_CODE);
        findViewById(R.id.button2).setOnClickListener(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Button showExtractedPDFButton = findViewById(R.id.button3);
        Intent intent = getIntent();
        if(intent.getBooleanExtra(Constants.VAR_SHOW_NO_HIGH_LIGHT_FOUND_MESSAGE,false)){
            showNoAnnotationFoundMessage();
        }
        else{
            boolean show = intent.getBooleanExtra(Constants.SHOW_EXTRACTED_PDF_BUTTON,false);
            if(show){
                showExtractedPDFButton.setVisibility(View.VISIBLE);
            }else
                showExtractedPDFButton.setVisibility(View.GONE);
        }


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
       getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            ViewGroup viewGroup = findViewById(android.R.id.content);
            View dialogView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.customdialog, viewGroup, false);
            Button dialogCloseButton = (Button)dialogView.findViewById(R.id.buttonOk);
            TextView textView = (TextView) dialogView.findViewById(R.id.customTextView);
            textView.setText(Constants.INFO_MSG);
            TextView textView1 = (TextView) dialogView.findViewById(R.id.customTextView1);
            textView1.setText("About App");
            builder.setView(dialogView);
            final AlertDialog alertDialog = builder.create();
            dialogCloseButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    alertDialog.dismiss();
                }
            });
            alertDialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Check Permission if not then request
     * @param permission Permission
     * @param requestCode RequestCode
     */
    public void checkPermission(String permission, int requestCode)
    {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission)
                == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[] { permission },
                    requestCode);
        }
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) return;
        if (null == data) return;
        Uri originalUri = null;
        if (requestCode == 123) {
            originalUri = data.getData();
        } else if (requestCode == 123) {
            originalUri = data.getData();
            final int takeFlags = data.getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(originalUri, takeFlags);
        }
        // Call progressBar Activity
        Intent intent = new Intent(this, ProgressBarActivity.class);
        intent.setData(originalUri);
        startActivity(intent);

    }



    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {
            case R.id.button2:
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/pdf");
                startActivityForResult(intent, 123);
                break;
            case R.id.button3:
                File outputFolder = new File(Environment.getExternalStorageDirectory(),OUT_FOLDER);
                if (!outputFolder.exists()) {
                    outputFolder.mkdirs();
                }
                Uri selectedUri = Uri.parse(Environment.getExternalStorageDirectory() + File.separator+OUT_FOLDER+File.separator);
                Intent fileOpenIntent = new Intent(Intent.ACTION_VIEW);
                fileOpenIntent.setDataAndType(selectedUri, "resource/folder");
                if (fileOpenIntent.resolveActivityInfo(getPackageManager(), 0) != null)
                {
                    startActivity(fileOpenIntent);
                }
                else
                {
                    Toast.makeText(this,"Check any file explorer app is installed on your device",Toast.LENGTH_SHORT).show();
                    logger.log(Level.ALL,"Error in opening folder as no default app found");
                }
                break;

        }
    }

    /**
     * Shows message if not Annotation is found
     * @param      */
    public void showNoAnnotationFoundMessage() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.customdialog, viewGroup, false);
        Button dialogCloseButton = (Button)dialogView.findViewById(R.id.buttonOk);
        TextView textView1 = (TextView) dialogView.findViewById(R.id.customTextView1);
        textView1.setText("Try Another PDF");
        TextView textView = (TextView) dialogView.findViewById(R.id.customTextView);
        textView.setText(Constants.NO_ANNOTATION_FOUND_MSG);
        builder.setView(dialogView);
        final AlertDialog alertDialog = builder.create();
        dialogCloseButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                alertDialog.dismiss();
            }
        });
        alertDialog.show();

    }

}
