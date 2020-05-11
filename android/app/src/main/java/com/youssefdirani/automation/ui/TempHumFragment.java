package com.youssefdirani.automation.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.youssefdirani.automation.R;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

//import androidx.lifecycle.ViewModelProvider; //fine
//import androidx.lifecycle.ViewModelProviders; //deprecated so replaced by the fine one

public class TempHumFragment extends Fragment { //in principle, this fragment represents the charcoal humidity and temperature panel.

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_temphum, container, false);

        return root;
    }

}