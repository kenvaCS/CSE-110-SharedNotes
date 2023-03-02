package edu.ucsd.cse110.sharednotes.model;

import static android.util.Log.println;

import android.util.Log;

import androidx.lifecycle.LiveData;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NoteAPI {
    // TODO: Implement the API using OkHttp!
    // TODO: Read the docs: https://square.github.io/okhttp/
    // TODO: Read the docs: https://sharednotes.goto.ucsd.edu/docs

    private volatile static NoteAPI instance = null;

    private OkHttpClient client;

    public NoteAPI() {
        this.client = new OkHttpClient();
    }

    public static NoteAPI provide() {
        if (instance == null) {
            instance = new NoteAPI();
        }
        return instance;
    }

    /**
     * An example of sending a GET request to the server.
     *
     * The /echo/{msg} endpoint always just returns {"message": msg}.
     */
    public void echo(String msg) {
        // URLs cannot contain spaces, so we replace them with %20.
        msg = msg.replace(" ", "%20");

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/echo/" + msg)
                .method("GET", null)
                .build();

        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            var body = response.body().string();
            Log.i("ECHO", body);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Note getNote(String title) throws ExecutionException, InterruptedException, TimeoutException {
        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + title)
                .method("GET", null)
                .build();

        var executor = Executors.newSingleThreadExecutor();
        Callable<Note> callable = () -> {
            var response = client.newCall(request).execute();
            assert response.body() != null;
            var body = response.body().string();
            Log.i("GET NOTE", body);
            return Note.fromJSON(body);
        };
        Future<Note> future_get = executor.submit(callable);
        Note note_get = future_get.get(1, TimeUnit.SECONDS);
        return note_get;
    }

    public void putNote(Note note){
        var title = note.title.replace(" ", "%20");
        final MediaType JSON = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        var body = RequestBody.create(noteToJson(note), JSON);
        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + title)
                .method("PUT", body)
                .build();
        var executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            Response response = null;
            try {
                response = client.newCall(request).execute();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            assert response.body() != null;
            String body1 = null;
            try {
                body1 = response.body().string();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Log.i("PUT NOTE", body1);
        });
    }

    public String noteToJson(Note note) {
        String toJson = "{  " + "\"content\":" + "\"" + note.content + "\"" + "," + "\"updated_at\":" + "\"" + note.updatedAt + "\"" + "}";
        Log.i("RIGHT FORMAT", toJson);
        return toJson;
    }
}
