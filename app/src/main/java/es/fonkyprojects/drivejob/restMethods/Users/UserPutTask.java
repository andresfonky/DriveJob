package es.fonkyprojects.drivejob.restMethods.Users;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import es.fonkyprojects.drivejob.model.User;

public class UserPutTask  extends AsyncTask<String, Void, String> {

    ProgressDialog progressDialog;
    Context context;
    String result;

    private User user;

    public UserPutTask(Context c) {
        this.context = c;
    }

    public void setUserPut(User user){
        this.user = user;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Updating data");
        progressDialog.show();
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            return putData(params[0]);
        } catch (IOException ioe) {
            return "Network error";
        } catch (JSONException js) {
            return "Data invalid";
        }
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        this.result = result;

        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    private String putData(String uriPath) throws IOException, JSONException {

        BufferedWriter bufferedWriter = null;

        //Create data to send to server
        JSONObject dataToSend = new JSONObject();
        dataToSend.put("username", user.getUsername());
        dataToSend.put("surname", user.getSurname());
        dataToSend.put("emailVerify", user.getEmailVerify());

        try {
            //Initialize and config request, the connect to server
            URL url = new URL(uriPath);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(10000);
            urlConnection.setRequestMethod("PUT");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.connect();

            //Write data into server
            OutputStream outputStream = urlConnection.getOutputStream();
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
            bufferedWriter.write(dataToSend.toString());
            bufferedWriter.flush();

            //Check update successful or not
            if (urlConnection.getResponseCode() == 200) {
                return "Update";
            } else {
                return "Update failed " + urlConnection.getResponseCode();
            }

        } finally {
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        }
    }
}
