package saga.alexwooten.com;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.ParseException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class SearchActivity extends Activity {

    EditText _KeywordEditText;
    ListView _SearchItemListView;
    Button _SearchButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        Constants.WriteToFile("\n\n\nstarting up search\n\n\n");

        _SearchButton = (Button)findViewById(R.id.buttonSearch);
        _SearchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    String keyword = "";
                    keyword = _KeywordEditText.getText().toString();
                    String url = Constants.URL_OF_API + "api/Search/" + keyword + "?code=" + Constants.USER_CODE;
                    new SearchItemTask(_SearchItemListView).execute(url);
                } catch (Exception e) {
                    Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "_SearchButton.onClick(): " + e.getMessage());
                }
            }
        });


        _KeywordEditText = (EditText)findViewById(R.id.editTextKeyword);
        _SearchItemListView = (ListView)findViewById(R.id.listViewSearchItems);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.search, menu);
        return true;
    }


    class SearchItemTask extends AsyncTask<String, Void, String>{
        private final WeakReference<ListView> listViewWeakReference;

        public SearchItemTask(ListView listView) {
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
                    SearchItemAdapter adapter = new SearchItemAdapter(SearchActivity.this, R.layout.search_item_list_view_item, searchItems);
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
