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
	@NonNull
	protected final Binding binding;

	/**
	 * 构造函数。
	 */
	public BaseViewWidget(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent){
		binding = viewBindingInflate(inflater, parent);
	}

	private BaseViewWidget(@NonNull Binding binding){
		this.binding = binding;
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
	@NonNull
	public final Binding getBinding(){
		return binding;
	}

	/**
	 * 获取View。
	 *
	 * @return View
	 */
	@NonNull
	public final View getView(){
		return binding.getRoot();
	}

	interface BaseViewWidgetBindingInflater<Binding extends ViewBinding>{
		@NonNull
		Binding inflate();
	}

	@NonNull
	public static <Binding extends ViewBinding> BaseViewWidget<Binding> getInstance(
		@NonNull BaseViewWidgetBindingInflater<Binding> inflater
	){
		Binding binding = inflater.inflate();
		return new BaseViewWidget<Binding>(binding){
			// 已通过构造方法参数直接提供binding，因此仅在形式上重写此方法。
			@NonNull @Override
			protected Binding viewBindingInflate(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent){
				//noinspection ConstantConditions
				return null;
			}
		};
	}
}
