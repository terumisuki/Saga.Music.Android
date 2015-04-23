package saga.tek_mak.com;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import android.os.Looper;

public class MyHttpClient {

    private static final int TIMEOUT_MILLISEC = 10000;

    static public String ReturnJsonFromUrl(String uri){
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        String responseString = null;
        try {
            response = httpclient.execute(new HttpGet(uri));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                out.close();
                responseString = out.toString();
            } else{
                Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "MyHttpClient.ReturnJsonFromUrl: statusLine ..... " + statusLine.getStatusCode() + "   " + statusLine.getReasonPhrase());
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (ClientProtocolException e) {
            Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "MyHttpClient.ReturnJsonFromUrl: " + e.toString() + " :: " + e.getMessage());
        } catch (IOException e) {
            Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "MyHttpClient.ReturnJsonFromUrl: " + e.toString() + " :: " + e.getMessage());
        }
        return responseString;
    }

    static public void SendJsonToUrl(final String uri, final JSONObject json){
        Thread t = new Thread() {

            public void run() {
                Looper.prepare();
                HttpClient client = new DefaultHttpClient();
                HttpConnectionParams.setConnectionTimeout(client.getParams(), TIMEOUT_MILLISEC);
                HttpResponse response;

                try {
                    HttpPost post = new HttpPost(uri);
                    StringEntity se = new StringEntity(json.toString(), "UTF-8");
                    se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json;charset=UTF-8"));
                    post.setEntity(se);
                    response = client.execute(post);
                    if(response!=null){
                        InputStream in = response.getEntity().getContent();
                    }

                } catch(Exception e) {
                    e.printStackTrace();
                    String err = Constants.PREPEND_TO_ERROR_LOG + "SendJsonToUrl():  " + e.getMessage() + " " + e.getStackTrace();
                    Constants.WriteToFile(err);
                }

                Looper.loop(); //Loop in the message queue
            }
        };

        t.start();
    }

    public static void GetUrl(final String url) {
        Thread t = new Thread(){
            public void run(){
                HttpClient httpclient = new DefaultHttpClient();
                HttpResponse response;
                try {
                    response = httpclient.execute(new HttpGet(url));
                    StatusLine statusLine = response.getStatusLine();
                    if(statusLine.getStatusCode() == HttpStatus.SC_OK || statusLine.getStatusCode() == HttpStatus.SC_NO_CONTENT){
                        // no problem.
                    } else{
                        Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "MyHttpClient.GetUrl: statusLine ..... " + statusLine.getStatusCode() + "   " + statusLine.getReasonPhrase());
                        response.getEntity().getContent().close();
                        throw new IOException(statusLine.getReasonPhrase());
                    }
                } catch (ClientProtocolException e) {
                    Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "MyHttpClient.GetUrl: " + e.toString() + " :: " + e.getMessage());
                } catch (IOException e) {
                    Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "MyHttpClient.GetUrl: " + e.toString() + " :: " + e.getMessage());
                }
            }
        };
        t.start();
    }
}
