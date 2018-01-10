package com.apps.alemjc.langchat4;
import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.v4.app.FragmentTransaction;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class MainActivity extends FragmentActivity implements LoginFragment.OnLoginFragmentInteractionListener,
        SocialLoginFragment.OnSignUpFragmentInteractionListener, FriendsFragment.OnFragmentInteractionListener,
        ChatListFragment.OnChatFragmentInteractionListener, ChatOpener{
    private FragmentManager fragmentManager;
    private ProgressBar progressBar;
    private FirebaseUser firebaseUser;
    private View loginAndLogoFragmentSpace;


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        MultiDex.install(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolBar);
        setActionBar(toolbar);


        fragmentManager = getSupportFragmentManager();
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        loginAndLogoFragmentSpace = findViewById(R.id.space);
        new LoadingIntroAsyncTask().execute((Object)null);

        //TODO: set the loading and logging space
    }

    public void showProgressBar(boolean show){
        if(show){
            progressBar.setVisibility(View.VISIBLE);
        }
        else{
            progressBar.setVisibility(View.INVISIBLE);
        }
    }



    @Override
    public void onLogin(FirebaseUser firebaseUser) {
        Toast.makeText(this, "user logged in!", Toast.LENGTH_LONG).show();
        this.firebaseUser = firebaseUser;
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.space, new PagingFragment());
        fragmentTransaction.commit();
    }

    @Override
    public void onNavigateToSignUp() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.space, new SignupFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public void onShowSearchFragment() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.space, new SearchFriendsFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        //invalidateOptionsMenu();

    }

    @Override
    public void onCreateNewChat() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.space, new NewChatFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    @Override
    public void openChat(String chatId) {
        if(chatId != null){
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.space, ChatFragment.newInstance(chatId));
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        }
    }

    @Override
    public void onUserCreated(FirebaseUser firebaseUser) {
        Toast.makeText(this, "user created!", Toast.LENGTH_LONG).show();
        this.firebaseUser = firebaseUser;
        Log.d("mainActivity", "user photo url: "+firebaseUser.getPhotoUrl());
        String imagePath = (firebaseUser.getPhotoUrl() == null)? null:firebaseUser.getPhotoUrl().toString();
        UserService.startActionCreate(this, firebaseUser.getUid(), new User(firebaseUser.getUid(), imagePath
                ,firebaseUser.getDisplayName(),"", new ArrayList<String>(), new ArrayList<String>()));
        for(int i = 0; i < fragmentManager.getBackStackEntryCount(); i++){
            fragmentManager.popBackStackImmediate();
        }
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.space, new PagingFragment());
        fragmentTransaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        Log.d("MainActivity","onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class LoadingIntroAsyncTask extends AsyncTask<Object, Integer, Object>{

        @Override
        protected Object doInBackground(Object... objects) {

//            try{
//                Thread.sleep(10000);
//            }
//            catch(InterruptedException e){
//                Log.d("loadingExcept", e.getMessage());
//            }

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            showProgressBar(false);
            // TODO: depending on whether the user is logged in or not, show the next view.
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            if(firebaseUser == null){
                // TODO: User is not logged it. Go to log in fragment.
                fragmentTransaction.replace(R.id.space, new SocialLoginFragment());
                fragmentTransaction.commit();
            }
            else{
                //TODO: User is logged in. make the ViewPager visible.
                Log.d("mainActivity", "photo uri: "+firebaseUser.getPhotoUrl());
                MainActivity.this.firebaseUser = firebaseUser;
                fragmentTransaction.replace(R.id.space, new PagingFragment());
                fragmentTransaction.commit();
            }


        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            //TODO: Might want to show something to let user know we are working.
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //TODO: Need to check whether user is logged in or not. also might need to set logo view.
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.space, new LogoFragment());
            fragmentTransaction.commit();
            showProgressBar(true);

        }
    }
}
