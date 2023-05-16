package pers.u8f23.template_android_project;

import android.view.LayoutInflater;

import pers.u8f23.template_android_project.core.BaseActivity;
import pers.u8f23.template_android_project.databinding.ActivityMainBinding;

public class MainActivity extends BaseActivity<ActivityMainBinding>{

	@Override protected ActivityMainBinding viewBindingInflate(LayoutInflater inflater){
		return ActivityMainBinding.inflate(getLayoutInflater());
	}
}