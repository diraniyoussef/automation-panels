package com.youssefdirani.automation.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.youssefdirani.automation.R;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class WaterHeaterFragment extends Fragment {

    private View root;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        root = inflater.inflate(R.layout.fragment_waterheater, container, false);


        return root;
    }

}