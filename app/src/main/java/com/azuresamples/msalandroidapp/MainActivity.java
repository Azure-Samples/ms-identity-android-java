package com.azuresamples.msalandroidapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;

import android.view.MenuItem;
import android.view.View;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;


import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OnFragmentInteractionListener{

    enum AppFragment {
        SingleAccount,
        MultipleAccount
    }

    private AppFragment mCurrentFragment;

    private ConstraintLayout mContentMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContentMain = findViewById(R.id.content_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        //Set default fragment
        navigationView.setCheckedItem(R.id.nav_single_account);
        setCurrentFragment(AppFragment.SingleAccount);
    }

    @Override
    public boolean onNavigationItemSelected(final MenuItem item) {
        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) { }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) { }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                // Handle navigation view item clicks here.
                int id = item.getItemId();

                if (id == R.id.nav_single_account) {
                    setCurrentFragment(AppFragment.SingleAccount);
                }

                if (id == R.id.nav_multiple_account) {
                    setCurrentFragment(AppFragment.MultipleAccount);
                }

                drawer.removeDrawerListener(this);
            }

            @Override
            public void onDrawerStateChanged(int newState) { }
        });

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setCurrentFragment(final AppFragment newFragment){
        if (newFragment == mCurrentFragment) {
            return;
        }

        mCurrentFragment = newFragment;
        setHeaderString(mCurrentFragment);
        displayFragment(mCurrentFragment);
    }

    private void setHeaderString(final AppFragment fragment){
        switch (fragment) {
            case SingleAccount:
                getSupportActionBar().setTitle("Single Account Mode");
                return;

            case MultipleAccount:
                getSupportActionBar().setTitle("Multiple Account Mode");
                return;
        }
    }

    private void displayFragment(final AppFragment fragment){
        switch (fragment) {
            case SingleAccount:
                attachFragment(new SingleAccountModeFragment());
                return;

            case MultipleAccount:
                attachFragment(new MultipleAccountModeFragment());
                return;
        }
    }

    private void attachFragment(final Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(mContentMain.getId(),fragment)
                .commit();
    }
}
