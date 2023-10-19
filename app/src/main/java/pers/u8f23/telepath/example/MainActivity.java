package pers.u8f23.telepath.example;

import android.view.LayoutInflater;

import pers.u8f23.telepath.example.core.BaseActivity;
import pers.u8f23.telepath.databinding.ActivityMainBinding;

public class MainActivity extends BaseActivity<ActivityMainBinding>{

	@Override protected ActivityMainBinding viewBindingInflate(LayoutInflater inflater){
		return ActivityMainBinding.inflate(getLayoutInflater());
	}
}