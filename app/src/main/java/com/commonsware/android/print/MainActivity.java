/***
  Copyright (c) 2014 CommonsWare, LLC
  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
  by applicable law or agreed to in writing, software distributed under the
  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
  OF ANY KIND, either express or implied. See the License for the specific
  language governing permissions and limitations under the License.
  
  Covered in detail in the book _The Busy Coder's Guide to Android Development_
    https://commonsware.com/Android
 */

package com.commonsware.android.print;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.print.PrintHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity {
  private static final int IMAGE_REQUEST_ID=1337;
  private EditText prose=null;
  private WebView wv=null;
  private PrintManager mgr=null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    prose=findViewById(R.id.prose);
    mgr=(PrintManager)getSystemService(PRINT_SERVICE);

  }

  public boolean isStoragePermissionGranted() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
              == PackageManager.PERMISSION_GRANTED) {
        Log.v(TAG,"Permission is granted");
        return true;
      } else {

        Log.v(TAG,"Permission is revoked");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        return false;
      }
    }
    else { //permission is automatically granted on sdk<23 upon installation
      Log.v(TAG,"Permission is granted");
      return true;
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
      Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
      //resume tasks needing this permission
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.actions, menu);

    return(super.onCreateOptionsMenu(menu));
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.bitmap:
        Intent i=
            new Intent(Intent.ACTION_GET_CONTENT)
              .addCategory(Intent.CATEGORY_OPENABLE)
              .setType("image/*");

        startActivityForResult(i, IMAGE_REQUEST_ID);

        return(true);

      case R.id.web:
        printWebPage();

        return(true);

      case R.id.report:
        printReport();

        return(true);

      case R.id.pdf:
        print("Test PDF",
              new PdfDocumentAdapter(getApplicationContext()),
              new PrintAttributes.Builder().build());

        return(true);
      case R.id.dowload_pdf:
        if (isStoragePermissionGranted()) {
          new DownloadFile().execute("https://fury-storage-public-default.s3.amazonaws.com/fbm-wms-pdf-creator/wms-dev-pdf/palletsheets/56a7b09f-8d54-45e9-b2d8-31e19e89d1c2.pdf?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIA2W4YYFSFFYUDR6JN%2F20190411%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20190411T132837Z&X-Amz-Expires=43200&X-Amz-Security-Token=AgoJb3JpZ2luX2VjEMX%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLWVhc3QtMSJHMEUCIQDtRwELm91VnyMC2l4Sq0fcXVY0wQzCoK3Sz7%2BCNYbFnAIgXCBMfDJkjwLl3vFHFwZjePag3omDG8mRROnFYGZYS%2Foq4wMIjv%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FARAAGgw3MzYzNzAzNzE3MjIiDKxdvw7YjcpGddpZRiq3A7SHdPyMzBt8LEmumKQgXGqZvMH4pAl3%2BVfBEjxPrMa8aQZCLYCW0Mbi3gCZEGqnd9cYdWpbAqe7n7udm1EAy7fQkoPQBNsl%2Fmw4hszsn2FnkK8rvExcLTq%2B6rn31hn7qAE1um7tJsRRzYpAAwKPXFViCkLa0MeOIWGz4hF%2BQObamJ8eydlfvZUrUL5GdDhOB239pt1YljG7E8lVjE6%2B3T6jAtpzztkhiwzOBgAWrwsIkjqrr81uSsbym3aetHZ3VbEcWTs%2FnlV5IOGHGrx1NHF97wqvqvtNtTA0%2BPJl1DTNHbYRamvmj00WG3oe0x1CSVqaqTx8IsfU2EblpOpGc07BD0lTpu%2FwHwwiIiGYuH0JVGLOxz5BhdCilRdl146SaZBVeiOt9wC7Q7NFqGpNtBhbvMPHz7lFBwq9YsGeHSkZi8OB%2FKfI8qTkbGyMcXz46xsjoqfwzPX3kw1glLoolLMAwEa81cAlVuUiXi8mtYYy7Ulg2K5ZQ6uShGe7Ip217UXE%2B0TV4UNQ2d3CAeao1MetWej3ymmAZ%2FL35cMFxhplb%2FodSET5olC8AFy%2BZnjBLxUpacdHqlowwuW85QU6tAHdoECfM9RyZcjdr%2B4kKlIxwE2uX3u%2F6orA1c8d1Yxj%2F4SRreN0YD5ubeDd4uAuqIzcBp%2F8MKUVsNN%2BQG8TzJz9BfSFzIwu4ma9hfbbLYNB1NmO4kxehxwagnNs9lU2r9wV5Mh5L8ixwHITrZFGilLfCul0ggr64ELHU0%2Bz0Xl%2B61uTnAGRDMjsF48pnlSWOv1PjKg5XvQXI6Hlk8xRvgmH%2FjvYdVkaBFDS7eTPg2WUcJrpLiU%3D&X-Amz-SignedHeaders=host&X-Amz-Signature=32e8d583dfa8f8c904182e7a1a8aa97d532b6791664ed3c1729ed3c13067e1e8", "pallet.pdf");
        }
        return(true);
    }

    return(super.onOptionsItemSelected(item));
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode,
                                  Intent data) {
    if (requestCode == IMAGE_REQUEST_ID
        && resultCode == Activity.RESULT_OK) {
      try {
        PrintHelper help=new PrintHelper(this);

        help.setScaleMode(PrintHelper.SCALE_MODE_FIT);
        help.printBitmap("Photo!", data.getData());
      }
      catch (FileNotFoundException e) {
        Log.e(getClass().getSimpleName(), "Exception printing bitmap",
              e);
      }
    }
  }

  private void printWebPage() {
    WebView print=prepPrintWebView(getString(R.string.web_page));

    print.loadUrl("https://commonsware.com/Android");
  }

  private void printReport() {
    Template tmpl=
        Mustache.compiler().compile(getString(R.string.report_body));
    WebView print=prepPrintWebView(getString(R.string.tps_report));

    print.loadData(tmpl.execute(new TpsReportContext(prose.getText()
                                                          .toString())),
                   "text/html; charset=UTF-8", null);
  }

  private WebView prepPrintWebView(final String name) {
    WebView result=getWebView();

    result.setWebViewClient(new WebViewClient() {
      @Override
      public void onPageFinished(WebView view, String url) {
        print(name, view.createPrintDocumentAdapter(),
              new PrintAttributes.Builder().build());
      }
    });

    return(result);
  }

  private WebView getWebView() {
    if (wv == null) {
      wv=new WebView(this);
    }

    return(wv);
  }

  private PrintJob print(String name, PrintDocumentAdapter adapter,
                         PrintAttributes attrs) {
    startService(new Intent(this, PrintJobMonitorService.class));

    return(mgr.print(name, adapter, attrs));
  }

  private static class TpsReportContext {
    private static final SimpleDateFormat fmt=
        new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    String msg;

    TpsReportContext(String msg) {
      this.msg=msg;
    }

    @SuppressWarnings("unused")
    String getReportDate() {
      return(fmt.format(new Date()));
    }

    @SuppressWarnings("unused")
    String getMessage() {
      return(msg);
    }
  }
}


