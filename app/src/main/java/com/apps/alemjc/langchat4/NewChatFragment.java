package com.apps.alemjc.langchat4;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ChatOpener} interface
 * to handle interaction events.
 */
public class NewChatFragment extends Fragment {

    private ChatOpener mListener;
    private Button nChatButton;
    private RecyclerView friendsRecyclerView;
    private RecyclerView selectedFriendsRecyclerView;
    private FriendsAdapter friendsAdapter;
    private SelectedFriendsAdapter selectedFriendsAdapter;
    private ArrayList<User> friendsList;
    private ArrayList<User> selectedFriendsList;
    private FirebaseUser firebaseUser;
    private Map<String, Boolean> sharedMap;
    private String startsWith;
    private static final String BG_NAME = "bg name";
    private Handler handler;
    private HandlerThread handlerThread;


    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            startsWith=editable.toString();

            getUserList(true);
        }
    };

    private View.OnClickListener createNewChat = new View.OnClickListener(){
        CountDownLatch countDownLatch;
        private void fillUpdates(DatabaseReference databaseReference,
                                 final String newChatKey, final Map<String, Object> updates, final String uid){
            databaseReference.child("users").child(uid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()){
                                Log.d("nChatFragment", "user exists");
                                User user = dataSnapshot.getValue(User.class);
                                List<String> chats = user.getChats();
                                if(chats == null){
                                    chats = new ArrayList<>();
                                    user.setChats(chats);
                                }

                                Log.d("nChatFragment", "adding chat key: "+newChatKey+"to user");
                                chats.add(newChatKey);
                                updates.put("/users/"+uid, user.toMap());

                                if(countDownLatch != null){
                                    countDownLatch.countDown();
                                }

                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
        }

        @Override
        public void onClick(View view) {
            final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
            final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            final Handler handler = new Handler(getActivity().getMainLooper());

            //User must be logged in, in order to make changes
            if(firebaseUser != null){
                final String key = databaseReference.child("chats").push().getKey();
                final Map<String, Object> chatUpdate = new HashMap<>();
                String chatUserKey;
                countDownLatch = new CountDownLatch(selectedFriendsList.size()+1);

                final Map<String, Object> userUpdates = new HashMap<>();

                for(User user: selectedFriendsList){
                    chatUserKey = databaseReference.child("chats").child(key).child("users").push().getKey();
                    chatUpdate.put("/chats/"+key+"/users/"+chatUserKey, user.toMap());
                    fillUpdates(databaseReference, key, userUpdates, user.getUid());
                }

                fillUpdates(databaseReference, key, userUpdates, firebaseUser.getUid());

                databaseReference.child("users").child(firebaseUser.getUid())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){

                            if(isDetached()){
                                return;
                            }

                            User loggedInUser = dataSnapshot.getValue(User.class);
                            String chatUserKey2 = databaseReference.child("chats").child(key).child("users").push().getKey();
                            chatUpdate.put("/chats/"+key+"/users/"+chatUserKey2,loggedInUser.toMap());
                            databaseReference.updateChildren(chatUpdate);

                            Log.d("nChatFragment", "updates are: "+userUpdates);

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        countDownLatch.await();
                                        databaseReference.updateChildren(userUpdates)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if(task.isSuccessful()){
                                                            if(!NewChatFragment.this.isDetached()){
                                                                handler.post(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        mListener.openChat(key);
                                                                    }
                                                                });

                                                            }
                                                        }
                                                    }
                                                });

                                    }
                                    catch(InterruptedException e){
                                        Log.d("nChatFragment",e.getMessage());
                                    }
                                }
                            }).start();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


            }

        }
    };


    public NewChatFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        friendsList = new ArrayList<>();
        selectedFriendsList = new ArrayList<>();
        sharedMap = new HashMap<>();
        startsWith = "";

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_new_chat, container, false);
        LinearLayoutManager linearLayoutManager;
        nChatButton = (Button) view.findViewById(R.id.newChat);
        EditText searchBar = (EditText) view.findViewById(R.id.searchBar);
        friendsRecyclerView = (RecyclerView) view.findViewById(R.id.userList);
        selectedFriendsRecyclerView = (RecyclerView) view.findViewById(R.id.selectUserList);
        friendsAdapter = new FriendsAdapter();
        selectedFriendsAdapter = new SelectedFriendsAdapter();
        linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        nChatButton.setEnabled(false);
        friendsRecyclerView.setLayoutManager(linearLayoutManager);
        friendsRecyclerView.setAdapter(friendsAdapter);

        linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        selectedFriendsRecyclerView.setLayoutManager(linearLayoutManager);
        selectedFriendsRecyclerView.setAdapter(selectedFriendsAdapter);
        searchBar.addTextChangedListener(textWatcher);
        nChatButton.setOnClickListener(createNewChat);
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        startThreads();
        if(firebaseUser == null){
            // TODO: User not logged in. should re-route to main fragment.
        }
        getUserList(false);


    }

    @Override
    public void onPause() {
        super.onPause();
        stopThreads();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(sharedMap != null){
            sharedMap.clear();
        }

        if(friendsList != null){
            int size = friendsList.size();
            friendsList.clear();
            friendsAdapter.notifyItemRangeRemoved(0, size);
        }

        if(selectedFriendsList != null){
            int size = selectedFriendsList.size();
            selectedFriendsList.clear();
            selectedFriendsAdapter.notifyItemRangeRemoved(0, size);
        }
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

    // TODO: Rename method, update argument and hook method into UI event
//    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
//    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        try {
            mListener = (ChatOpener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }



    private void getUserList(final boolean clearData){

        if(firebaseUser != null){
            final DatabaseReference db = FirebaseDatabase.getInstance().getReference();
            db.child("users").child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    if(isDetached()){
                        return;
                    }

                    if(dataSnapshot.exists()){
                        User user = dataSnapshot.getValue(User.class);
                        List<String> friendsIds = user.getFriends();

                        if(friendsIds != null && friendsIds.size() > 0){

                            if(clearData){
                                int size = 0;
                                if(friendsList != null){
                                    size = friendsList.size();
                                    friendsList.clear();
                                }
                                else{
                                    friendsList = new ArrayList<>();
                                }


                                friendsAdapter.notifyItemRangeRemoved(0,size);
                            }

                            for(String id: friendsIds){
                                db.child("users").child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.exists()){
                                            User user1 = dataSnapshot.getValue(User.class);
                                            friendsList.add(user1);
                                            friendsAdapter.notifyItemInserted(friendsList.size()-1);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });

                            }

                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }
    }


    private class FriendsClickListener implements CompoundButton.OnCheckedChangeListener{

        private int position;
        public FriendsClickListener(int position){
            this.position = position;
        }


        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            User user = friendsList.get(position);
            sharedMap.put(user.getUid(), isChecked);
            Log.d("nCFragment", "friendsClickListener, selected: "+isChecked);
            if(isChecked){
                selectedFriendsList.add(user);
                selectedFriendsAdapter.notifyItemInserted(selectedFriendsList.size()-1);
                nChatButton.setEnabled(true);
            }
            else{
                int p = selectedFriendsList.indexOf(user);
                selectedFriendsList.remove(user);
                selectedFriendsAdapter.notifyItemRemoved(p);
                if(selectedFriendsList.size() == 0){
                    nChatButton.setEnabled(false);
                }
            }
        }
    }

    private class SelectedFriendsClickListener implements View.OnClickListener{

        private int position;

        public SelectedFriendsClickListener(int position){
            this.position = position;
        }

        @Override
        public void onClick(View view) {
            User user = selectedFriendsList.get(position);
            selectedFriendsList.remove(position);
            sharedMap.put(user.getUid(), false);
            selectedFriendsAdapter.notifyItemRemoved(position);
            Log.d("ncFragment","uid: "+user.getUid());
            friendsAdapter.notifyDataSetChanged();
        }
    }

    private class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder>{

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater.inflate(R.layout.searchuserscheckbox, parent, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            User user = friendsList.get(position);
            holder.displayName.setText(user.getDisplayName());
            holder.status.setText(user.getDisplayName());
            boolean selected = false;

            if(sharedMap.size() > 0){
                selected = sharedMap.get(user.getUid());
            }

            Log.d("ncFragment", String.format("selected: %s position: %d uid: %s", selected, position, user.getUid()));

            holder.checkBox.setChecked(selected);
            holder.checkBox.setOnCheckedChangeListener(new FriendsClickListener(position));

            String imagePath = user.getImagePath();
            Uri imageUri = Uri.parse(imagePath);
            if(imagePath != null){
                Handler uiHandler = new Handler(Looper.getMainLooper());
                //FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
                String fileName = imageUri.getLastPathSegment();
                //String path = "images/"+user.getUid()+"/"+imageUri.getLastPathSegment();
                StorageUtility.setImage(getActivity(), holder.imageView, null, user.getUid(),
                        fileName, imagePath, uiHandler, handler);

            }

        }

        @Override
        public int getItemCount() {
            if(friendsList != null){
                return friendsList.size();
            }
            return 0;
        }

        public class ViewHolder extends RecyclerView.ViewHolder{
            public TextView displayName;
            public TextView status;
            public CheckBox checkBox;
            public ImageView imageView;
            public ViewHolder(View itemView) {
                super(itemView);
                displayName = (TextView) itemView.findViewById(R.id.displayName);
                status = (TextView) itemView.findViewById(R.id.status);
                checkBox = (CheckBox) itemView.findViewById(R.id.usersCheckbox);
                imageView = (ImageView) itemView.findViewById(R.id.userImage);
            }
        }
    }

    private class SelectedFriendsAdapter extends RecyclerView.Adapter<SelectedFriendsAdapter.ViewHolder>{

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.searchusersselectedimages, parent, false);
            Log.d("nCFragment", "onCreateViewHolder");
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {

            Log.d("nCFragment", "onBindViewHolder. position: "+position);
            holder.cancelView.setOnClickListener(new SelectedFriendsClickListener(position));
            User user = selectedFriendsList.get(position);
            String imagePath = user.getImagePath();
            Uri imageUri = Uri.parse(imagePath);
            if(imagePath != null){
                Handler uiHandler = new Handler(Looper.getMainLooper());
                //FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
                String fileName = imageUri.getLastPathSegment();
                //String path = "images/"+user.getUid()+"/"+imageUri.getLastPathSegment();
                StorageUtility.setImage(getActivity(), holder.imageView, null, user.getUid(),
                        fileName, imagePath, uiHandler, handler);

            }
        }

        @Override
        public int getItemCount() {
            Log.d("nCFragment", "getItemCount");
            if(selectedFriendsList != null){
              return selectedFriendsList.size();
            }
            return 0;
        }

        public class ViewHolder extends RecyclerView.ViewHolder{
            public ImageView imageView;
            public ImageView cancelView;
            public ViewHolder(View itemView) {
                super(itemView);
                imageView = (ImageView) itemView.findViewById(R.id.userImage);
                cancelView = (ImageView) itemView.findViewById(R.id.cancel_action);
            }
        }
    }

}
