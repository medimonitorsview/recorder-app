package org.mdeimonitorsview.android.recorder;

import android.graphics.Bitmap;
import android.media.Image;
import android.os.Handler;
import android.util.Log;

import org.mdeimonitorsview.android.recorder.classes.Segment;
import org.mdeimonitorsview.android.recorder.publishers.ImagePublisher;
import org.mdeimonitorsview.android.recorder.publishers.MonitorData;
import org.mdeimonitorsview.android.recorder.publishers.OcrPublisher;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static org.mdeimonitorsview.android.recorder.App.OCR_FILES_DIR;
import static org.mdeimonitorsview.android.recorder.App.staticLogger;

public class ImageManager implements Runnable {

    private final Handler backgroundHandler;
    private final int imageId;
    private final ResponseDataSharingListener responseDataSharingListener;
    private int segmentsImageId;
    private final String monitorId;
    private final String baseUrl;
    private final UiHandler uiHandler;
    //    private final CameraActivity activity;
    private Image image;
    private ImageTreatment imageTreatment;
    SegmentsSyncer.MonitorInformation monitorInformation;

    private void saveData() {
        String folderTimed =  new SimpleDateFormat("yyy_MM_dd_HH").format(new Date());
        File folder = new File(OCR_FILES_DIR + "/" + folderTimed + "/");

        // create the directory named like the image id
        try{
            folder.mkdirs();
//            saveImage(bitmap, new File(folder, Integer.toString(imageId) + ".jpg"));
            File file  = new File(folder, Integer.toString(imageId) + ".jpg");
        } catch (Exception e) {
            staticLogger.error("error while trying to save ocr log for image id" + imageId);
        }

//        ImageTreatment.saveImage();

    }

    ImageManager(Image image, ImageTreatment imageTreatment, Handler backgroundHandler,
                 int imageId, String monitorId, String baseUrl, SegmentsSyncer.MonitorInformation monitorInformation, UiHandler uiHandler, ResponseDataSharingListener responseDataSharingListener) {
        this.image = image;
        this.imageTreatment = imageTreatment;
        this.backgroundHandler = backgroundHandler;
        this.imageId = imageId;
        this.monitorId = monitorId;
        this.baseUrl = baseUrl;
        this.responseDataSharingListener = responseDataSharingListener;
        this.monitorInformation = monitorInformation;
        this.uiHandler = uiHandler;
    }


    @Override
    public void run() {
        byte[] bytes = imageTreatment.convertImageToByteArray(image);
        image.close();



        if (bytes == null) {
            staticLogger.info("did not take picture");
            return;
        }

        long timestamp = System.currentTimeMillis();
        Log.d("imageid", "run: " + imageId);
        uiHandler.showToast("sending " + imageId);

        boolean sendImage = true;
        Bitmap bitmap = imageTreatment.convertByteArrayToBitmap(bytes);
        if (imageTreatment.isOperational()) {

//            if (CameraActivity.getNumberOfImagesPerSaving() != null && imageId % CameraActivity.getNumberOfImagesPerSaving() == 0) {
//                imageTreatment.setLogOcrImageId(imageId);
//            }

            int ocrLogImageId = (CameraActivity.getNumberOfImagesPerSaving() != null && imageId % CameraActivity.getNumberOfImagesPerSaving() == 0) ? imageId : 0;

            ArrayList<Segment> segments = imageTreatment.getAllMeasurement(bitmap, monitorInformation, imageId);

            if (segments != null) {
                MonitorData monitorData = new MonitorData(segments, timestamp);
//                FileUtils.writeStringToFile(OcrPublisher.gson.toJson(monitorData), );
//                backgroundHandler.post(new OcrPublisher(monitorData, imageId, monitorId, baseUrl, uiHandler));
                try {
                    OcrPublisher ocrpub = new OcrPublisher(monitorData, imageId, monitorId, baseUrl, uiHandler, responseDataSharingListener);
                    ocrpub.run();
                    sendImage = ocrpub.sendNextImage;
                }
                catch (Exception ex){
                    staticLogger.warn("sending ocr failed" + ex.getMessage());
                }

            }

            // It is important to save the data after the ocr publisher run
            // Because the ocr json is set in the publisher's run
            DataSaver.getInstance().generateDataFolder();

//            if (CameraActivity.getNumberOfImagesPerSaving() != null && imageId % CameraActivity.getNumberOfImagesPerSaving() == 0) {
//                imageTreatment.setLogOcrImageId(ImageTreatment.DOT_LOG_OCR);
//            }

        } else {
            uiHandler.showToast("ocr is not supported");
            staticLogger.info("ocr is not supported");
        }
        try {
            if (bitmap != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                bytes = stream.toByteArray();
            }
            new ImagePublisher(bytes, sendImage, imageId, monitorId, timestamp, baseUrl, uiHandler).run();
        }
        catch (Exception ex){
            staticLogger.warn("sending image failed" + ex.getMessage());
        }
//        backgroundHandler.post(new ImagePublisher(bytes, imageId, monitorId, timestamp, baseUrl, uiHandler));
    }
}
