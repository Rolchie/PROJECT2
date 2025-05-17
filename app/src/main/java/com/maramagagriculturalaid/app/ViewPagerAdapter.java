package com.maramagagriculturalaid.app;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.maramagagriculturalaid.app.Login.BarangayLogin;
import com.maramagagriculturalaid.app.Login.MunicipalLogin;

public class ViewPagerAdapter extends FragmentStateAdapter {
    public ViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle
    lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if(position == 1) {
            return new MunicipalLogin();
        }
        return new BarangayLogin();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
