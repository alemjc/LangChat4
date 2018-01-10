package com.apps.alemjc.langchat4;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FriendsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FriendsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FriendsFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private RecyclerView recyclerView;
    private List<User> friends;
    private FriendsAdapter friendsAdapter;
    private static final String BG_NAME = "bg name";
    private Handler handler;
    private HandlerThread handlerThread;


    private ValueEventListener userListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if(dataSnapshot.exists()){
                final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                User user = dataSnapshot.getValue(User.class);
                List<String> friendsIDs = user.getFriends();

                if(friends == null){
                    friends = new ArrayList<>();
                }
                else{
                    friends.clear();
                }
                friendsAdapter.notifyDataSetChanged();
                if(friendsIDs == null)
                    return;


                for(String id: friendsIDs){
                    databaseReference.child("users").child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            if(isDetached()){
                                return;
                            }

                            if(dataSnapshot.exists()){
                                User user1 = dataSnapshot.getValue(User.class);
                                friends.add(user1);
                                friendsAdapter.notifyItemInserted(friends.size()-1);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }

            }
            else{
                Log.d("friendsFragment","users does not exists");
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.

     * @return A new instance of fragment FriendsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static FriendsFragment newInstance() {
        FriendsFragment fragment = new FriendsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public FriendsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_friends, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        friendsAdapter = new FriendsAdapter();
        recyclerView.setAdapter(friendsAdapter);
        final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        startThreads();
        Log.d("friendsFragment","onResume");
        if(firebaseUser == null){
            //TODO: it is unlikely for the user not to be logged in at this point, but just in case some action should be taken.
        }
        else{
            databaseReference.child("users").child(firebaseUser.getUid()).addValueEventListener(userListener);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("friendsFragment", "onPause");
        final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if(firebaseUser != null)
            databaseReference.child("users").child(firebaseUser.getUid()).removeEventListener(userListener);

        if(friends != null) {
            friends.clear();
            friendsAdapter.notifyItemRangeRemoved(0, friends.size());
            friendsAdapter = null;
            friends = null;
        }
        stopThreads();
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d("FriendsFragment","onCreateOptionsMenu");
        inflater.inflate(R.menu.friends_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.addUser){
            mListener.onShowSearchFragment();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("friendsFragment", "onStop");
        if(friends != null) {
            friends.clear();
            friendsAdapter.notifyItemRangeRemoved(0, friends.size());
            friendsAdapter = null;
            friends = null;
        }

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
            mListener = (OnFragmentInteractionListener) activity;
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
        void onShowSearchFragment();
    }


    private class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendsAdapterHolder>{

        @Override
        public FriendsAdapterHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            View view = layoutInflater.inflate(R.layout.friends_view_item, parent, false);

            return new FriendsAdapterHolder(view);
        }

        @Override
        public void onBindViewHolder(final FriendsAdapterHolder holder, final int position) {
            User user = friends.get(position);
            String imagePath = user.getImagePath();
            holder.displayName.setText(user.getDisplayName());
            holder.status.setText(user.getStatus());
            Handler uiHandler = new Handler(Looper.getMainLooper());

            holder.removeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                    final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

                    if(firebaseUser != null){
                        databaseReference.child("users").child(firebaseUser.getUid()).addListenerForSingleValueEvent(
                                new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {

                                        if(dataSnapshot.exists()){
                                            User user1 = dataSnapshot.getValue(User.class);
                                            List<String> friendsList = new ArrayList<>();
                                            friends.remove(position);
                                            for(User user2: friends){
                                                friendsList.add(user2.getUid());
                                            }

                                            user1.setFriends(friendsList);
                                            databaseReference.child("users").child(firebaseUser.getUid()).setValue(user1);
                                        }

                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                    }


                }
            });

            if(imagePath != null){
                Uri imageUri = Uri.parse(imagePath);
//                FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
                String fileName = imageUri.getLastPathSegment();
//                String path = "images/"+user.getUid()+"/"+ imageUri.getLastPathSegment();
                StorageUtility.setImage(getActivity(), holder.accountPhoto, null, user.getUid(),
                        fileName, imagePath, uiHandler, handler);

            }
        }

        @Override
        public int getItemCount() {
            if(friends != null)
                return friends.size();
            else
                return 0;
        }

        public class FriendsAdapterHolder extends RecyclerView.ViewHolder{

            public ImageView accountPhoto;
            public ImageView removeButton;
            public TextView displayName;
            public TextView status;

            public FriendsAdapterHolder(View view){
                super(view);

                displayName = (TextView) view.findViewById(R.id.displayName);
                status = (TextView) view.findViewById(R.id.status);
                accountPhoto = (ImageView) view.findViewById(R.id.userImage);
                removeButton = (ImageView) view.findViewById(R.id.removeButton);

            }

        }
    }

}
