package pers.u8f23.template_android_project.core;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

/**
 * @author 8f23
 * @create 2023/5/16-14:40
 */
public abstract class BaseViewWidget<Binding extends ViewBinding>{
	/** 布局Binding。 */
	protected final Binding binding;

	/**
	 * 构造函数。
	 */
	public BaseViewWidget(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent){
		binding = viewBindingInflate(inflater, parent);
	}

	/**
	 * 构建ViewBinding。
	 *
	 * @param inflater inflater
	 * @return ViewBinding
	 */
	@NonNull
	protected abstract Binding viewBindingInflate(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent);

	/**
	 * 界面生成完成后执行的方法，在此对界面自定义。
	 */
	public void show(){

	}

	/**
	 * 获取Binding。
	 *
	 * @return Binding
	 */
	public Binding getBinding(){
		return binding;
	}

	/**
	 * 获取View。
	 *
	 * @return View
	 */
	public View getView(){
		return binding.getRoot();
	}
}
