package com.apps.alemjc.langchat4;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
//import pl.aprilapps.easyphotopicker.DefaultCallback;
//import pl.aprilapps.easyphotopicker.EasyImage;
//import pl.aprilapps.easyphotopicker.EasyImageConfig;

import java.io.*;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnSignUpFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SignupFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SignupFragment extends Fragment {

    private OnSignUpFragmentInteractionListener mListener;
    private FirebaseAuth firebaseAuth;
    private EditText displayName;
    private EditText username;
    private EditText password;
    private EditText confirmedPassword;
    private ImageView accountPhoto; //TODO
    private Uri accountUri;
    private View picGroup; //TODO
    private Button signUp;
    private boolean clickedSignup;


    private FirebaseAuth.AuthStateListener authStateListener = new FirebaseAuth.AuthStateListener() {
        @Override
        public void onAuthStateChanged(@NonNull final FirebaseAuth firebaseAuth) {
            final FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
            //User is created and logged in
            if(firebaseUser != null){
                if(clickedSignup){

                    if(accountUri == null){
                        finalizeSignUp(firebaseUser, displayName.getText().toString(), null);
                    }
                    else{
                        StorageReference firebaseStorageRef = FirebaseStorage.getInstance().getReference();
                        StorageReference imageRef = firebaseStorageRef.child("images/"+firebaseUser.getUid()+"/" +
                                ""+accountUri.getLastPathSegment());
                        UploadTask uploadTask = imageRef.putFile(accountUri);
                        uploadTask.addOnSuccessListener(getActivity(), new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                finalizeSignUp(firebaseUser, displayName.getText().toString(), downloadUrl);
                            }
                        }).addOnFailureListener(getActivity(), new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d("SignUpFragment","failure to store picture");
                                finalizeSignUp(firebaseUser, displayName.getText().toString(), null);
                            }
                        });

                    }

                }
            }
            //User could not be created
            else{
                if(clickedSignup){
                    Toast.makeText(getActivity(), R.string.userCreationFailedMessage, Toast.LENGTH_LONG).show();
                }
            }
        }
    };


    private View.OnClickListener signUpListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String userName = username.getText().toString();
            String password = SignupFragment.this.password.getText().toString();
            String confirmPassword = confirmedPassword.getText().toString();
            String displayNameText = displayName.getText().toString();
            clickedSignup = true;

            if(userName.isEmpty() || displayNameText.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()){
                Toast.makeText(getActivity(), "Please fill out all fields", Toast.LENGTH_LONG).show();
            }
            else if(!password.equals(confirmPassword)){
                Toast.makeText(getActivity(), "Password fields don't match", Toast.LENGTH_LONG).show();
            }
            else{
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                signUpUser(userName, password);
            }
        }
    };

//    private View.OnClickListener setPictureListener = new View.OnClickListener() {
//        @Override
//        public void onClick(View view) {
//            EasyImage.openGallery(SignupFragment.this, EasyImageConfig.REQ_PICK_PICTURE_FROM_GALLERY);
//        }
//    };



    public static SignupFragment newInstance() {
        SignupFragment fragment = new SignupFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public SignupFragment() {
        // Required empty public constructor
    }

    private void signUpUser(String username, String password){
        firebaseAuth.createUserWithEmailAndPassword(username, password)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d("firebaseAuthComplt", "createUserWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.

                        if (!task.isSuccessful() && clickedSignup) {
//                            Toast.makeText(getActivity(), R.string.userCreationFailedMessage,
//                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(getActivity(), new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("onFailure", e.getMessage());
                        Toast.makeText(getActivity(), e.getMessage(),
                                Toast.LENGTH_SHORT).show();

                    }
                });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        clickedSignup = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View layoutView = inflater.inflate(R.layout.fragment_signup, container, false);
        username = (EditText) layoutView.findViewById(R.id.username);
        password = (EditText) layoutView.findViewById(R.id.password);
        picGroup = layoutView.findViewById(R.id.picGroup);
        accountPhoto = (ImageView) layoutView.findViewById(R.id.accountPhoto);
        confirmedPassword = (EditText) layoutView.findViewById(R.id.confirmPassword);
        displayName = (EditText) layoutView.findViewById(R.id.displayName);
        signUp = (Button) layoutView.findViewById(R.id.signup);
        signUp.setOnClickListener(signUpListener);
//        picGroup.setOnClickListener(setPictureListener);
        return layoutView;
    }

    @Override
    public void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        firebaseAuth.removeAuthStateListener(authStateListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        firebaseAuth.removeAuthStateListener(authStateListener);
    }

    private void finalizeSignUp(final FirebaseUser firebaseUser, String displayName, Uri picture){
        UserProfileChangeRequest.Builder builder = new UserProfileChangeRequest.Builder();

        if(displayName != null){
            builder.setDisplayName(displayName);
        }

        if(picture != null){
            builder.setPhotoUri(picture);
        }

        UserProfileChangeRequest userProfileChangeRequest = builder.build();
        firebaseUser.updateProfile(userProfileChangeRequest)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Log.d("signUpFragment","onComplete");
                            clickedSignup = false;

                            firebaseUser.reload().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(mListener != null)
                                        mListener.onUserCreated(firebaseUser);
                                }
                            });

                        }
                    }
                });


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

//        EasyImage.handleActivityResult(requestCode, resultCode, data, getActivity(), new DefaultCallback() {
//            @Override
//            public void onImagePicked(File file, EasyImage.ImageSource imageSource, int i) {
//                Log.d("SignUpFragment", "Image was picked");
//                if(file.exists()){
//                    InputStream inputStream = null;
//                    try{
//
//                        inputStream = new FileInputStream(file);
//                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
//                        accountPhoto.setImageBitmap(Bitmap.createScaledBitmap(bitmap, accountPhoto.getWidth(),
//                                accountPhoto.getHeight(), false));
//                        accountUri = Uri.fromFile(file);
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
//
//            @Override
//            public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {
//                super.onImagePickerError(e, source, type);
//                Log.d("SignUpFragment", "Error picking image");
//            }
//
//            @Override
//            public void onCanceled(EasyImage.ImageSource source, int type) {
//                super.onCanceled(source, type);
//                Log.d("SignUpFragment", "User canceled image selection");
//            }
//        });
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        try {
            mListener = (OnSignUpFragmentInteractionListener) activity;
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
    public interface OnSignUpFragmentInteractionListener {
        // TODO: Update argument type and name
        void onUserCreated(FirebaseUser firebaseUser);
    }

}
