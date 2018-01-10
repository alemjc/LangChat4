package com.apps.alemjc.langchat4;

import android.content.Context;
import android.os.Bundle;
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
import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnChatFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ChatListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatListFragment extends Fragment {

    private OnChatFragmentInteractionListener mListener;
    private ChatOpener chatOpener;
    private List<Chat> chatList;
    private RecyclerView recyclerView;
    private ChatOpenerListener chatOpenerListener;
    private ChatListAdapter chatListAdapter;
    private DatabaseReference db;


    private ValueEventListener userChangeListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if(isDetached()){
                return;
            }

            if(dataSnapshot.exists()){
                User user = dataSnapshot.getValue(User.class);
                List<String> chatIds = user.getChats();
                if(chatList != null){
                    chatList.clear();
                    chatListAdapter.notifyDataSetChanged();
                }

                if(chatIds != null){
                    for(String chatId:chatIds){
                        getChatParticipants(chatId);
                    }
                }

            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };


    public static ChatListFragment newInstance() {
        ChatListFragment fragment = new ChatListFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    public ChatListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        chatList = new ArrayList<>();
        db = FirebaseDatabase.getInstance().getReference();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.chatList);
        chatListAdapter = new ChatListAdapter();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(chatListAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if(firebaseUser == null){
            //TODO: Do something in case the user is not logged in.
        }

        else{
            db.child("users").child(firebaseUser.getUid()).addValueEventListener(userChangeListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if(firebaseUser != null){
            db.child("users").child(firebaseUser.getUid()).removeEventListener(userChangeListener);
        }

        if(chatList != null){
            int size = chatList.size();
            chatList.clear();
            chatListAdapter.notifyItemRangeRemoved(0, size);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.chat_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == R.id.newChat){
            mListener.onCreateNewChat();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        try {
            mListener = (OnChatFragmentInteractionListener) activity;
            chatOpener = (ChatOpener) activity;
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

    private class ChatOpenerListener implements View.OnClickListener{
        private String chatId;
        public ChatOpenerListener(String chatId){
            this.chatId = chatId;
        }
        @Override
        public void onClick(View view) {
            chatOpener.openChat(chatId);
        }
    }

    private void getChatParticipants(final String chatId){

        Log.d("ChatListFragment", "chatId: "+chatId);
        db.child("chats").child(chatId).child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(isDetached()){
                    return;
                }

                if(dataSnapshot.exists()){

                    Chat chat = new Chat();
                    chat.chatId = chatId;
                    chat.participants = new ArrayList<>();
                    for(DataSnapshot userSnapShot: dataSnapshot.getChildren()){
                        User user = userSnapShot.getValue(User.class);
                        Log.d("ChatListFragment", user.getDisplayName());
                        chat.participants.add(user);
                    }

                    chatList.add(chat);
                    Log.d("ChatListFragment", chatList.toString());
                    Log.d("ChatListFragment", "size: "+chatList.size());
                    //chatListAdapter.notifyItemInserted(chatList.size()-1);
                    chatListAdapter.notifyDataSetChanged();

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private class Chat {
        public List<User> participants;
        public String chatId;
    }

    private class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder>{
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater.inflate(R.layout.chatlayout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            String list = "";
            int count = 0;

            Log.d("ChatListFragment", "position: "+position);

            //Log.d("ChatListFragment", chatList.get(position).participants.toString());
            for(int i = 0; i < chatList.get(position).participants.size(); i++){
                User user = chatList.get(position).participants.get(i);
                //Log.d("ChatListFragment", "user: "+user.getDisplayName());

                list+=user.getDisplayName();
                if(i < chatList.get(position).participants.size()-1){
                    list+=", ";
                }
                count++;
            }

            holder.removeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                    final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                    if(firebaseUser != null){
                        databaseReference.child("users").child(firebaseUser.getUid())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {

                                        if(dataSnapshot.exists()){
                                            final User user = dataSnapshot.getValue(User.class);
                                            List<String> chats = user.getChats();

                                            if(chats == null || chats.isEmpty()) return;

                                            String chatId = chatList.get(position).chatId;
                                            int chatIndex = chats.indexOf(chatId);
                                            chats.remove(chatIndex);
                                            user.setChats(chats);

                                            databaseReference.child("chats").child(chatId).child("users")
                                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(DataSnapshot dataSnapshot1) {
                                                            if(dataSnapshot1.exists()){
                                                                for(DataSnapshot dataSnapshot2: dataSnapshot1.getChildren()){
                                                                    User user1 = dataSnapshot2.getValue(User.class);
                                                                    if(user1.getUid().equals(firebaseUser.getUid())){
                                                                        dataSnapshot2.getRef().removeValue();
                                                                        databaseReference.child("users")
                                                                                .child(firebaseUser.getUid()).setValue(user);
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

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                    }
                }
            });



            if(count >= 2){
                holder.imageGroup.setBackgroundResource(R.drawable.ic_group_black_24dp);
            }

            holder.participantsText.setText(list);
            holder.chatLayout.setOnClickListener(new ChatOpenerListener(chatList.get(position).chatId));

        }

        @Override
        public int getItemCount() {
            if(chatList == null)
                return 0;
            else{
                Log.d("ChatListFragment", "size: "+chatList.size());
                return chatList.size();
            }
        }

        public class ViewHolder extends RecyclerView.ViewHolder{
            public View chatLayout;
            public TextView participantsText;
            public ImageView imageGroup;
            public ImageView removeButton;
            public ViewHolder(View itemView) {
                super(itemView);
                chatLayout = itemView.findViewById(R.id.chatLayout);
                participantsText = (TextView) itemView.findViewById(R.id.chatUsersText);
                imageGroup = (ImageView) itemView.findViewById(R.id.imageGroup);
                removeButton = (ImageView) itemView.findViewById(R.id.removeButton);
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
    public interface OnChatFragmentInteractionListener {
        // TODO: Update argument type and name
        void onCreateNewChat();
    }



}
