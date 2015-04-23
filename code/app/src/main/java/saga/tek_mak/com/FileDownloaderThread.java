package saga.tek_mak.com;

// Code taken and adapted from:
//	http://www.hassanpur.com/blog/2011/04/android-development-downloading-a-file-from-the-web/

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import android.os.Message;

public class FileDownloaderThread extends Thread {
    // constants
    private static final int DOWNLOAD_BUFFER_SIZE = 4096;

    // instance variables
    private MainActivity parentActivity;
    private String downloadUrl;
    private String filepath;
    private long fileSize;
    private boolean sentStartCommandToParent = false;
    private File theFile;

    /**
     * Instantiates a new DownloaderThread object.
     * @param parentActivity Reference to AndroidFileDownloader activity.
     * @param inUrl String representing the URL of the file to be downloaded.
     */
    public FileDownloaderThread(MainActivity thisActivity, String inUrl, String inFilepath, File file)
    {
        this.init(thisActivity, inUrl, inFilepath, file);
    }
    private void init(MainActivity thisActivity, String inUrl, String inFilepath, File file) {
        downloadUrl = "";
        if(inUrl != null)
        {
            downloadUrl = inUrl;
        }
        filepath = inFilepath;
        parentActivity = thisActivity;
        theFile = file;
    }
    public FileDownloaderThread(MainActivity thisActivity, String inUrl, String inFilepath)
    {
        this.init(thisActivity, inUrl, inFilepath);
    }
    public FileDownloaderThread(MainActivity thisActivity, String inUrl, String inFilepath, long inFileSize)
    {
        this.init(thisActivity, inUrl, inFilepath, inFileSize);
    }


    private void init(MainActivity thisActivity, String inUrl, String inFilepath){
        downloadUrl = "";
        if(inUrl != null)
        {
            downloadUrl = inUrl;
        }
        filepath = inFilepath;
        parentActivity = thisActivity;
    }
    private void init(MainActivity thisActivity, String inUrl, String inFilepath, long inFileSize){
        downloadUrl = "";
        if(inUrl != null)
        {
            downloadUrl = inUrl;
        }
        fileSize = inFileSize;
        filepath = inFilepath;
        parentActivity = thisActivity;
    }

    /**
     * Connects to the URL of the file, begins the download, and notifies the
     * AndroidFileDownloader activity of changes in state. Writes the file to
     * the root of the SD card.
     */
    @Override
    public void run()
    {
        URL url;
        URLConnection conn;
        int lastSlash;
        String fileName = "";
        BufferedInputStream inStream;
        BufferedOutputStream outStream;
        File outFile;
        FileOutputStream fileStream;
        Message msg;

        // we're going to connect now
        msg = Message.obtain(parentActivity.activityHandler, Constants.MESSAGE_CONNECTING_STARTED, 0, 0, downloadUrl);
        parentActivity.activityHandler.sendMessage(msg);

        try
        {
            url = new URL(downloadUrl);
            conn = url.openConnection();
            conn.setUseCaches(false);
            if (this.fileSize == 0) this.fileSize = conn.getContentLength();

            // get the filename
            lastSlash = filepath.toString().lastIndexOf('/');
            fileName = "file.bin";
            if(lastSlash >=0)
            {
                fileName = filepath.toString().substring(lastSlash + 1);
            }
            if(fileName.equals(""))
            {
                fileName = "file.bin";
            }

            // notify download start
            long fileSizeInKB = this.fileSize / 1024;
            msg = Message.obtain(parentActivity.activityHandler, Constants.MESSAGE_DOWNLOAD_STARTED, Integer.parseInt(String.valueOf(fileSizeInKB)), 0, fileName);
            parentActivity.activityHandler.sendMessage(msg);

            // start download
            inStream = new BufferedInputStream(conn.getInputStream());

            String theSDCardFilePath = Constants.TRACK_DOWNLOAD_LOCATION + filepath;

            outFile = new File(theSDCardFilePath);

            fileStream = new FileOutputStream(outFile);
            outStream = new BufferedOutputStream(fileStream, DOWNLOAD_BUFFER_SIZE);
            byte[] data = new byte[DOWNLOAD_BUFFER_SIZE];
            int bytesRead = 0, totalRead = 0;
            while(!isInterrupted() && (bytesRead = inStream.read(data, 0, data.length)) >= 0)
            {
                outStream.write(data, 0, bytesRead);

                totalRead += bytesRead;
                int totalReadInKB = totalRead / 1024;
                int percentComplete = (int)fileSizeInKB;
                // update progress
                msg = Message.obtain(parentActivity.activityHandler, Constants.MESSAGE_UPDATE_PROGRESS_BAR, totalReadInKB, percentComplete, 0);
                parentActivity.activityHandler.sendMessage(msg);
            }

            outStream.close();
            fileStream.close();
            inStream.close();

            if(isInterrupted())
            {
                // the download was canceled, so let's delete the partially downloaded file
                outFile.delete();
            }
            else
            {
                // notify completion
                msg = Message.obtain(parentActivity.activityHandler, Constants.MESSAGE_DOWNLOAD_COMPLETE);
                parentActivity.activityHandler.sendMessage(msg);
            }
        }
        catch(MalformedURLException e)
        {
            Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "FileDownloaderThread.run() " + e.getMessage());
            String errMsg = "bad_url: " + downloadUrl;
            msg = Message.obtain(parentActivity.activityHandler,
                    Constants.MESSAGE_ENCOUNTERED_ERROR,
                    0, 0, errMsg);
            parentActivity.activityHandler.sendMessage(msg);
        }
        catch(FileNotFoundException e)
        {
            String errMsg = "file_not_found: " + filepath;
            Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "FileDownloaderThread.run() " + errMsg + "         "+ e.getMessage());
            msg = Message.obtain(parentActivity.activityHandler,
                    Constants.MESSAGE_ENCOUNTERED_ERROR,
                    0, 0, errMsg);
            parentActivity.activityHandler.sendMessage(msg);
        }
        catch(Exception e)
        {
            String errMsg = e.toString();
            Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "FileDownloaderThread.run() " + e.getMessage());
            msg = Message.obtain(parentActivity.activityHandler,
                    Constants.MESSAGE_ENCOUNTERED_ERROR,
                    0, 0, errMsg);
            parentActivity.activityHandler.sendMessage(msg);
        }
    }
}