package com.apps.alemjc.langchat4;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;
import com.google.firebase.database.*;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class UserService extends IntentService {
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_UPDATE = "com.chat.langchat.app.action.Update";
    private static final String ACTION_CREATE = "com.chat.langchat.app.action.Create";

    private static final String EXTRA_USER = "com.chat.langchat.app.extra.USER";
    private static final String EXTRA_USER_ID = "com.chat.langchat.app.extra.USER_ID";

    private DatabaseReference databaseReference;

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionCreate(Context context, String uid, User user) {
        Intent intent = new Intent(context, UserService.class);
        intent.setAction(ACTION_CREATE);
        intent.putExtra(EXTRA_USER, user);
        intent.putExtra(EXTRA_USER_ID, uid);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionUpdate(Context context, String uid, String user) {
        Intent intent = new Intent(context, UserService.class);
        intent.setAction(ACTION_UPDATE);
        intent.putExtra(EXTRA_USER, user);
        intent.putExtra(EXTRA_USER_ID, uid);
        context.startService(intent);
    }

    public UserService() {
        super("UserService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_CREATE.equals(action)) {
                final User user = (User)intent.getSerializableExtra(EXTRA_USER);
                final String uid = intent.getStringExtra(EXTRA_USER_ID);

                handleActionCreate(uid, user);
            } else if (ACTION_UPDATE.equals(action)) {
                final User user = (User)intent.getSerializableExtra(EXTRA_USER);
                final String uid = intent.getStringExtra(EXTRA_USER_ID);
                handleActionUpdate(uid, user);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionUpdate(String uid, User user) {

        DatabaseReference userRef = databaseReference.child("users").child(uid);
        if(userRef != null){

            userRef.updateChildren(user.toMap());

        }

    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionCreate(final String uid, final User user) {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.child("users").child(uid).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Log.d("userService", "data exists: "+dataSnapshot.exists());
                        Log.d("userService", "user null: "+(user == null));

                        if(!dataSnapshot.exists()){
                            databaseReference.child("users").child(uid).setValue(user);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                }
        );
    }
}
