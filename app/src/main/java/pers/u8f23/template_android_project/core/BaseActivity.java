package pers.u8f23.template_android_project.core;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;

import lombok.Getter;

/**
 * Activity基类。
 *
 * @author 8f23
 * @create 2023/5/16-12:34
 */
public abstract class BaseActivity<Binding extends ViewBinding> extends AppCompatActivity{
	/** 组件ViewBinding */
	@Getter
	protected Binding binding;

	/**
	 * 构建ViewBinding
	 *
	 * @param inflater inflater
	 * @return ViewBinding
	 */
	protected abstract Binding viewBindingInflate(LayoutInflater inflater);

	/**
	 * 创建页面。
	 */
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		binding = viewBindingInflate(getLayoutInflater());
		View rootView = binding.getRoot();
		setContentView(rootView);
	}

	/**
	 * 销毁页面。
	 */
	@Override
	protected void onDestroy(){
		super.onDestroy();
		this.binding = null;
	}
}
