package pers.u8f23.template_android_project.fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;

import pers.u8f23.template_android_project.core.BaseFragment;
import pers.u8f23.template_android_project.databinding.FragmentSecondPageBinding;

/**
 * @author 8f23
 * @create 2023/5/16-13:46
 */
public class SecondPageFragment extends BaseFragment<FragmentSecondPageBinding>{


	@NonNull @Override
	protected FragmentSecondPageBinding viewBindingInflate(LayoutInflater inflater, ViewGroup container){
		return FragmentSecondPageBinding.inflate(getLayoutInflater());
	}

	@Override public void onStart(){
		super.onStart();
		binding.backButton.setOnClickListener(
			(v) -> Navigation.findNavController(v).navigateUp()
		);
	}
}
