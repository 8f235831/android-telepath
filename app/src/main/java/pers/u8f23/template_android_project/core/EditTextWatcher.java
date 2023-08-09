package pers.u8f23.template_android_project.core;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import androidx.annotation.NonNull;

import java.util.function.Consumer;

/**
 * {@link EditText}的绑定{@link TextWatcher}实现。
 * 适用于{@link androidx.recyclerview.widget.RecyclerView}组件中，防止监听事件重复添加。
 *
 * @author 8f23
 * @create 2023/6/27-9:17
 * @see #bind(EditText, Consumer)
 * @see #unbind(EditText)
 */
public final class EditTextWatcher implements TextWatcher{
	private static final int TAG_ID = -352324524;
	@NonNull private final Consumer<String> onChangeListener;

	private EditTextWatcher(@NonNull Consumer<String> onChangeListener){
		this.onChangeListener = onChangeListener;
	}

	@Override public void beforeTextChanged(CharSequence s, int start, int count, int after){

	}

	@Override public void onTextChanged(CharSequence s, int start, int before, int count){
		String str = s == null ? null : s.toString();
		onChangeListener.accept(str);
	}

	@Override public void afterTextChanged(Editable s){

	}

	/**
	 * 调用{@link EditText#addTextChangedListener}，注册监听事件。
	 * 该监听事件会自动移除已通过此方法注册的监听事件，防止重复添加。
	 *
	 * @param editText         需要注册监听的{@link EditText}组件。
	 * @param onChangeListener {@link EditText}组件发生变化的监听事件。
	 * @see #unbind(EditText)
	 */
	public static void bind(@NonNull EditText editText, @NonNull Consumer<String> onChangeListener){
		// remove old watcher.
		unbind(editText);
		EditTextWatcher textWatcher = new EditTextWatcher(onChangeListener);
		editText.addTextChangedListener(textWatcher);
		editText.setTag(TAG_ID, textWatcher);
	}

	/**
	 * 调用{@link EditText#removeTextChangedListener}，移除通过{@link #bind(EditText, Consumer)}注册的监听事件。
	 *
	 * @param editText 需要注销监听的{@link EditText}组件。
	 * @see #bind(EditText, Consumer)
	 */
	public static void unbind(@NonNull EditText editText){
		Object tag = editText.getTag(TAG_ID);
		if (tag instanceof EditTextWatcher) {
			editText.removeTextChangedListener((EditTextWatcher) tag);
		}
	}
}
