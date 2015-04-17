package saga.alexwooten.com;

/**
 * Created by alex on 4/17/2015.
 */
public class Track {
    public int mediaId;
    public String fileLocation;
    public boolean fileDownloaded;
    public String title;

    public Track(){}

    public Track(int mediaId, String fileLocation, String title){
        this.mediaId = mediaId;
        this.fileLocation = fileLocation;
        this.title = title;

    }
}
