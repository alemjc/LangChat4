package com.apps.alemjc.langchat4;
import android.*;
import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.*;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnPagingFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PagingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PagingFragment extends Fragment {

    private OnPagingFragmentInteractionListener mListener;
    private ViewPager viewPager;
    private TabLayout tabLayout;
    private FPagerAdapter fPagerAdapter;
    private final int NUM_TABS = 3;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    };


    private ViewPager.SimpleOnPageChangeListener viewPagerPageChangeListener = new ViewPager.SimpleOnPageChangeListener(){
        @Override
        public void onPageSelected(int position) {
            TabLayout.Tab tab = tabLayout.getTabAt(position);
            Log.d("pagingFragment","viewPager: "+position);
            if(tab != null)
                tab.select();
        }
    };

    private TabLayout.OnTabSelectedListener tabSelectedListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            int position = tab.getPosition();
            Log.d("pFragment","tabLayoutposition: "+position);
            viewPager.setCurrentItem(position);
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };


    // TODO: Rename and change types and number of parameters
    public static PagingFragment newInstance() {
        PagingFragment fragment = new PagingFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public PagingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("pFragment","onResume");
        Activity thisActivity = getActivity();

        if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            // Should we show an explanation?

            if (ActivityCompat.shouldShowRequestPermissionRationale(thisActivity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(thisActivity,
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }

        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("pFragment","onPause");

    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("pFragment","onStop");

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d("PagingFragment", "hey");
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Log.d("PagingFragment","hey 2");
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_paging, container, false);
        Log.d("pFragment","onCreate");
        int [] icons = {R.drawable.ic_person_black_24dp, R.drawable.ic_chat_bubble_black_24dp, R.drawable.ic_settings_black_24dp};

        viewPager = (ViewPager) view.findViewById(R.id.pager);
        tabLayout = (TabLayout) view.findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(tabSelectedListener);
        viewPager.addOnPageChangeListener(viewPagerPageChangeListener);
        fPagerAdapter = new FPagerAdapter(getChildFragmentManager());
        viewPager.setAdapter(fPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
        for(int i = 0; i < tabLayout.getTabCount(); i++){
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if(tab != null)
                tab.setIcon(icons[i]);
        }


        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_EXTERNAL_STORAGE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                fPagerAdapter.notifyDataSetChanged();
            }
            else{
                /*
                 * TODO: User did not give permissions to app to reach to external storage.
                 * show disable features that need this permission or tell user why we need this.
                 */
            }
        }
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
//        try {
//            mListener = (OnPagingFragmentInteractionListener) activity;
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

    private class FPagerAdapter extends FragmentStatePagerAdapter {
        final private Fragment [] fragments = {new FriendsFragment(), new ChatListFragment(), new SettingsFragment()};;
        final private String[] titles = {"Friends","Chat", "Settings"};

        public FPagerAdapter(FragmentManager fragmentManager){
            super(fragmentManager);
        }

        @Override
        public android.support.v4.app.Fragment getItem(int i) {
            Log.d("pFragment", "getting item: "+i);
            return fragments[i];
        }

        @Override
        public int getCount() {
            return NUM_TABS;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
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
    public interface OnPagingFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

}
