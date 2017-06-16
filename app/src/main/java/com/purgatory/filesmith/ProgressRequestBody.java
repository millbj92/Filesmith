package com.purgatory.filesmith;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.webkit.MimeTypeMap;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

 class ProgressRequestBody extends RequestBody {
    private File mFile;
    private UploadCallbacks mListener;

    private static final int DEFAULT_BUFFER_SIZE = 2048;

     interface UploadCallbacks {
        void onProgressUpdate(int percentage);
        void onError();
        void onFinish();
    }

     ProgressRequestBody(final File file, final  UploadCallbacks listener) {
        mFile = file;
        mListener = listener;
    }

    @Override
    public MediaType contentType() {
        // i want to upload only images
        String ext = FilenameUtils.getExtension(mFile.getName());
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        return MediaType.parse(mime);
    }

    @Override
    public long contentLength() throws IOException {
        return mFile.length();
    }

    @Override
    public void writeTo(@NonNull  BufferedSink sink) throws IOException {
        long fileLength = mFile.length();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        FileInputStream in = new FileInputStream(mFile);
        long uploaded = 0;

        try {
            int read;
            Handler handler = new Handler(Looper.getMainLooper());
            while ((read = in.read(buffer)) != -1) {

                // update progress on UI thread
                handler.post(new ProgressUpdater(uploaded, fileLength));

                uploaded += read;
                sink.write(buffer, 0, read);
            }
        } finally {
            in.close();
        }
    }

    private class ProgressUpdater implements Runnable {
        private long mUploaded;
        private long mTotal;
         ProgressUpdater(long uploaded, long total) {
            mUploaded = uploaded;
            mTotal = total;
        }

        @Override
        public void run() {
            mListener.onProgressUpdate((int)(100 * mUploaded / mTotal));
        }
    }
}
