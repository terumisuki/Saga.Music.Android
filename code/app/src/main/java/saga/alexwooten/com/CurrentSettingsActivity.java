package saga.alexwooten.com;

/**
 * This Activity shows the settings that the user currently has.
 *
 * example:
 *
 * 	Classical 		(genre)		(off)
 * 	Dream Theater 	(artist)	(on)
 *
 */

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.net.ParseException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ListView;

public class CurrentSettingsActivity extends Activity {
    ListView _CurrentSettingsListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.current_settings);

        Constants.WriteToFile("\n\n\nstarting up current settings activity\n\n\n");

        try{
            _CurrentSettingsListView = (ListView)findViewById(R.id._CurrentSettingsListView);

            Constants.WriteToFile("calling house to get current settings");
            String url = Constants.URL_OF_API + "api/CurrentSettings/?code=" + Constants.USER_CODE;
            new CurrentSettingsItemTask(_CurrentSettingsListView).execute(url);
        } catch (Exception e) {
            Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + e.getMessage());
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.current_settings, menu);

        return true;
    }


    class CurrentSettingsItemTask extends AsyncTask<String, Void, String>{
        private final WeakReference<ListView> listViewWeakReference;

        public CurrentSettingsItemTask(ListView listView) {
            listViewWeakReference = new WeakReference<ListView>(listView);
        }

        @Override
        protected String doInBackground(String... arg0) {
            String json = MyHttpClient.ReturnJsonFromUrl(arg0[0]);
            return json;
        }

        @Override
        protected void onPostExecute(String jsonTasks) {
            super.onPostExecute(jsonTasks);

            if (listViewWeakReference == null){ return; }

            ListView listView = listViewWeakReference.get();
            if (listView != null){
                try {
                    JSONArray jsonArray = new JSONArray(jsonTasks);
                    List<SearchItem> searchItems = new ArrayList<SearchItem>();
                    for(int i=0; i<jsonArray.length(); i++){
                        SearchItem searchItem = new SearchItem();
                        JSONObject row = jsonArray.getJSONObject(i);

                        int sItemId = row.getInt("Id");
                        String sItemLabel = row.getString("Label");
                        String sSetting = row.getString("Setting");
                        String sType = row.getString("Type");
                        Boolean bSetting = null;
                        if (sSetting == "true"){
                            bSetting = true;
                        } else if (sSetting == "false"){
                            bSetting = false;
                        }

                        searchItem.ItemId = sItemId;
                        searchItem.ItemType = sType;
                        searchItem.ItemLabel = sItemLabel;
                        searchItem.Setting = bSetting;
                        searchItems.add(searchItem);
                    }
                    SearchItemAdapter adapter = new SearchItemAdapter(CurrentSettingsActivity.this, R.layout.search_item_list_view_item, searchItems);
                    listView.setAdapter(adapter);
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }

}
