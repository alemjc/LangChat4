package com.apps.alemjc.langchat4;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
//import pl.aprilapps.easyphotopicker.DefaultCallback;
//import pl.aprilapps.easyphotopicker.EasyImage;
//import pl.aprilapps.easyphotopicker.EasyImageConfig;

import java.io.*;
import java.util.HashMap;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SettingsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private Button save;
    private EditText displayName;
    private EditText status;
    private ImageView accountPhoto;
    private Uri currentPicFileUri;
    private UserValueEventListener userValueEventListener;
    private static final String BG_NAME = "bg name";
    private Handler handler;
    private HandlerThread handlerThread;

//    private View.OnClickListener setPictureListener = new View.OnClickListener() {
//        @Override
//        public void onClick(View view) {
//            EasyImage.openGallery(SettingsFragment.this, EasyImageConfig.REQ_PICK_PICTURE_FROM_GALLERY);
//        }
//    };

    private View.OnClickListener saveListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if(firebaseUser != null){
                if(currentPicFileUri == null){
                    saveChanges(firebaseUser, displayName.getText().toString(), status.getText().toString(), null);
                }
                else{
                    StorageReference firebaseStorageRef = FirebaseStorage.getInstance().getReference();
                    StorageReference imageRef = firebaseStorageRef.child("images/"+firebaseUser.getUid()+"/" +
                            ""+currentPicFileUri.getLastPathSegment());
                    UploadTask uploadTask = imageRef.putFile(currentPicFileUri);
                    uploadTask.addOnSuccessListener(getActivity(), new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Uri downloadUrl = taskSnapshot.getDownloadUrl();
                            saveChanges(firebaseUser, displayName.getText().toString(), status.getText().toString()
                                    , downloadUrl);
                        }
                    }).addOnFailureListener(getActivity(), new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d("SignUpFragment","failure to store picture");
                            saveChanges(firebaseUser, displayName.getText().toString(), status.getText().toString()
                                    , null);
                        }
                    });
                }
            }

        }
    };

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SettingsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        displayName = (EditText) view.findViewById(R.id.displayName);
        status = (EditText) view.findViewById(R.id.status);
        save = (Button) view.findViewById(R.id.save);
        accountPhoto = (ImageView) view.findViewById(R.id.accountPhoto);
        save.setOnClickListener(saveListener);
        //accountPhoto.setOnClickListener(setPictureListener);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        userValueEventListener = new UserValueEventListener();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        startThreads();

        if(firebaseUser != null){
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

            databaseReference.child("users").child(firebaseUser.getUid()).addValueEventListener(userValueEventListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        databaseReference.removeEventListener(userValueEventListener);
        stopThreads();
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

    private void saveChanges(final FirebaseUser firebaseUser, final String displayName, final String status, final Uri downloadUrl){

        UserProfileChangeRequest.Builder builder = new UserProfileChangeRequest.Builder();

        if(displayName != null){
            builder.setDisplayName(displayName);
        }

        if(downloadUrl != null){
            builder.setPhotoUri(downloadUrl);
        }

        UserProfileChangeRequest userProfileChangeRequest = builder.build();
        firebaseUser.updateProfile(userProfileChangeRequest)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Log.d("signUpFragment","onComplete");

                            firebaseUser.reload().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                                    DatabaseReference userReference = databaseReference.child("users")
                                            .child(firebaseUser.getUid());

                                    HashMap<String, Object> updates = new HashMap<>();
                                    updates.put("displayName", displayName);
                                    updates.put("status", status);
                                    if(downloadUrl != null){
                                        String imagePath = downloadUrl.getPath();
                                        updates.put("imagePath", imagePath);
                                    }

                                    userReference.updateChildren(updates, new DatabaseReference.CompletionListener() {
                                        @Override
                                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                            Toast.makeText(getActivity(), "User details updated", Toast.LENGTH_LONG)
                                                    .show();
                                        }
                                    });

                                }
                            });

                        }
                    }
                });



    }

    private class UserValueEventListener implements ValueEventListener{
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {

            if(isDetached()){
                return;
            }

            if(dataSnapshot.exists()) {
                Handler uiHandler = new Handler(Looper.getMainLooper());
                User user = dataSnapshot.getValue(User.class);
                String displayName = user.getDisplayName();
                String status = user.getStatus();
                String imagePath = user.getImagePath();

                SettingsFragment.this.displayName.setText(displayName);
                SettingsFragment.this.status.setText(status);

                if (imagePath != null) {
                    Uri imageUri = Uri.parse(imagePath);
                    //FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
                    String fileName = imageUri.getLastPathSegment();
                    //String path = "images/" + user.getUid() + "/" + imageUri.getLastPathSegment();
                    StorageUtility.setImage(getActivity(), accountPhoto, null, user.getUid(),
                            fileName, imagePath, uiHandler, handler);
                }

            }

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

//        EasyImage.handleActivityResult(requestCode, resultCode, data, getActivity(), new DefaultCallback() {
//            @Override
//            public void onImagePicked(File file, EasyImage.ImageSource imageSource, int i) {
//                if(file.exists()){
//                    InputStream inputStream = null;
//                    try{
//
//                        inputStream = new FileInputStream(file);
//                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
//                        accountPhoto.setImageBitmap(Bitmap.createScaledBitmap(bitmap, accountPhoto.getWidth(),
//                                accountPhoto.getHeight(), false));
//                        currentPicFileUri = Uri.fromFile(file);
//
//                    }
//                    catch (FileNotFoundException e){
//                        Log.d("SignupFragment", e.getMessage());
//                    }
//                    finally {
//                        if(inputStream != null){
//                            try{
//                                inputStream.close();
//                            }
//                            catch (IOException e){
//                                Log.d("SignupFragment", e.getMessage());
//                            }
//
//                        }
//                    }
//
//                }
//            }
//        });
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
//        try {
//            mListener = (OnFragmentInteractionListener) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
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
       void onFragmentInteraction(Uri uri);
    }

}
