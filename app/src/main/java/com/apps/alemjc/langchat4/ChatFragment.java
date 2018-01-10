package com.apps.alemjc.langchat4;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;

import java.util.*;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ChatFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_CHAT_ID = "chatId";

    private String chatId;
    private List<Message> messages;
    MessageListAdapter messageListAdapter;
    private ImageButton floatingActionButton;
    private EditText editText;
    private RecyclerView recyclerView;
    private OnFragmentInteractionListener mListener;
    private DatabaseReference databaseReference;
    private FirebaseUser firebaseUser;
    private static final String BG_NAME = "bg name";
    private Handler handler;
    private HandlerThread handlerThread;


    private View.OnClickListener sendMessageListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            InputMethodSubtype imms = imm.getCurrentInputMethodSubtype();
            String locale = imms.getLocale();
            Locale locale1 = new Locale(locale);
            String localeLanguage = locale1.getLanguage();
            String language;

            if(localeLanguage.contains("_")){
                String split [] = localeLanguage.split("_");
                language = split[0];
            }
            else{
                language = localeLanguage;
            }

            String message = editText.getText().toString();
            Message messageObj = new Message(firebaseUser.getUid(), message, language);

            String messageKey = databaseReference.child("chats").child(chatId).child("messagepools").child("server")
                    .push().getKey();

            Map<String, Object> newMessageUpdate = new HashMap<>();
            newMessageUpdate.put("/chats/"+chatId+"/messagepools/server/"+messageKey, messageObj.toMap());
//            newMessageUpdate.put("/chats/"+chatId+"/messagepools/"+firebaseUser.getUid()+"/messages/"+messageKey,
//                    messageObj.toMap());
            databaseReference.updateChildren(newMessageUpdate);
            editText.setText(""); // assume message was successfully sent and clear text area

        }
    };

    private ChildEventListener nMessageListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {

            if(isDetached()){
                return;
            }

            if(dataSnapshot.exists()){
                Message message = dataSnapshot.getValue(Message.class);
                messages.add(message);
                messageListAdapter.notifyItemInserted(messages.size()-1);
                recyclerView.scrollToPosition(messages.size()-1);
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Chat id.
     * @return A new instance of fragment ChatFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChatFragment newInstance(String param1) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHAT_ID, param1);
        fragment.setArguments(args);
        return fragment;
    }

    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        messages = new ArrayList<>();
        if (getArguments() != null) {
            chatId = getArguments().getString(ARG_CHAT_ID);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        messageListAdapter = new MessageListAdapter();
        recyclerView = (RecyclerView) view.findViewById(R.id.texts);
        floatingActionButton = (ImageButton) view.findViewById(R.id.sendButton);
        editText = (EditText) view.findViewById(R.id.message);
        floatingActionButton.setOnClickListener(sendMessageListener);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(messageListAdapter);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        startThreads();
        if(firebaseUser == null){
            //TODO: user is not logged in. should probably redirect user to login page or show a log in pop up
        }
        else{
            databaseReference.child("chats").child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    if(isDetached()){
                        return;
                    }

                    if(dataSnapshot.exists()){
                        DataSnapshot poolSnapShot = dataSnapshot.child("messagepools").child(firebaseUser.getUid());
                        if(!poolSnapShot.exists()){
                            selectLaguageDialog();
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            databaseReference.child("chats").child(chatId).child("messagepools").child(firebaseUser.getUid()).child("messages")
                    .addChildEventListener(nMessageListener);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        stopThreads();
        databaseReference.child("chats").child(chatId).child("messagepools").child(firebaseUser.getUid()).child("messages")
                .removeEventListener(nMessageListener);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void startThreads(){
        handlerThread = new HandlerThread(BG_NAME);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    private void stopThreads(){
        handlerThread.quitSafely();
        handlerThread = null;
        handler = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(messages != null){
            int size = messages.size();
            messages.clear();
            messages = null;
            messageListAdapter.notifyItemRangeRemoved(0, size);
        }

    }

    private void selectLaguageDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle("Choose a language for receiving messages")
                .setItems(R.array.languages, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        String[] languages = getResources().getStringArray(R.array.languages);
                        String language = languages[i];

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("language",language);

                        Map<String, Object> location = new HashMap<>();
                        location.put("/chats/"+chatId+"/messagepools/"+firebaseUser.getUid(),updates);
                        databaseReference.updateChildren(location);

                    }
                });
        builder.create().show();
    }

    // TODO: Rename method, update argument and hook method into UI event
//    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
//    }

//    @Override
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
//        try {
//            mListener = (OnFragmentInteractionListener) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
//    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private class MessageListAdapter extends RecyclerView.Adapter<MessageListAdapter.ViewHolder>{

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater.inflate(R.layout.messagelayout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            Log.d("chatFragment","onBindViewHolder");
            final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
            Message message = messages.get(position);

            Log.d("chatFragment","onBindViewHolder. message: "+message.getMessage());
            holder.messageView.setText(message.getMessage());
            RelativeLayout.LayoutParams messageLayoutParams = (RelativeLayout.LayoutParams)holder.messageLayout.getLayoutParams();
            RelativeLayout.LayoutParams cardViewLayoutParams = (RelativeLayout.LayoutParams)holder.messageParentView.getLayoutParams();
            if(message.getBy().equals(firebaseUser.getUid())){
                messageLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                holder.messageLayout.setLayoutParams(messageLayoutParams);
                holder.messageParentView.setVisibility(View.GONE);
            }
            else{
                holder.messageLayout.setLayoutParams(messageLayoutParams);
                holder.messageParentView.setLayoutParams(cardViewLayoutParams);
                databaseReference.child("users").child(message.getBy()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if(isDetached()){
                            return;
                        }

                        if(dataSnapshot.exists()){
                            User user = dataSnapshot.getValue(User.class);
                            String imagePath = user.getImagePath();
                            if(imagePath != null){
                                Handler uihandler = new Handler(Looper.getMainLooper());
                                Uri imageUri = Uri.parse(imagePath);
                                //FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
                                String fileName = imageUri.getLastPathSegment();
                                //String path = "images/"+user.getUid()+"/"+ imageUri.getLastPathSegment();
                                StorageUtility.setImage(getActivity(), holder.accountPhoto, null, user.getUid(),
                                        fileName, imagePath, uihandler, handler);
                            }

                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            holder.messageLayout.setLayoutParams(messageLayoutParams);
        }

        @Override
        public int getItemCount() {
            if(messages == null)
                return 0;
            else{
                return messages.size();
            }
        }

        public class ViewHolder extends RecyclerView.ViewHolder{
            public View messageLayout;
            public TextView messageView;
            public ImageView accountPhoto;
            public View messageParentView;

            public ViewHolder(View itemView) {
                super(itemView);

                messageLayout = itemView.findViewById(R.id.messageLayout);
                messageView = (TextView) itemView.findViewById(R.id.message);
                accountPhoto = (ImageView) itemView.findViewById(R.id.userImage);
                messageParentView = itemView.findViewById(R.id.cardView);
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}
