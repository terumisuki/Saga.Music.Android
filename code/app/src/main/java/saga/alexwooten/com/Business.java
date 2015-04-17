package saga.alexwooten.com;

import java.io.File;
import java.util.List;
import java.util.Random;

import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class Business {

    static void FreeUpDiskSpace(DataHelper database) {
        Track track = PickATrackToFreeUpFromDisk(database);
        MarkTrackAsNeedingADownload(track.mediaId, database);
        DeleteTrackFileFromDisk(track.fileLocation);
    }

    private static void DeleteTrackFileFromDisk(String fileLocation) {
        File file = new File(Constants.TRACK_DOWNLOAD_LOCATION + fileLocation);
        long totalBytesSize = file.length();
        if (file.delete()) {
            Constants.WriteToFile("freed up " + (totalBytesSize/1024) + " kb");
        } else {
            Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "failed to delete file.");
        }
    }

    private static void MarkTrackAsNeedingADownload(int mediaId, DataHelper database) {
        database.trackFlagFileNeedsDownloading(mediaId);
    }

    private static Track PickATrackToFreeUpFromDisk(DataHelper database) {
        // This currently just randomly picks a Track form the database and deletes its file.
        // Change this one to something like ....
        //		* get the track with the lowest darwin score,
        //		* or least played,
        //		* or oldest, ...
        //		something. anything.
        //	Just make it smarter than this, please.
        Random random = new Random();
        List<String> trackMediaIds = database.tracksSelectAllWithFilesDownloaded();
        String sMediaId = trackMediaIds.get(random.nextInt(trackMediaIds.size()));
        int mediaId = Integer.parseInt(sMediaId);
        Track track = database.trackGet(mediaId);
        if (!track.fileDownloaded){
            Constants.WriteToFile(track.mediaId + " is already deleted. looking again ... ");
            return PickATrackToFreeUpFromDisk(database);
        }
        return track;
    }

    static String GetTracksDirectory() {
        return Constants.TRACK_DOWNLOAD_LOCATION;
    }

    static String GetTrackFilePath(Track track) {
        if (track == null) {
            return "";
        }
        String tmp = Business.GetTracksDirectory();
        tmp = tmp + track.fileLocation;
        return tmp;
    }

    static boolean IsPlaying(MediaPlayer player) {
        if (player == null) {
            return false;
        }
        return player.isPlaying();
    }

    static void StopPlaying(MediaPlayer player) {
        if (Business.IsPlaying(player)){
            player.stop();
        }
    }

    public static void TellHouseTrackWasPlayed(Track track) {
        class TellHouseTrackWasPlayedTask extends AsyncTask<String, Void, String>{
            private String sUrl;
            private String sMediaId;
            private String sTitle;

            @Override
            protected String doInBackground(String... params) {
                sUrl = params[0];
                sMediaId = params[1];
                sTitle = params[2];

                String result = MyHttpClient.ReturnJsonFromUrl(sUrl);
                return result;
            }

            @Override
            protected void onPostExecute(String json){
                Constants.WriteToFile("told the house (" + sMediaId + ") " + sTitle + " was played.");
                Constants.WriteToFile(Constants.PREPEND_REPLY_FROM_HOUSE + json);
            }
        }

        try{
            String[] args= new String [3];

            String url = Constants.URL_OF_API + "api/trackplayed/" + track.mediaId + "?code=" + Constants.USER_CODE;
            args[0] = url;
            args[1] = Integer.toString(track.mediaId);
            args[2] = track.title;
            new TellHouseTrackWasPlayedTask().execute(args);
        } catch (Exception e){
            Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "TellHouseTrackWasPlayed(Track track): " + e.getMessage() + " ..... " + e.getStackTrace());
        }
    }


    class HttpRequestTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... uri) {
            String json = MyHttpClient.ReturnJsonFromUrl(uri[0]);
            return json;
        }

        @Override
        protected void onPostExecute(String jsonTasks) {
            super.onPostExecute(jsonTasks);
        }
    }


    public static void ProcessDarwin(Track track, int secondsPlayed, boolean blessed){
        class ProcessDarwinTask extends AsyncTask<String, Void, String>{
            private String sBlessed;
            private String sMediaId;
            private String sTitle;
            private String sSecondsPlayed;

            @Override
            protected String doInBackground(String... params) {
                String url = params[0];
                sBlessed = params[1];
                sMediaId = params[2];
                sTitle = params[3];
                sSecondsPlayed = params[4];

                String result = MyHttpClient.ReturnJsonFromUrl(url);
                return result;
            }

            @Override
            protected void onPostExecute(String json){
                String penaltyBonus = "penalty";
                if (sBlessed == "1"){
                    penaltyBonus = "bonus";
                }
                Constants.WriteToFile("told the house to score (" + sMediaId + ") " + sTitle + " with 180 point " + penaltyBonus + " plus " + sSecondsPlayed + " points");
                Constants.WriteToFile(Constants.PREPEND_REPLY_FROM_HOUSE + json);
            }
        }

        try{
            String[] args = new String[5];
            String url = Constants.URL_OF_API + "api/DarwinScore?"
                    + "mediaId=" + track.mediaId
                    + "&secondsPlayed=" + secondsPlayed
                    + "&blessed=" + blessed
                    + "&code=" + Constants.USER_CODE;
            args[0] = url;

            if (blessed){
                args[1] = "1";
            } else {
                args[1] = "0";
            }

            args[2] = Integer.toString(track.mediaId);
            args[3] = track.title;
            args[4] = Integer.toString(secondsPlayed);

            new ProcessDarwinTask().execute(args);
        } catch (Exception e){
            Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "ProcessDarwin(" + track.mediaId + "," + secondsPlayed + ", " + blessed + "): " + e.toString() + " ... : ... " + e.getMessage());
        }
    }


    public static void TellHouseTrackCompletelyPlayed(Track track, int secondsPlayed) {
        Business.ProcessDarwin(track, secondsPlayed, true);
    }


    public static void TellHouseTrackWasSkippedBeforeCompletion(Track track, int secondsPlayed) {
        Business.ProcessDarwin(track, secondsPlayed, false);
    }


    public static void ProcessSettings(String usercode, int targetTypeId, int targetId, Boolean include){
        class ProcessSettingsTask extends AsyncTask<String, Void, String>{

            @Override
            protected String doInBackground(String... params) {
                String url = params[0];

                String result = MyHttpClient.ReturnJsonFromUrl(url);
                return result;
            }

            @Override
            protected void onPostExecute(String json){
                Constants.WriteToFile(Constants.PREPEND_REPLY_FROM_HOUSE + json);
            }
        }

        try {
            String sInclude = "";
            if (include != null){
                if (include){
                    sInclude = "true";
                } else if (!include){
                    sInclude = "false";
                }
            }

            String url = "";
            if (targetTypeId == 1){
                url = Constants.URL_OF_API + "api/GenreSetting/" + targetId + "?code=" + usercode + "&include=" + sInclude;
            } else if (targetTypeId == 2){
                url = Constants.URL_OF_API + "api/ArtistSetting/" + targetId + "?code=" + usercode + "&include=" + sInclude;
            } else if (targetTypeId == 3){
                url = Constants.URL_OF_API + "api/PartSetting/" + targetId + "?code=" + usercode + "&include=" + sInclude;
            }




            String[] args = new String[2];
            args[0] = url;
            new ProcessSettingsTask().execute(args);
        } catch (Exception e){
            Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "ProcessSettings(): " + e.toString() + " ... : ... " + e.getMessage());
        }
    }

    // part
    public static void TellHouseExcludePartFromSettings(int partId) {
        Business.ProcessSettings(Constants.USER_CODE, 3, partId, false);
    }

    public static void TellHouseClearPartFromSettings(int partId) {
        Business.ProcessSettings(Constants.USER_CODE, 3, partId, null);
    }

    public static void TellHouseIncludePartFromSettings(int partId) {
        Business.ProcessSettings(Constants.USER_CODE, 3, partId, true);
    }

    // artist
    public static void TellHouseExcludeArtistFromSettings(int artistId) {
        Business.ProcessSettings(Constants.USER_CODE, 2, artistId, false);
    }

    public static void TellHouseClearArtistFromSettings(int artistId) {
        Business.ProcessSettings(Constants.USER_CODE, 2, artistId, null);
    }

    public static void TellHouseIncludeArtistFromSettings(int artistId) {
        Business.ProcessSettings(Constants.USER_CODE, 2, artistId, true);
    }

    // genre
    public static void TellHouseExcludeGenreFromSettings(int genreId) {
        Business.ProcessSettings(Constants.USER_CODE, 1, genreId, false);
    }

    public static void TellHouseClearGenreFromSettings(int genreId) {
        Business.ProcessSettings(Constants.USER_CODE, 1, genreId, null);
    }

    public static void TellHouseIncludeGenreFromSettings(int genreId) {
        Business.ProcessSettings(Constants.USER_CODE, 1, genreId, true);
    }
}
