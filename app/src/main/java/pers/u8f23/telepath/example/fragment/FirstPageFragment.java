package pers.u8f23.telepath.example.fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;

import pers.u8f23.telepath.example.core.BaseFragment;
import pers.u8f23.telepath.databinding.FragmentFirstPageBinding;

/**
 * @author 8f23
 * @create 2023/5/16-13:43
 */
public class FirstPageFragment extends BaseFragment<FragmentFirstPageBinding>{
	@NonNull @Override
	protected FragmentFirstPageBinding viewBindingInflate(LayoutInflater inflater, ViewGroup container){
		return FragmentFirstPageBinding.inflate(getLayoutInflater());
	}

	@Override public void onStart(){
		super.onStart();
		binding.backButton.setOnClickListener(
			(v) -> Navigation.findNavController(v).navigateUp()
		);
	}
}
