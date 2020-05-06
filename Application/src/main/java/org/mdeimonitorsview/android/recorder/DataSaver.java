package org.mdeimonitorsview.android.recorder;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.mdeimonitorsview.android.recorder.App.OCR_FILES_DIR;
import static org.mdeimonitorsview.android.recorder.App.staticLogger;


public class DataSaver {
    private static final DataSaver ourInstance = new DataSaver();

    private File dirPath;

    private DataSaver() {
        generateDataFolder();
    }

    public static DataSaver getInstance() {
        return ourInstance;
    }

    public static void saveImage(final Bitmap bmp, final File file) {
        try (FileOutputStream out = new FileOutputStream(file)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 1, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (IOException e) {
            staticLogger.error("can not save file: " + file.getAbsolutePath(), e);
        }
    }

    public void generateDataFolder() {
        String folderTimed = new SimpleDateFormat("yyy_MM_dd_HH").format(new Date());
        dirPath = new File(OCR_FILES_DIR + "/" + folderTimed + "/");
    }

    void save(Bitmap image, int imageId) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Log.d("dataSaver", String.format("saving to %s/%d", dirPath.getAbsolutePath(), imageId));
                saveImage(image, new File(dirPath, String.valueOf(imageId) + ".png"));
            }
        });
    }

    public void save(String string, int imageId) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("dataSaver", String.format("saving to %s/%d", dirPath.getAbsolutePath(), imageId));
                    FileUtils.writeStringToFile(new File(dirPath, String.valueOf(imageId) + ".json"), string, (String) null);
                } catch (IOException e) {
                    staticLogger.error(String.format("error saving json file for image id: %d", imageId), e);
                }
            }
        });
    }

}