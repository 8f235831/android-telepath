package pers.u8f23.telepath.example.fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import pers.u8f23.telepath.IntentNavController;
import pers.u8f23.telepath.IntentNavMethod;
import pers.u8f23.telepath.R;
import pers.u8f23.telepath.example.core.BaseFragment;
import pers.u8f23.telepath.databinding.FragmentHomePageBinding;

import java.util.Map;

/**
 * @author 8f23
 * @create 2023/5/16-13:34
 */
public class HomePageFragment extends BaseFragment<FragmentHomePageBinding>{
	@NonNull @Override
	protected FragmentHomePageBinding viewBindingInflate(LayoutInflater inflater, ViewGroup container){
		return FragmentHomePageBinding.inflate(getLayoutInflater());
	}

	@Override public void onStart(){
		super.onStart();
		binding.goFirstPageButton.setOnClickListener(
			(v) -> Navigation.findNavController(v).navigate(R.id.fragment_first_page)
		);
		binding.goSecondPageButton.setOnClickListener(
			(v) -> Navigation.findNavController(v).navigate(R.id.fragment_second_page)
		);
	}

	@IntentNavMethod (value = "/test", description = "test")
	public static void navi(
		@IntentNavController NavController controller
	){

	}
}
