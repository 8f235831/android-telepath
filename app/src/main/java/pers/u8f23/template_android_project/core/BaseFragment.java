package pers.u8f23.template_android_project.core;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import lombok.Getter;

/**
 * @author 8f23
 * @create 2023/5/16-12:38
 */
public abstract class BaseFragment<Binding extends ViewBinding> extends Fragment{
	/** 组件ViewBinding */
	@Getter
	protected Binding binding;

	@Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
		@Nullable Bundle savedInstanceState){
		return inflateView(inflater, container);
	}

	/**
	 * 构建ViewBinding。
	 *
	 * @param inflater  inflater
	 * @param container 容器
	 * @return ViewBinding
	 */
	@NonNull
	protected abstract Binding viewBindingInflate(LayoutInflater inflater, ViewGroup container);

	/** 填充内容。 */
	protected View inflateView(LayoutInflater inflater, ViewGroup container){
		binding = viewBindingInflate(inflater, container);
		return binding.getRoot();
	}

	/**
	 * 销毁组件。
	 */
	@Override
	public void onDestroyView(){
		super.onDestroyView();
		this.binding = null;
	}
}
