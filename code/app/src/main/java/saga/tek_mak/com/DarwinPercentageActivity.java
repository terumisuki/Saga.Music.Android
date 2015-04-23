package saga.tek_mak.com;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class DarwinPercentageActivity extends Activity {
    TextView _CurrentDarwinPercentageTextView;
    Button _SetDarwinPercentageButton;
    EditText _DarwinPercentageEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_darwin_percentage);

        Constants.WriteToFile("\n\n\nstarting up manage darwin percentage activity\n\n\n");


        _SetDarwinPercentageButton = (Button) findViewById(R.id._SetDarwinPercentageButton);
        _SetDarwinPercentageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    _DarwinPercentageEditText = (EditText)findViewById(R.id._DarwinPercentageTextbox);
                    String sPercentage = _DarwinPercentageEditText.getText().toString();
                    int percentage = Integer.parseInt(sPercentage);
                    Constants.WriteToFile("calling house to set current darwin percentage to " + percentage);

                    String url = Constants.URL_OF_API + "api/darwinpercentage?percentage=" + percentage + "&code=" + Constants.USER_CODE;
                    new SetDarwinPercentageTask().execute(url);
                } catch (Exception e) {
                    Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "btnPausePlay.onClick(): " + e.getMessage());
                }
            }
        });



        try{
            // Todo: Finish pulling the current darwin percentage for display.
//			_CurrentDarwinPercentageTextView = (TextView)findViewById(R.id._CurrentDarwinPercentageTextView);
//
//			Constants.WriteToFile("calling house to get current darwin percentage");
//			String url = Constants.URL_OF_API + "api/CurrentSettings/?code=" + Constants.USER_CODE;
            //new CurrentDarwinPercentageTask(_CurrentDarwinPercentageTextView).execute(url);
        } catch (Exception e) {
            Constants.WriteToFile(Constants.PREPEND_TO_ERROR_LOG + "CurrentSettingsActivity.onCreateOptionsMenu(Menu menu) " + e.getMessage());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.darwin_percentage, menu);
        return true;
    }





    class SetDarwinPercentageTask extends AsyncTask<String, Void, String>{

        public SetDarwinPercentageTask() {
        }

        @Override
        protected String doInBackground(String... arg0) {
            String json = MyHttpClient.ReturnJsonFromUrl(arg0[0]);
            return json;
        }

        @Override
        protected void onPostExecute(String jsonTasks) {
            super.onPostExecute(jsonTasks);
        }

    }


}
