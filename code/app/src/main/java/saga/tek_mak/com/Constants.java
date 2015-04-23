package saga.tek_mak.com;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Queue;

import org.joda.time.DateTime;

import android.graphics.Color;
import android.util.Log;
import android.view.Menu;

public class Constants {

    // This is the default user
    public static String USER_CODE = Private.USER_CODE_2;

    // max number of tracks to queue
    public static final int TRACK_QUEUE_MAX_CAPACITY = 5;

    // time, in milliseconds, between runs of the track updater
    public static final long TRACK_UPDATER_TIME_INTERVAL = 1000;

    // the maximum number of allowed requests to be opened at one time.
    public static int OPEN_TRACK_REQUEST_MAX_LIMIT = 2;




    public static final String TRACK_DOWNLOAD_LOCATION = Private.TRACK_DOWNLOAD_LOCATION;

    public static final String USER_DISPLAY_1 = Private.USER_NAME_1;
    public static final String USER_DISPLAY_2 = Private.USER_NAME_2;
    public static final String USER_DISPLAY_3 = Private.USER_NAME_3;
    public static final String USER_DISPLAY_4 = Private.USER_NAME_4;


    public static final int MENU_ITEM_CHANGE_TO_USER_1 = Menu.FIRST + 1;
    public static final int MENU_ITEM_CHANGE_TO_USER_2 = Menu.FIRST + 2;
    public static final int MENU_ITEM_CHANGE_TO_USER_3 = Menu.FIRST + 3;
    public static final int MENU_ITEM_CHANGE_TO_USER_4 = Menu.FIRST + 4;
    public static final int MENU_ITEM_SEARCH = Menu.FIRST + 8;
    public static final int MENU_ITEM_CURRENT_SETTINGS = Menu.FIRST + 9;
    public static final int MENU_ITEM_DARWIN_PERCENTAGE = Menu.FIRST + 10;

    public static final String USER_1_CODE = Private.USER_CODE_1;
    public static final String USER_2_CODE = Private.USER_CODE_2;
    public static final String USER_3_CODE = Private.USER_CODE_3;
    public static final String USER_4_CODE = Private.USER_CODE_4;

    public static boolean FILE_DOWNLOAD_PROGRESS_BAR_ENABLED = false;

    public static final String URL_OF_API = Private.URL_OF_API___PRIVATE;

    public static final String ROOT_OF_EXTERNAL_FILE_LOCATION = Private.ROOT_OF_EXTERNAL_LOCATION___PRIVATE;

    public static final boolean TRACK_UPDATER_TASK_IS_RUNNING = true;

    public static final String LOG_CAT = "Saga.Music";

    public static final String PREPEND_TO_ERROR_LOG = "!!!!!!!!!!  ";
    public static final String PREPEND_REPLY_FROM_HOUSE = "house says ==========>  ";

    public static final int MESSAGE_DOWNLOAD_STARTED = 1000;
    public static final int MESSAGE_DOWNLOAD_COMPLETE = 1001;
    public static final int MESSAGE_UPDATE_PROGRESS_BAR = 1002;
    public static final int MESSAGE_DOWNLOAD_CANCELED = 1003;
    public static final int MESSAGE_CONNECTING_STARTED = 1004;
    public static final int MESSAGE_ENCOUNTERED_ERROR = 1005;
    public static final int FILE_DOWNLOAD_PERCENTAGE = 10;

    public static final int PartBackGroundColor = Color.rgb(25, 0, 51);
    public static final int ArtistBackGroundColor = Color.rgb(64, 4, 15);
    public static final int GenreBackGroundColor = Color.rgb(11, 77, 4);


    // SQLite
    public static DataHelper Database;

    // if we're downloading a track, it's media id is here.
    //	-1 means no track is downloading.
    public static int DownloadingTrackMediaId = -1;

    // are we waiting on a download before starting the player?
    public static boolean IsWaitingOnDownloadBeforePlayingAudio = false;

    // are we currently downloading anything?
    public static boolean IsDownloading = false;

    // are we currently prefetching anything?
    public static boolean IsPrefetching = false;

    // a queue of tracks to be played in the player.
    public static Queue<Track> TrackQueue = null;

    // used for downloading files to the phone
    public static Thread DownloaderThread;

    // if we want to queue the track we're downloading, the track's media id will be here.
    //	-1 means we aren't waiting on a download to queue anything.
    public static int IsWaitingOnDownloadBeforeQueuingAudio = -1;

    // tells if the user is currently touching the seek bar.
    //	used to tell the onProgressChanged event to not do any seeking when the updater adjusts the seekbar's progress
    public static boolean SeekBarIsBeingTouched;

    // used to hold the duration of the track's file, which is being set during the onPrepared listener
    public static int TrackFileDuration;

    public static boolean IsWaitingForTrackQueueToFill = false;

    // the track that is currently loaded in the player (MediaPlayer)
    public static Track TrackCurrentlyLoadedInPlayer;

    // the number of currently open requests to the house to get a track for the track queue.
    public static int NumberOfTrackQueueRequests = 0;


    // write to a log file on the device.
    public static void WriteToFile(String data) {
        try {
            Log.e(Constants.LOG_CAT, data);

            String path = Constants.TRACK_DOWNLOAD_LOCATION + "/saga.txt";

            File file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            String timeStamp = GetTimeStamp();
            bw.write(timeStamp + "  :: " + data);
            bw.newLine();
            bw.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private static String GetTimeStamp(){
        String timeStamp = DateTime.now().toString("MM/dd/yyyy hh:mm ss a");
        return timeStamp;
    }

}
