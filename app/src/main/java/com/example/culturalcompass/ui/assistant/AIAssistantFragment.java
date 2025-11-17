package com.example.culturalcompass.ui.assistant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.culturalcompass.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIAssistantFragment extends Fragment {

    private static String API_KEY;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private TextView assistantResponse;
    private TextView userQuestion;
    private EditText inputMessage;
    private ImageButton sendButton;

    private LinearLayout assistantContainer;
    private ImageView assistantIcon;

    private final OkHttpClient client = new OkHttpClient();

    // Session memory
    private final List<JSONObject> conversationHistory = new ArrayList<>();


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_assistant, container, false);

        API_KEY = getString(R.string.gemini_api_key);

        // Bind UI
        userQuestion = view.findViewById(R.id.user_question);
        assistantResponse = view.findViewById(R.id.assistant_response);
        assistantContainer = view.findViewById(R.id.assistant_container);
        assistantIcon = view.findViewById(R.id.assistant_icon);
        inputMessage = view.findViewById(R.id.input_message);
        sendButton = view.findViewById(R.id.send_button);

        // Hide UI bubbles at start
        userQuestion.setVisibility(View.GONE);
        assistantContainer.setVisibility(View.GONE);
        assistantResponse.setVisibility(View.GONE);
        assistantIcon.setVisibility(View.GONE);

        setupSendListener();

        return view;
    }


    private void setupSendListener() {
        sendButton.setOnClickListener(v -> {
            String userMsg = inputMessage.getText().toString().trim();
            if (userMsg.isEmpty()) return;

            // Show user bubble
            userQuestion.setText(userMsg);
            userQuestion.setVisibility(View.VISIBLE);

            // Make assistant visible (with "thinking…")
            assistantContainer.setVisibility(View.VISIBLE);
            assistantIcon.setVisibility(View.VISIBLE);
            assistantResponse.setVisibility(View.VISIBLE);
            assistantResponse.setText("Thinking...");

            inputMessage.setText("");

            sendMessageToGemini(userMsg);
        });
    }


    private void sendMessageToGemini(String userMsg) {

        try {
            // Build user JSON
            JSONObject userObj = new JSONObject();
            userObj.put("role", "user");

            JSONArray userParts = new JSONArray();
            JSONObject textObj = new JSONObject();
            textObj.put("text", userMsg);
            userParts.put(textObj);

            userObj.put("parts", userParts);

            conversationHistory.add(userObj);

        } catch (Exception e) {
            assistantResponse.setText("JSON error: " + e.getMessage());
            return;
        }

        // Build full JSON payload
        JSONArray contentsArray = new JSONArray(conversationHistory);

        JSONObject requestBodyJson = new JSONObject();
        try {
            requestBodyJson.put("contents", contentsArray);
        } catch (Exception e) {
            assistantResponse.setText("JSON build error");
            return;
        }

        RequestBody body = RequestBody.create(requestBodyJson.toString(), JSON);

        String endpoint =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="
                        + API_KEY;

        Request request = new Request.Builder()
                .url(endpoint)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() ->
                        assistantResponse.setText("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();

                String reply = parseReply(json);

                requireActivity().runOnUiThread(() -> {
                    assistantResponse.setText(reply);
                });

                addModelMessage(reply);
            }
        });
    }


    private void addModelMessage(String reply) {
        try {
            JSONObject modelObj = new JSONObject();
            modelObj.put("role", "model");

            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", reply);
            parts.put(part);

            modelObj.put("parts", parts);

            conversationHistory.add(modelObj);

        } catch (Exception ignored) { }
    }


    private String parseReply(String json) {
        try {
            JSONObject obj = new JSONObject(json);

            JSONArray candidates = obj.getJSONArray("candidates");
            JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            return parts.getJSONObject(0).getString("text");

        } catch (Exception e) {
            return "Parsing error: " + e.getMessage() + "\nRAW:\n" + json;
        }
    }
}
