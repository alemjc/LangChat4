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
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.*;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SearchFriendsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SearchFriendsFragment extends Fragment {

    private FirebaseUser firebaseUser;
    private RecyclerView recyclerView;
    private List<UserWrapper> userList;
    private Map<String, User> friendsMap;
    private FriendsAdapter friendsAdapter;
    private EditText searchBar;
    private boolean emptyTextEncountered;
    private static final String BG_NAME = "bg name";
    private Handler handler;
    private HandlerThread handlerThread;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.

     * @return A new instance of fragment SearchFriendsFragment.
     */
    public static SearchFriendsFragment newInstance() {
        SearchFriendsFragment fragment = new SearchFriendsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public SearchFriendsFragment() {
        // Required empty public constructor
    }

    private View.OnKeyListener keyListener = new View.OnKeyListener(){
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {

            if(keyCode == KeyEvent.KEYCODE_ENTER){
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                return true;
            }

            // key code was not enter so we let android know we don't process the key event.
            return false;
        }
    };

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            String displayName = editable.toString();
            Log.d("sFriendsFragment","displayName: "+displayName);
            if(displayName.isEmpty()){
                emptyTextEncountered = true;
            }
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
            databaseReference.child("users").orderByChild("displayName").startAt(displayName)
                    .addListenerForSingleValueEvent(new EditTextValueEventListener(displayName));
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        emptyTextEncountered = false;
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search_friends, container, false);
        friendsAdapter = new FriendsAdapter();
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        searchBar = (EditText) view.findViewById(R.id.searchBar);

        recyclerView.setAdapter(friendsAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        searchBar.addTextChangedListener(textWatcher);
        searchBar.setOnKeyListener(keyListener);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        menu.clear();

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        startThreads();
        friendsMap = new HashMap<>();
        final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.child("users").child(firebaseUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if(isDetached()){
                            return;
                        }

                        if(dataSnapshot.exists()){
                            User user = dataSnapshot.getValue(User.class);
                            List<String> friends = user.getFriends();

                            databaseReference.child("users").addListenerForSingleValueEvent(new UsersValueEventListener(friends));
                        }


                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

    }

    @Override
    public void onPause() {
        super.onPause();
        stopThreads();
        if(userList != null){
            int size = userList.size();
            userList.clear();
            userList = null;
            friendsMap = null;
            friendsAdapter.notifyItemRangeRemoved(0, size);
        }
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

    private class AddUserClickListener implements View.OnClickListener{
        private UserWrapper userWrapper;
        private int position;

        public AddUserClickListener(UserWrapper user, int position){
            this.userWrapper = user;
            this.position = position;
        }

        @Override
        public void onClick(final View view) {
            final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

            databaseReference.child("users").child(firebaseUser.getUid()).addListenerForSingleValueEvent(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            if(isDetached()){
                                return;
                            }

                            if(dataSnapshot.exists()){
                                User user = dataSnapshot.getValue(User.class);
                                List<String> users = user.getFriends();
                                if(users == null){
                                    users = new ArrayList<>();
                                    user.setFriends(users);
                                }
                                String nFriend;
                                User user1 = userWrapper.getUser();
                                nFriend = user1.getUid();
                                users.add(nFriend);
                                if(dataSnapshot.getRef() == null){
                                    Log.d("sFriendsFragment","ref is null");
                                }
                                else{
                                    Log.d("sFriendsFragment","ref is not null");
                                }
                                dataSnapshot.getRef().updateChildren(user.toMap())
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if(task.isSuccessful()){
                                                    userList.get(position).setFriended(true);
                                                    friendsAdapter.notifyItemChanged(position);
                                                }
                                            }
                                        });
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    }
            );
        }
    }

    private class EditTextValueEventListener implements ValueEventListener{

        private String displayName;

        public EditTextValueEventListener(String displayName){
            this.displayName = displayName;
        }

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {

            if(isDetached()){
                return;
            }

            if(dataSnapshot.exists()){
                Log.d("sFriendsFragment", "exists");
                if(displayName.isEmpty()){
                    emptyTextEncountered = false;
                }
                userList.clear();
                for(DataSnapshot dataSnapshot1: dataSnapshot.getChildren()){
                    User user = dataSnapshot1.getValue(User.class);
                    UserWrapper userWrapper;
                    Set<String> friendsIds = friendsMap.keySet();
                    if(friendsIds.contains(user.getUid())){
                        userWrapper = new UserWrapper(user, true);
                    }
                    else{
                        userWrapper = new UserWrapper(user, false);
                    }

                    userList.add(userWrapper);
                }

                friendsAdapter.notifyDataSetChanged();
            }
            else{
                Log.d("sFriendsFragment", "does not exists");
                if(!emptyTextEncountered){
                    userList.clear();
                    friendsAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    }

    private class UsersValueEventListener implements ValueEventListener{

        private List<String> friendsIds;

        public UsersValueEventListener(List<String> friendsIds){
            this.friendsIds = friendsIds;
        }

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {

            if(isDetached()){
                return;
            }

            if(dataSnapshot.exists()){
                userList = new ArrayList<>();
                for(DataSnapshot dataSnapshot1:dataSnapshot.getChildren()){
                    User user = dataSnapshot1.getValue(User.class);
                    UserWrapper userWrapper;

                    if(friendsIds != null && friendsIds.contains(user.getUid())){
                        friendsMap.put(user.getUid(), user);
                        userWrapper = new UserWrapper(user, true);
                    }
                    else {
                        userWrapper = new UserWrapper(user, false);
                    }

                    userList.add(userWrapper);
                    friendsAdapter.notifyItemInserted(userList.size()-1);
                }
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    }

    private class UserWrapper{
        private User user;
        private boolean friended;
        public UserWrapper(User user, boolean friended){
            this.user = user;
            this.friended = friended;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public boolean isFriended() {
            return friended;
        }

        public void setFriended(boolean friended) {
            this.friended = friended;
        }
    }

    private class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder>{
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater.inflate(R.layout.searchuserslayout, parent, false);

            recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            UserWrapper userWrapper = userList.get(position);
            User user = userWrapper.getUser();
            String imagePath = user.getImagePath();
            holder.displayName.setText(user.getDisplayName());
            holder.status.setText(user.getStatus());
            if(!userWrapper.isFriended())
                //TODO: Check if adding users as friends works.
                holder.add.setOnClickListener(new AddUserClickListener(userWrapper, position));
            else
                holder.add.setEnabled(false);

            if(imagePath != null){
                Uri imageUri = Uri.parse(imagePath);
                Handler uiHandler = new Handler(Looper.getMainLooper());
                //FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
                String fileName = imageUri.getLastPathSegment();
                //String path = "images/"+user.getUid()+"/"+ fileName;

                StorageUtility.setImage(getActivity(), holder.accountPhoto, null, user.getUid(), fileName, imagePath,
                        uiHandler, handler);
            }

        }

        @Override
        public int getItemCount() {

            if(userList!= null){
                return userList.size();
            }
            return 0;
        }

        public class ViewHolder extends RecyclerView.ViewHolder{
            private TextView displayName;
            private TextView status;
            private ImageButton add;
            private ImageView accountPhoto;
            public ViewHolder(View itemView) {
                super(itemView);
                displayName = (TextView) itemView.findViewById(R.id.displayName);
                status = (TextView) itemView.findViewById(R.id.status);
                add = (ImageButton) itemView.findViewById(R.id.add);
                accountPhoto = (ImageView) itemView.findViewById(R.id.userImage);
            }
        }
    }


}
