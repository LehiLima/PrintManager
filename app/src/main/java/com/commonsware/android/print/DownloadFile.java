package com.commonsware.android.print;


import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;

class DownloadFile extends AsyncTask<String, Void, Void> {

    @Override
    protected Void doInBackground(String... strings) {
        String fileUrl = strings[0];   // -> http://maven.apache.org/maven-1.x/maven.pdf
        String fileName = strings[1];  // -> maven.pdf


        String extStorageDirectory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
        File CheckDirectory;
        CheckDirectory = new File(extStorageDirectory,"pdf");
        if (!CheckDirectory.exists()){
            CheckDirectory.mkdirs();
        }

        File pdfFile = new File(CheckDirectory, fileName);

        try{
            pdfFile.createNewFile();
        }catch (IOException e){
            e.printStackTrace();
        }
        FileDownloader.downloadFile(fileUrl, pdfFile);
        return null;
    }
}

