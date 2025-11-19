package com.example.culturalcompass.ui.assistant;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.culturalcompass.R;
import com.example.culturalcompass.model.Message;

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
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    // UI
    private TextView clearButton;
    private RecyclerView chatRecycler;
    private ChatAdapter chatAdapter;
    private EditText inputMessage;
    private ImageButton sendButton;

    // Typing animation flag
    private volatile boolean stopTyping = false;

    // RecyclerView messages
    private final List<Message> messages = new ArrayList<>();

    // History sent to Gemini
    private final List<JSONObject> conversationHistory = new ArrayList<>();

    private final OkHttpClient client = new OkHttpClient();

    private LinearLayout emptyState;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_assistant, container, false);

        View mainHeader = requireActivity().findViewById(R.id.header);
        if (mainHeader != null)
            mainHeader.setVisibility(View.GONE);


        emptyState = view.findViewById(R.id.empty_state);


        API_KEY = getString(R.string.gemini_api_key);

        // Bind UI
        chatRecycler = view.findViewById(R.id.chat_recycler);
        inputMessage = view.findViewById(R.id.input_message);
        sendButton = view.findViewById(R.id.send_button);
        clearButton = view.findViewById(R.id.ai_header_clear);
        clearButton.setOnClickListener(v -> {
            if (messages.isEmpty()) {
                Toast.makeText(requireContext(), "No messages to clear.", Toast.LENGTH_SHORT).show();
                return;
            }
            showClearDialog();
        });


        // Init RecyclerView
        chatAdapter = new ChatAdapter(messages);
        chatRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatRecycler.setAdapter(chatAdapter);

        // System context
        addSystemContextMessage();

        setupSendListener();

        return view;
    }


    private void showClearDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear Chat History?")
                .setMessage("This will delete all your conversation.\nThis action cannot be undone.")
                .setCancelable(true)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Clear", (dialog, which) -> {
                    clearChat();
                    dialog.dismiss();
                })
                .show();
    }


    private void clearChat() {
        conversationHistory.clear();
        addSystemContextMessage(); // keep system prompt

        // Clear UI
        messages.clear();
        chatAdapter.notifyDataSetChanged();

        // Show welcome screen again
        emptyState.setVisibility(View.VISIBLE);

        stopTyping = true;
    }


    private void setupSendListener() {
        sendButton.setOnClickListener(v -> {
            String userMsg = inputMessage.getText().toString().trim();
            if (userMsg.isEmpty()) return;

            // Add user bubble to chat
            addMessageToUI(new Message(Message.USER, userMsg));

            inputMessage.setText("");

            // Show typing bubble
            showTypingBubble();

            // Send to Gemini
            sendMessageToGemini(userMsg);
        });
    }


    private void addSystemContextMessage() {
        try {
            JSONObject sys = new JSONObject();
            sys.put("role", "model");

            JSONArray parts = new JSONArray();
            JSONObject txt = new JSONObject();

            txt.put("text",
                    "You are Cultural Compass, an AI cultural guide. " +
                            "Rules: No asterisks, no markdown, plain text only, short and friendly."
            );

            parts.put(txt);
            sys.put("parts", parts);

            conversationHistory.add(sys);

        } catch (Exception ignored) {
        }
    }


    private void addMessageToUI(Message msg) {
        emptyState.setVisibility(View.GONE);
        messages.add(msg);
        chatAdapter.notifyItemInserted(messages.size() - 1);
        chatRecycler.smoothScrollToPosition(messages.size() - 1);
    }


    @SuppressLint("NotifyDataSetChanged")
    private void showTypingBubble() {
        stopTyping = false;

        // Add typing message
        Message typingMsg = new Message(Message.ASSISTANT, "Typing...");
        messages.add(typingMsg);
        chatAdapter.notifyDataSetChanged();
        chatRecycler.smoothScrollToPosition(messages.size() - 1);

        new Thread(() -> {
            String[] dots = {"", ".", "..", "..."};
            int i = 0;

            while (!stopTyping) {
                String text = "Typing" + dots[i % dots.length];
                int index = messages.size() - 1;
                int fi = i;

                requireActivity().runOnUiThread(() -> {
                    if (index >= 0 && index < messages.size()) {
                        messages.get(index).text = "Typing" + dots[fi % dots.length];
                        chatAdapter.notifyDataSetChanged();
                    }
                });

                i++;
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {
                }

            }
        }).start();
    }


    private void sendMessageToGemini(String userMsg) {

        try {
            // Build user JSON
            JSONObject userObj = new JSONObject();
            userObj.put("role", "user");

            JSONArray userParts = new JSONArray();
            JSONObject t = new JSONObject();
            t.put("text", userMsg);
            userParts.put(t);

            userObj.put("parts", userParts);

            conversationHistory.add(userObj);

        } catch (Exception ignored) {
            addMessageToUI(new Message(Message.ASSISTANT, "JSON error."));
            return;
        }

        // Build request JSON
        JSONArray contentsArray = new JSONArray(conversationHistory);

        JSONObject bodyJson = new JSONObject();
        try {
            bodyJson.put("contents", contentsArray);
        } catch (Exception ignored) {
        }

        RequestBody body = RequestBody.create(bodyJson.toString(), JSON_MEDIA);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="
                + API_KEY;

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();


        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() -> {
                    stopTyping = true;
                    removeTypingBubble();
                    addMessageToUI(new Message(Message.ASSISTANT, "Network error."));
                });
            }


            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();
                String reply = parseReply(json);

                requireActivity().runOnUiThread(() -> {
                    stopTyping = true;
                    removeTypingBubble();
                    addMessageToUI(new Message(Message.ASSISTANT, reply));
                });

                addReplyToConversationHistory(reply);
            }
        });
    }


    private void removeTypingBubble() {
        int lastIndex = messages.size() - 1;
        if (lastIndex >= 0 && messages.get(lastIndex).text.startsWith("Typing")) {
            messages.remove(lastIndex);
            chatAdapter.notifyDataSetChanged();
        }
    }


    private void addReplyToConversationHistory(String reply) {
        try {
            JSONObject modelObj = new JSONObject();
            modelObj.put("role", "model");

            JSONArray parts = new JSONArray();
            JSONObject p = new JSONObject();
            p.put("text", reply);
            parts.put(p);

            modelObj.put("parts", parts);

            conversationHistory.add(modelObj);

        } catch (Exception ignored) {
        }
    }


    private String parseReply(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray candidates = obj.getJSONArray("candidates");
            JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");

            return parts.getJSONObject(0).getString("text").trim();

        } catch (Exception e) {
            return "Something went wrong. Try again.";
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        conversationHistory.clear();
        View mainHeader = requireActivity().findViewById(R.id.header);
        if (mainHeader != null)
            mainHeader.setVisibility(View.VISIBLE);

    }

    @Override
    public void onResume() {
        super.onResume();

        View mainHeader = requireActivity().findViewById(R.id.header);
        if (mainHeader != null)
            mainHeader.setVisibility(View.GONE);
    }

}
