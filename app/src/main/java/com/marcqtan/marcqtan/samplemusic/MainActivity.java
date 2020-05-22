package com.marcqtan.marcqtan.samplemusic;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.MenuItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Created by Marc Q. Tan on 17/04/2020.
 */

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    OnFragmentReselected onFragmentReselected;

    public interface OnFragmentReselected {
        void onReselect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_view);

        bottomNavigationView.setSelectedItemId(R.id.music);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment f = null;
                switch (item.getItemId()) {
                    case R.id.about:
                        f = AboutFragment.newInstance();
                        break;
                    case R.id.music:
                        f = MusicFragment.newInstance();
                        break;
                }

                openFragment(f);
                return true;
            }
        });

        bottomNavigationView.setOnNavigationItemReselectedListener(new BottomNavigationView.OnNavigationItemReselectedListener() {
            @Override
            public void onNavigationItemReselected(@NonNull MenuItem item) {
                if(onFragmentReselected != null) {
                    onFragmentReselected.onReselect();
                }
            }
        });
        openFragment(MusicFragment.newInstance());
    }

    public void setOnFragmentReselectedListener(OnFragmentReselected listener) {
        onFragmentReselected = listener;
    }

    private void openFragment(Fragment f) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, f);
        ft.commit();
    }
}
