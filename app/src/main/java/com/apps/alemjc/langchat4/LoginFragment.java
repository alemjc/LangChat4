package com.apps.alemjc.langchat4;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.annotation.NonNull;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnLoginFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link LoginFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LoginFragment extends Fragment {


    private OnLoginFragmentInteractionListener mListener;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener = new FirebaseAuth.AuthStateListener() {
        @Override
        public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
            Log.d("authStateChng","Entered");

            if(firebaseUser != null){
                //TODO: user is logged in. send credentials to main activity.
                mListener.onLogin(firebaseUser);
            }
            else{
                //TODO: could not log in user.
                if(clickedLogin)
                    Toast.makeText(getActivity(), R.string.userLoginFailedMessage, Toast.LENGTH_LONG).show();
                else{
                    clickedLogin = false;
                }

            }
        }
    };

    private Button login;
    private boolean clickedLogin;
    private EditText username;
    private EditText password;
    private TextView createAccountText;

    private View.OnClickListener goToSignUpListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            username.getText().clear();
            password.getText().clear();
            mListener.onNavigateToSignUp();
        }
    };


    private View.OnClickListener loginInListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d("LogInClick","loggin user in");
            String userName = username.getText().toString();
            String password = LoginFragment.this.password.getText().toString();
            clickedLogin = true;
            Log.d("LogInClick","username: "+userName);
            Log.d("LogInClick","password: "+password);
            if(userName.isEmpty() || password.isEmpty()){
                Toast.makeText(getActivity(), "Please fill out all fields", Toast.LENGTH_LONG).show();
            }
            else{
                Log.d("LogInClick","calling loginUser");
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                loginUser(userName, password);

            }

            //View focused = getActivity().getCurrentFocus();


        }
    };


    public static LoginFragment newInstance() {
        LoginFragment fragment = new LoginFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public LoginFragment() {
        // Required empty public constructor
    }

    private void setCreateAccountText(boolean underLinedLink){
        String createAccountString = getString(R.string.firebaseCreateAccountText);
        if(!underLinedLink){
            createAccountText.setText(createAccountString);
            createAccountText.setClickable(false);
        }
        else{
            SpannableString underlined = new SpannableString(createAccountString);
            underlined.setSpan(new UnderlineSpan(), 0, createAccountString.length(), 0);
            createAccountText.setTextColor(getResources().getColor(R.color.textLinkColor));
            createAccountText.setText(underlined);
            createAccountText.setClickable(true);
        }
    }


    private void loginUser(String userName, String password){

        firebaseAuth.signInWithEmailAndPassword(userName, password)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d("firebaseAuthComplt", "loginUserWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful() && clickedLogin) {
                            Toast.makeText(getActivity(), R.string.userLoginFailedMessage,
                                    Toast.LENGTH_SHORT).show();
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
        clickedLogin = false;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragmentLayoutView = inflater.inflate(R.layout.fragment_login, container, false);
        createAccountText = (TextView) fragmentLayoutView.findViewById(R.id.createAccountTextView);
        username = (EditText) fragmentLayoutView.findViewById(R.id.username);
        password = (EditText) fragmentLayoutView.findViewById(R.id.password);
        login = (Button) fragmentLayoutView.findViewById(R.id.fbLoginButton);
        setCreateAccountText(true);
        login.setOnClickListener(loginInListener);
        createAccountText.setOnClickListener(goToSignUpListener);

        return fragmentLayoutView;
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        firebaseAuth.removeAuthStateListener(authStateListener);
    }

    @Override
    public void onStop(){
        super.onStop();
        firebaseAuth.removeAuthStateListener(authStateListener);
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        try {
            mListener = (OnLoginFragmentInteractionListener) activity;
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
    public interface OnLoginFragmentInteractionListener {
        void onLogin(FirebaseUser firebaseUser);
        void onNavigateToSignUp();

    }

}
