package de.hosenhasser.funktrainer;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class LichtblickeViewerActivity extends Activity implements OnLoadCompleteListener, OnPageChangeListener{

    public static final String LICHTBLICKE_A_FILE = "Lichtblick-A.pdf";
    public static final String LICHTBLICKE_A_FILE_ZIP = "LichtblickA.zip";
    public static final String LICHTBLICKE_A_URL = "http://www.dl9hcg.a36.de/download/LichtblickA.zip";

    private SharedPreferences mPrefs;

    PDFView pdfView;

    int nbPages = 0;
    int pageToShow = 0;

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    protected void onPause() {
        super.onPause();
        if(pdfView != null) {
            SharedPreferences.Editor ed = mPrefs.edit();
            ed.putInt("last_page_shown", pdfView.getCurrentPage());
            ed.apply();
        }
    }

    protected void showDownloadProgress(boolean show) {
        final LinearLayout progressLayout = findViewById(R.id.lichtblickDownloadProgress);
        final Button downloadButton = findViewById(R.id.lichtblickADownloadButton);
        if (show) {
            progressLayout.setVisibility(View.VISIBLE);
            downloadButton.setEnabled(false);
        } else {
            progressLayout.setVisibility(View.INVISIBLE);
            downloadButton.setEnabled(true);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = getSharedPreferences("advanced_question_asker_shared_preferences", MODE_PRIVATE);

        // already downloaded lichtblicke a?
        boolean lichtblicke_a_downloaded = mPrefs.getBoolean("lichtblicke_a_downloaded", false);
        try {
            FileInputStream fis = openFileInput(LICHTBLICKE_A_FILE);
        } catch (FileNotFoundException e) {
            lichtblicke_a_downloaded = false;
        }

        if (!lichtblicke_a_downloaded) {
            setContentView(R.layout.activity_lichtblicke_downloader);
            final TextView downloadLinkTextView = findViewById(R.id.lichtblickeDownloaderLinkTextView);
            downloadLinkTextView.setMovementMethod(LinkMovementMethod.getInstance());
            final Button downloadButton = findViewById(R.id.lichtblickADownloadButton);
            downloadButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    downloadButton.setEnabled(false);
                    new DownloadLichtblickFileAsync(LichtblickeViewerActivity.this).execute(LICHTBLICKE_A_URL);
                }
            });
        } else {
            File lichtblick_a_file = getBaseContext().getFileStreamPath(LICHTBLICKE_A_FILE);

            setContentView(R.layout.activity_lichtblicke_viewer);
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                // Show the Up button in the action bar.
                actionBar.setDisplayHomeAsUpEnabled(true);
            }

            this.pageToShow = getIntent().getExtras().getInt(getClass().getName() + ".lichtblickPage", 0);

            if (this.pageToShow <= 0) {
                this.pageToShow = mPrefs.getInt("last_page_shown", 0);
            }

            pdfView = findViewById(R.id.pdfview);
            pdfView.fromFile(lichtblick_a_file)
                    .defaultPage(this.pageToShow)
                    .enableSwipe(true)
                    .enableDoubletap(true)
                    .onLoad(this)
                    .onPageChange(this)
                    .load();
        }
    }

    public void loadComplete(int nbPages) {
        this.nbPages = nbPages;
        this.pageToShow = max(min(nbPages, this.pageToShow), 0);
    }

    public void onPageChanged(int page, int pageCount) {

    }

    class DownloadLichtblickFileAsync extends AsyncTask<String, Integer, String> {
        private Context mContext;

        DownloadLichtblickFileAsync(Context context) {
            this.mContext = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDownloadProgress(true);
        }

        @Override
        protected String doInBackground(String... urls) {
            int count;
            HttpURLConnection connection = null;
            InputStream input = null;
            FileOutputStream output = null;

            try {
                URL url = new URL(urls[0]);
                connection = (HttpURLConnection)url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                int lenghtOfFile = connection.getContentLength();
//                Log.d("ANDRO_ASYNC", "Lenght of file: " + lenghtOfFile);

                input = new BufferedInputStream(url.openStream());
//                OutputStream output = new FileOutputStream("/sdcard/some_photo_from_gdansk_poland.jpg");
                output = this.mContext.openFileOutput(LICHTBLICKE_A_FILE_ZIP, Context.MODE_PRIVATE);

                byte data[] = new byte[4096];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    if (lenghtOfFile > 0) {
                        publishProgress((int) ((total * 100) / lenghtOfFile));
                    }
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null) {
                        output.flush();
                        output.close();
                    }
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException ignored) {

                }
                if (connection != null) {
                    connection.disconnect();
                }
                // unzip
                try {
                    FileInputStream fin = this.mContext.openFileInput(LICHTBLICKE_A_FILE_ZIP);
                    FileOutputStream fout = this.mContext.openFileOutput(LICHTBLICKE_A_FILE, Context.MODE_PRIVATE);
                    ZipInputStream zin = new ZipInputStream(fin);
                    ZipEntry ze;
                    boolean decompressed_something = false;
                    while ((ze = zin.getNextEntry()) != null) {
                        if (!ze.isDirectory()) {
                            final String n = ze.getName();
                            if (ze.getName().equals(LICHTBLICKE_A_FILE)) {
                                decompressed_something = true;
                                BufferedOutputStream out = new BufferedOutputStream(fout);

                                byte[] buffer = new byte[4096];
                                int read = 0;
                                while ((read = zin.read(buffer)) != -1) {
                                    out.write(buffer, 0, read);
                                }

                                out.close();
                                zin.closeEntry();
                                fout.close();
                                zin.close();
                                fin.close();
                                try {
                                    this.mContext.deleteFile(LICHTBLICKE_A_FILE_ZIP);
                                } catch (Exception e) {

                                }
                                break;
                            }
                        }
                    }
                    if (!decompressed_something) {
                        return LICHTBLICKE_A_FILE + " not found in zipfile.";
                    }
                } catch (FileNotFoundException e) {
                    return "zipfile not found: " + e.toString();
                } catch (IOException e) {
                    return "zipfile IOException: " + e.toString();
                }
            }
            return null;
        }

        protected void onProgressUpdate(Integer... progress) {
            Log.d("Funktrainer", "DownloadLichtblickFileAsync: " + progress[0]);
            final ProgressBar pbar = findViewById(R.id.lichtblickeDownloaderProgressBar);
            pbar.setIndeterminate(false);
            pbar.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d("Funktrainer", "DownloadLichtblickFileAsync Result: " + result);

            if (result != null) {
                final String lichtblickDownloadToastText = this.mContext.getString(R.string.lichtblickeDownloaderToastTextFailure);
                Toast.makeText(this.mContext, lichtblickDownloadToastText + result, Toast.LENGTH_LONG).show();
                showDownloadProgress(false);
            } else {
                final String lichtblickDownloadToastText = this.mContext.getString(R.string.lichtblickeDownloaderToastTextSuccess);
                Toast.makeText(this.mContext, lichtblickDownloadToastText, Toast.LENGTH_LONG).show();
                showDownloadProgress(false);
                mPrefs = getSharedPreferences("advanced_question_asker_shared_preferences", MODE_PRIVATE);
                SharedPreferences.Editor ed = mPrefs.edit();
                ed.putBoolean("lichtblicke_a_downloaded", true);
                ed.apply();
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        }
    }
}
