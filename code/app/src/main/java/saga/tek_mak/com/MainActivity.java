package saga.tek_mak.com;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import org.json.JSONException;
import org.json.JSONObject;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.app.ProgressDialog;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements OnCompletionListener, OnPreparedListener, OnSeekBarChangeListener {
    private static int SEARCH_ACTIVITY__TERMINATED_CODE = 0;

    private MainActivity thisActivity;
    private Button btnPausePlay = null;
    private Button btnNextTrack = null;
    private Button btnStopTrackQuitApp = null; 	// if the app is playing a track, the track is stopped.
                                                //		else, it quits the app
    private SeekBar trackSeekBar; 				// seekbar for the currently loaded track
    public MediaPlayer player = null;
    private Handler trackHandler = new Handler();
    private ProgressDialog progressDialog;		// used for tracking file downloads
    private int CurrentTrackSecondsPlayed = 0;	// holds the number of seconds the currently loaded track has played.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        thisActivity = this;
        Init();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        InitMenu(menu);
        return (super.onCreateOptionsMenu(menu));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        OnOptionsItemSelected(item);
        return (super.onOptionsItemSelected(item));
    }


    class QueueTrackTask extends AsyncTask<String, String, Track> {
        @Override
        protected Track doInBackground(String... uri) {
            if (Constants.OPEN_TRACK_REQUEST_MAX_LIMIT <= Constants.NumberOfTrackQueueRequests){
                return null;
            }

            Constants.NumberOfTrackQueueRequests++;

            String sUri = Constants.URL_OF_API + "api/nexttrack/?code=" + Constants.USER_CODE;
            String jsonTrack = MyHttpClient.ReturnJsonFromUrl(sUri);
            if (jsonTrack == null){
                return null;
            }
            JSONObject jTrack;
            int mediaIdReceivedFromHouse = 0;
            String mediaFilePathReceivedFromHouse = "";
            String titleReceivedFromTheHouse = "";
            try {
                jTrack = new JSONObject(jsonTrack);

                mediaIdReceivedFromHouse = jTrack.getInt("MediaId");
                if (mediaIdReceivedFromHouse == -1){
                    Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "mediaIdReceivedFromHouse == -1");
                    Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + jTrack.getString("Title"));
                    Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + jTrack.getString("MediaFilePath"));
                    return null;
                }

                mediaFilePathReceivedFromHouse = jTrack.getString("MediaFilePath");
                mediaFilePathReceivedFromHouse = mediaFilePathReceivedFromHouse.toLowerCase().replace(Constants.ROOT_OF_EXTERNAL_FILE_LOCATION, "");
                mediaFilePathReceivedFromHouse = mediaFilePathReceivedFromHouse.replace("\\", "/");

                titleReceivedFromTheHouse = jTrack.getString("Title");
            } catch (JSONException e) {
                e.printStackTrace();
                Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "MainActivity.QueueTrackTask received from house .... MediaId = " + mediaIdReceivedFromHouse + ", FilePath = " + mediaFilePathReceivedFromHouse + ", Title = " + titleReceivedFromTheHouse);
                Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + sUri);
                return null;
            }

            Track track = Constants.Database.trackGet(mediaIdReceivedFromHouse);
            if (track.mediaId < 1) {
                Constants.Database.trackInsert(Integer.toString(mediaIdReceivedFromHouse), titleReceivedFromTheHouse, mediaFilePathReceivedFromHouse);
                if (this.DownloadTrack(mediaIdReceivedFromHouse, mediaFilePathReceivedFromHouse)) {
                    Constants.IsWaitingOnDownloadBeforeQueuingAudio = mediaIdReceivedFromHouse;
                }
                return null;
            } else {
                if (track.mediaId > 0 && track.fileDownloaded) {
                    return track;
                } else {
                    if (this.DownloadTrack(mediaIdReceivedFromHouse, mediaFilePathReceivedFromHouse)) {
                        Constants.IsWaitingOnDownloadBeforeQueuingAudio = mediaIdReceivedFromHouse;
                    }
                    return null;
                }
            }
        }

        @Override
        protected void onPostExecute(Track track) {
            super.onPostExecute(track);
            Constants.NumberOfTrackQueueRequests--;
            if (Constants.NumberOfTrackQueueRequests < 0){
                Constants.NumberOfTrackQueueRequests = 0;
            }
            if (track != null){
                this.trackQueueAdd(track);
            }
        }

        private void trackQueueAdd(Track track) {
            for (Track queuedTrack : Constants.TrackQueue) {
                if (queuedTrack.mediaId == track.mediaId) {
                    return;
                }
            }
            Constants.TrackQueue.add(track);
        }

        // return true if you started a download
        // return false if you are not going to download
        private boolean DownloadTrack(int mediaId, String filePath) {
            if (Constants.IsDownloading) {
                return false;
            }
            Constants.IsDownloading = true;
            Constants.DownloadingTrackMediaId = mediaId;

            Constants.DownloaderThread = new FileDownloaderThread(thisActivity,
                    Constants.URL_OF_API + "api/track/" + mediaId + "?code=" + Constants.USER_CODE,
                    filePath
            );
            Constants.DownloaderThread.start();
            return true;
        }
    }
    // END QUEUE TRACK ASYNCTASK ============================


    private Runnable updateTrackTask = new Runnable() {
        public void run() {
            int qSize = thisActivity.queueSizeGet();
            String trackTitle = "";
            if (Constants.TrackCurrentlyLoadedInPlayer != null){
                trackTitle = Constants.TrackCurrentlyLoadedInPlayer.title;
            } else {
                trackTitle = "no track loaded";
            }
            if (qSize == 0){
                if (thisActivity.player == null){
                    thisActivity.btnPausePlay.setEnabled(false);
                } else {
                    thisActivity.btnPausePlay.setEnabled(true);
                }
                thisActivity.btnNextTrack.setEnabled(false);
                thisActivity.btnNextTrack.setText(trackTitle);
            } else {
                thisActivity.btnPausePlay.setEnabled(true);
                thisActivity.btnNextTrack.setEnabled(true);
                thisActivity.btnNextTrack.setText(trackTitle);
            }
            if (qSize < Constants.TRACK_QUEUE_MAX_CAPACITY) {
                new QueueTrackTask().execute("");
            }

            // set the pause play button text
            String msg = "";
            msg = "Q " + thisActivity.queueSizeGet() + "    ";
            if (thisActivity.player != null){
                if (thisActivity.player.isPlaying()){
                    msg += "    || ";
                } else {
                    msg += "     > ";
                }
            }
            thisActivity.btnPausePlay.setText(msg);

            // update the Track Timer.
            if (thisActivity.player != null){
                if (thisActivity.player.isPlaying()){
                    ++thisActivity.CurrentTrackSecondsPlayed;
                }
            }

            if (Constants.IsWaitingForTrackQueueToFill && thisActivity.queueSizeGet() > 0) {
                thisActivity.StartPlaying();
            }

            // if there's no track loaded, leave.
            if (Constants.TrackCurrentlyLoadedInPlayer == null || player == null) {
                thisActivity.trackHandler.postDelayed(this, Constants.TRACK_UPDATER_TIME_INTERVAL);
                return;
            }

            int playerDuration = thisActivity.getTrackFileDuration();
            int currentPosition = thisActivity.player.getCurrentPosition();


            // update the track's seek bar
            if (playerDuration > 0) {
                thisActivity.trackSeekBar.setMax((int) playerDuration);
                thisActivity.trackSeekBar.setProgress(currentPosition);
            }

            thisActivity.trackHandler.postDelayed(this, Constants.TRACK_UPDATER_TIME_INTERVAL);
        }
    };



    // MEDIA PLAYER EVENTS
    public void onCompletion(MediaPlayer arg0) {
        if (arg0.getDuration() == 1108672036){
            Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "bad looking duration. releasing player");
            this.player.release();
        }
        TellHouseTrackCompletelyPlayed();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        OnPlayNextTrack(true);
    }


    public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
        return false;
    }

    public void onPrepared(MediaPlayer arg0) {
        Constants.TrackFileDuration = this.player.getDuration();
    }

    // TRACK SEEKBAR EVENTS
    public void onProgressChanged(SeekBar seekBar, int progress, boolean arg2) {
        if (!Business.IsPlaying(this.player))
            return;
        if (!Constants.SeekBarIsBeingTouched)
            return;
        this.player.seekTo(progress);
    }

    public void onStartTrackingTouch(SeekBar arg0) {
        Constants.SeekBarIsBeingTouched = true;
        this.trackHandler.removeCallbacks(this.updateTrackTask);
    }

    public void onStopTrackingTouch(SeekBar arg0) {
        Constants.SeekBarIsBeingTouched = false;
        this.trackHandler.postDelayed(this.updateTrackTask, Constants.TRACK_UPDATER_TIME_INTERVAL);
    }
    // end TRACK SEEKBAR EVENTS


    //This is the Handler for this activity. It will receive messages from the DownloaderThread and make the necessary updates to the UI.
    public Handler activityHandler = new Handler() {
        public void handleMessage(Message msg) {
            HandleFileDownloaderThreadMessage(msg);
        }

    };








    // METHODS FOR THE MAIN ACTIVITY -----------------------------------------------------------------------
    private void HandleFileDownloaderThreadMessage(Message msg){
        switch (msg.what) {
            case Constants.MESSAGE_UPDATE_PROGRESS_BAR:
                final int currentProgress = msg.arg1;
                final int totalFileSize = msg.arg2;
                btnPausePlay.post(new Runnable() {
                    public void run() {
                        btnStopTrackQuitApp.setText(Integer.toString(currentProgress) + "     of    " + totalFileSize + "        stop");
                    }
                });

                if (!Constants.FILE_DOWNLOAD_PROGRESS_BAR_ENABLED)
                    return;

                if (progressDialog != null) {
                    progressDialog.setProgress(currentProgress);
                }
                break;

            case Constants.MESSAGE_CONNECTING_STARTED:
                // turn off the progress bar if we already have a track ready to play.
                if (thisActivity.player != null) {
                    Constants.FILE_DOWNLOAD_PROGRESS_BAR_ENABLED = false;
                    return;
                }
                if (!Constants.FILE_DOWNLOAD_PROGRESS_BAR_ENABLED) {
                    return;
                }

                if (msg.obj != null && msg.obj instanceof String) {
                    String url = (String) msg.obj;
                    // truncate the url
                    if (url.length() > 16) {
                        String tUrl = url.substring(0, 15);
                        tUrl += "...";
                        url = tUrl;
                    }
                    String pdTitle = "connecting";
                    String pdMsg = "connecting to ";
                    pdMsg += " " + url;

                    dismissCurrentProgressDialog();
                    progressDialog = new ProgressDialog(thisActivity);
                    progressDialog.setTitle(pdTitle);
                    progressDialog.setMessage(pdMsg);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progressDialog.setIndeterminate(true);
                    Message newMsg = Message.obtain(this.trackHandler, Constants.MESSAGE_DOWNLOAD_CANCELED);
                    progressDialog.setCancelMessage(newMsg);
                    progressDialog.show();
                }
                break;

            case Constants.MESSAGE_DOWNLOAD_STARTED:
                if (!Constants.FILE_DOWNLOAD_PROGRESS_BAR_ENABLED) {
                    return;
                }

                if (msg.obj != null && msg.obj instanceof String) {
                    int maxValue = msg.arg1;

                    String fileName = (String) msg.obj;
                    String pdTitle = "downloading";
                    String pdMsg = fileName.replaceAll("%20", " ");

                    dismissCurrentProgressDialog();

                    progressDialog = new ProgressDialog(thisActivity);
                    progressDialog.setTitle(pdTitle);
                    progressDialog.setMessage(pdMsg);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setProgress(0);
                    progressDialog.setMax(maxValue);
                    Message newMsg = Message.obtain(this.trackHandler, Constants.MESSAGE_DOWNLOAD_CANCELED);
                    progressDialog.setCancelMessage(newMsg);
                    progressDialog.setCancelable(true);
                    progressDialog.show();
                }
                break;

            case Constants.MESSAGE_DOWNLOAD_COMPLETE:
                Constants.IsDownloading = false;
                Constants.IsPrefetching = false;

                btnPausePlay.post(new Runnable() {
                    public void run() {
                        btnStopTrackQuitApp.setText("stop");
                    }
                });

                // Update that the track's file was fully downloaded.
                trackFlagFileDownloaded();
                setPausePlayButtonTextAfterDownloadCompleted();

                if (Constants.FILE_DOWNLOAD_PROGRESS_BAR_ENABLED) {
                    dismissCurrentProgressDialog();
                    displayMessage("download complete");
                }

                // if we're waiting on a download before queuing a track, queue it.
                int isWaitingOnDownloadBeforeQueuingAudio = Constants.IsWaitingOnDownloadBeforeQueuingAudio;
                if (isWaitingOnDownloadBeforeQueuingAudio > 0) {
                    Track track = Constants.Database.trackGet(isWaitingOnDownloadBeforeQueuingAudio);
                    thisActivity.queueTrackAfterDownloadingIt(track);
                    Constants.IsWaitingOnDownloadBeforeQueuingAudio = -1;
                    isWaitingOnDownloadBeforeQueuingAudio = Constants.IsWaitingOnDownloadBeforeQueuingAudio;
                }
                // if we're waiting for an audio track to download, start playing it.
                if (Constants.IsWaitingOnDownloadBeforePlayingAudio) {
                    Constants.IsWaitingOnDownloadBeforePlayingAudio = false;
                }
                break;

            case Constants.MESSAGE_DOWNLOAD_CANCELED:
                if (Constants.DownloaderThread != null) {
                    Constants.DownloaderThread.interrupt();
                }
                btnPausePlay.post(new Runnable() {
                    public void run() {
                        btnStopTrackQuitApp.setText("stop");
                    }
                });
                Constants.IsDownloading = false;
                if (Constants.FILE_DOWNLOAD_PROGRESS_BAR_ENABLED) {
                    dismissCurrentProgressDialog();
                    displayMessage("download canceled");
                }

                Constants.IsWaitingOnDownloadBeforePlayingAudio = false;
                break;

            case Constants.MESSAGE_ENCOUNTERED_ERROR:
                // obj will contain a string representing the error message
                String errorMessage = "";
                if (msg.obj != null && msg.obj instanceof String) {
                    errorMessage = (String) msg.obj;
                }
                if (Constants.FILE_DOWNLOAD_PROGRESS_BAR_ENABLED) {
                    dismissCurrentProgressDialog();
                }
                Constants.IsDownloading = false;
                btnPausePlay.post(new Runnable() {
                    public void run() {
                        btnStopTrackQuitApp.setText("stop");
                    }
                });

                Constants.DownloadingTrackMediaId = -1;
                Constants.IsWaitingOnDownloadBeforePlayingAudio = false;
                Constants.IsWaitingOnDownloadBeforeQueuingAudio = -1;

                // check to see if we need to free up some disk space
                if (errorMessage.toLowerCase().contains("no space left on device")) {
                    // delete a few track files from the device
                    new Thread(new Runnable() {
                        public void run() {
                            Business.FreeUpDiskSpace(Constants.Database);
                        }
                    }).start();
                    new Thread(new Runnable() {
                        public void run() {
                            Business.FreeUpDiskSpace(Constants.Database);
                        }
                    }).start();
                }

                displayMessage(errorMessage);
                break;

            default:
                break;
        }
    }




    private void trackFlagFileDownloaded() {
        Constants.Database.trackFlagFileDownloaded(Constants.DownloadingTrackMediaId);
        Constants.DownloadingTrackMediaId = -1;
    }

    private void setPausePlayButtonTextAfterDownloadCompleted() {
        if (player == null) {
            btnPausePlay.setText(" ");
        } else if (player.isPlaying()) {
            btnPausePlay.setText(R.string.button_pause_text);
        } else {
            btnPausePlay.setText(R.string.button_play_text);
        }
    }

    public void displayMessage(String message) {
        if (message != null) {
            Toast.makeText(thisActivity, message, Toast.LENGTH_SHORT).show();
        }
    }

    //If there is a progress dialog, dismiss it and set progressDialog to null.
    public void dismissCurrentProgressDialog() {
        if (progressDialog != null) {
            progressDialog.hide();
            progressDialog.dismiss();
            progressDialog = null;
        }
    }



    private void OnPlayNextTrack(boolean start) {
        if (start) {
            Business.StopPlaying(player);
            StartPlaying();
        } else {
            Business.StopPlaying(player);
        }
    }



    private void StartPlaying() {
        // if the player is already playing, leave it alone.
        if (Business.IsPlaying(player)) {
            Constants.WriteToFile("i went to start playing, but the player was already playing. leaving....");
            return;
        }

        // if nothing's in the queue, wait
        if (Constants.TrackQueue.isEmpty()) {
            Constants.WriteToFile("i went to start playing, but the queue is empty. leaving....");
            Constants.IsWaitingForTrackQueueToFill = true;
            return;
        } else {
            Constants.IsWaitingForTrackQueueToFill = false;
        }

        // otherwise, remove the track from the queue and play it
        Constants.TrackCurrentlyLoadedInPlayer = Constants.TrackQueue.remove();
        Constants.Database.queueDelete(Constants.TrackCurrentlyLoadedInPlayer.mediaId);

        player = new MediaPlayer();
        player.setOnCompletionListener(this);
        player.setOnPreparedListener(this);

        try {
            FileInputStream inputStream = new FileInputStream(Business.GetTrackFilePath(Constants.TrackCurrentlyLoadedInPlayer));
            player.setDataSource(inputStream.getFD());
            player.prepare();
            player.start();

        } catch (IOException e) {
            Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "MainActivity.StartPlaying() failed: " + e.getMessage() + " mediaId: " + Constants.TrackCurrentlyLoadedInPlayer.mediaId);
            // for now, assume a bad downloaded file.
            Constants.Database.trackDelete(Constants.TrackCurrentlyLoadedInPlayer.mediaId);
            Constants.TrackCurrentlyLoadedInPlayer = null;
            Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "MainActivity.StartPlaying()   making new call to OnPlayNextTrack after error.");
            OnPlayNextTrack(true);
            return;
        }

        Business.TellHouseTrackWasPlayed(Constants.TrackCurrentlyLoadedInPlayer);
    }

    private void ChangeUser(String usercode) {
        Constants.WriteToFile("changing to user " + usercode);
        Constants.TrackQueue.clear();
        Constants.Database.queueDeleteAll();
        Constants.IsWaitingOnDownloadBeforeQueuingAudio = -1;
        Constants.USER_CODE = usercode;
        this.btnNextTrack.setText(">>");
        OnPlayNextTrack(true);
    }

    private void Init() {
        Constants.WriteToFile("\n\n\nstarting up");

        Constants.Database = new DataHelper(this);

        Constants.TrackQueue = new LinkedList<Track>();
        Constants.DownloaderThread = null;
        progressDialog = null;

        btnPausePlay = (Button) findViewById(R.id.btnPausePlay);
        btnPausePlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    togglePausePlay();
                } catch (Exception e) {
                    Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "btnPausePlay.onClick(): " + e.getMessage());
                }
            }
        });

        btnNextTrack = (Button) findViewById(R.id.btnNextTrack);
        btnNextTrack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (player != null) {
                    TellHouseTrackWasSkippedBeforeCompletion();
                }
                OnPlayNextTrack(true);
            }

        });

        btnStopTrackQuitApp = (Button) findViewById(R.id.btnStopTrackQuitApp);
        btnStopTrackQuitApp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Business.IsPlaying(thisActivity.player)) {
                    Business.StopPlaying(thisActivity.player);
                } else {
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            }
        });

        this.trackSeekBar = (SeekBar) findViewById(R.id.seekBar1);
        this.trackSeekBar.setOnSeekBarChangeListener(this);

        // TRACK UPDATE TASK
        // remove any callbacks on the track updater.
        this.trackHandler.removeCallbacks(this.updateTrackTask);
        if (Constants.TRACK_UPDATER_TASK_IS_RUNNING) {
            this.trackHandler.postDelayed(this.updateTrackTask, 100);
        }
        StartPlaying();
    }

    private void InitMenu(Menu menu) {
        menu.add(Menu.NONE, Constants.MENU_ITEM_SEARCH, Menu.NONE, "search");
        menu.add(Menu.NONE, Constants.MENU_ITEM_CURRENT_SETTINGS, Menu.NONE, "current settings");
        menu.add(Menu.NONE, Constants.MENU_ITEM_DARWIN_PERCENTAGE, Menu.NONE, "darwin percentage");
        menu.add(Menu.NONE, Constants.MENU_ITEM_CHANGE_TO_USER_4, Menu.NONE, Constants.USER_DISPLAY_4);
        menu.add(Menu.NONE, Constants.MENU_ITEM_CHANGE_TO_USER_3, Menu.NONE, Constants.USER_DISPLAY_3);
        menu.add(Menu.NONE, Constants.MENU_ITEM_CHANGE_TO_USER_1, Menu.NONE, Constants.USER_DISPLAY_1);
        menu.add(Menu.NONE, Constants.MENU_ITEM_CHANGE_TO_USER_2, Menu.NONE, Constants.USER_DISPLAY_2);
    }

    private void OnOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case Constants.MENU_ITEM_DARWIN_PERCENTAGE:
                Intent darwinPercentageIntent = new Intent(MainActivity.this, DarwinPercentageActivity.class);
                MainActivity.this.startActivityForResult(darwinPercentageIntent, SEARCH_ACTIVITY__TERMINATED_CODE);
                break;

            case Constants.MENU_ITEM_SEARCH:
                Intent searchIntent = new Intent(MainActivity.this, SearchActivity.class);
                MainActivity.this.startActivityForResult(searchIntent, SEARCH_ACTIVITY__TERMINATED_CODE);
                break;

            case Constants.MENU_ITEM_CURRENT_SETTINGS:
                Intent currentSettingsIntent = new Intent(MainActivity.this, CurrentSettingsActivity.class);
                MainActivity.this.startActivityForResult(currentSettingsIntent, SEARCH_ACTIVITY__TERMINATED_CODE);
                break;

            case Constants.MENU_ITEM_CHANGE_TO_USER_1:
                this.ChangeUser(Constants.USER_1_CODE);
                break;

            case Constants.MENU_ITEM_CHANGE_TO_USER_3:
                this.ChangeUser(Constants.USER_3_CODE);
                break;

            case Constants.MENU_ITEM_CHANGE_TO_USER_2:
                this.ChangeUser(Constants.USER_2_CODE);
                break;

            case Constants.MENU_ITEM_CHANGE_TO_USER_4:
                this.ChangeUser(Constants.USER_4_CODE);
                break;
        }
    }

    private int queueSizeGet() {
        return Constants.TrackQueue.size();
    }

    private int getTrackFileDuration() {
        return Constants.TrackFileDuration;
    }

    private void togglePausePlay() {
        if (Business.IsPlaying(this.player)) {
            player.pause();
            btnPausePlay.setText(R.string.button_play_text);
        } else {
            player.start();
            btnPausePlay.setText(R.string.button_pause_text);
        }
    }

    private void queueTrackAfterDownloadingIt(Track track) {
        Constants.WriteToFile("finished downloading (" + track.mediaId + ") " + track.title + ". queuin it up");

        for (Track queuedTrack : Constants.TrackQueue) {
            if (queuedTrack.mediaId == track.mediaId) {
                Constants.WriteToFile("woah, ...... no. can't queue (" + track.mediaId + ") " + track.title + ". it's already queued. ... leaving");
                return;
            }
        }
        Constants.TrackQueue.add(track);
        this.btnPausePlay.setText("Q " + this.queueSizeGet());
        if (Constants.IsWaitingForTrackQueueToFill) {
            StartPlaying();
        }
    }

    private void TellHouseTrackWasSkippedBeforeCompletion() {
        if (Constants.TrackCurrentlyLoadedInPlayer == null){
            return;
        }
        Business.TellHouseTrackWasSkippedBeforeCompletion(Constants.TrackCurrentlyLoadedInPlayer, player.getCurrentPosition() / 1000);  // current position is in milliseconds
    }

    private void TellHouseTrackCompletelyPlayed() {
        if (Constants.TrackCurrentlyLoadedInPlayer == null){
            return;
        }
        Business.TellHouseTrackCompletelyPlayed(Constants.TrackCurrentlyLoadedInPlayer, CurrentTrackSecondsPlayed);
        this.CurrentTrackSecondsPlayed = 0;
    }
}
