package com.marcqtan.marcqtan.samplemusic;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.MenuItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.marcqtan.marcqtan.samplemusic.databinding.ActivityMainBinding;
import com.marcqtan.marcqtan.samplemusic.fragments.AboutFragment;
import com.marcqtan.marcqtan.samplemusic.fragments.MusicFragment;

/**
 * Created by Marc Q. Tan on 17/04/2020.
 */

public class MainActivity extends AppCompatActivity {
    OnFragmentReselected onFragmentReselected;

    public interface OnFragmentReselected {
        void onReselect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomView.setSelectedItemId(R.id.music);

        binding.bottomView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
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

        binding.bottomView.setOnNavigationItemReselectedListener(new BottomNavigationView.OnNavigationItemReselectedListener() {
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

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private void openFragment(Fragment f) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.container, f);
        ft.commit();
    }
}
