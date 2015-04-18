package edu.nyu.scps.weather;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;


public class MainActivity extends ActionBarActivity {
    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            throw new RuntimeException();
        }

    };
    private String string;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


    //For documentation about parameters such as q= and units=, see
    //http://openweathermap.org/current

        //A builder object can create a dialog object.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Name");
        builder.setMessage("Enter a name");

        //This inflator reads the dialog.xml and creates the objects described therein.
        //Pass null as the parent view because it's going in the dialog layout.
        LayoutInflater inflater = getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog, null);
        builder.setView(view);

        //Must be final to be mentioned in the anonymous inner class.
        final EditText editText = (EditText)view.findViewById(R.id.editText);

        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && keyCode == KeyEvent.KEYCODE_ENTER) {

                    Editable editable = editText.getText();
                    string = editable.toString();


                    //Sending this message will break us out of the loop below.
                    Message message = handler.obtainMessage();
                    handler.sendMessage(message);
                }
                return false;
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        //Loop until the user presses the EditText's Done button.
        try {
            Looper.loop();
        }
        catch(RuntimeException runtimeException) {
        }

        alertDialog.dismiss();

        String urlString =
                "http://api.openweathermap.org/data/2.5/weather"
                        + "?q="+string+",US"     //The Woolworth Building has its own zip code.
                        + "&units=imperial" //fahrenheit, not celsius
                        + "&mode=json";     //vs. xml or html

        DownloadTask downloadTask = new DownloadTask();
        downloadTask.execute(urlString);

    }

    private class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urlString) {
            //Must be declared outside of try block,
            //so we can mention them in finally block.
            HttpURLConnection httpURLConnection = null;
            BufferedReader bufferedReader = null;

            try {
                URL url = new URL(urlString[0]);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.connect();

                InputStream inputStream = httpURLConnection.getInputStream();
                if (inputStream == null) {
                    return null;
                }

                StringBuffer buffer = new StringBuffer();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                bufferedReader = new BufferedReader(inputStreamReader);

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    buffer.append(line);
                }

                if (buffer.length() == 0) {
                    return null;
                }
                String json = buffer.toString();
                return json;
            } catch (IOException exception) {
                Log.e("myTag", "doInBackground IOException ", exception);
                return null;
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (final IOException exception) {
                        Log.e("myTag", "doInBackground IOException closing BufferedReader", exception);
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(String json) {
            useTheResult(json);
        }
    }

    private void useTheResult(String json) {
        TextView textView = (TextView) findViewById(R.id.textView);
        if (json == null) {
            textView.setText("Couldn't get JSON string from server.");
            return;
        }
        textView.setText("JSON string downloaded from server:\n\n");
        textView.append(json + "\n\n");

        //For documentation about
        //http://openweathermap.org/weather-data#current
        //For the id condition code in weather, see
        //http://openweathermap.org/weather-conditions

        try {
            JSONObject jSONObject = new JSONObject(json);
            textView.append("Pretty printed version of JSON string:\n\n");
            textView.append(jSONObject.toString(4) + "\n\n"); //number of spaces to indent

            textView.append("The main part of the weather report:\n\n");
            JSONObject main = jSONObject.getJSONObject("main");
            textView.append(main.toString(4) + "\n\n");

            double fahrenheit = main.getDouble("temp");
            textView.append("Woolworth Building temperature is " + fahrenheit + "\u00B0 Fahrenheit.\n");

            long seconds = jSONObject.getLong("dt"); //data receiving time, in seconds after 1970
            SimpleDateFormat dateFormat = new SimpleDateFormat("K:m:s a EEEE, MMMM d, yyyy");
            textView.append(dateFormat.format(1000L * seconds));   //convert seconds to milliseconds
        } catch (JSONException exception) {
            textView.setText("JSONException" + exception);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
