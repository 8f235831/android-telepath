package pers.u8f23.telepath.example;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.multidex.MultiDex;
import pers.u8f23.telepath.BuildConfig;

import java.lang.ref.WeakReference;

/**
 * @author 8f23
 * @create 2023/5/16-12:29
 */
public class MyApp extends Application{
	private static WeakReference<MyApp> instance;

	@Override
	protected void attachBaseContext(Context base){
		super.attachBaseContext(base);
		MultiDex.install(this);
	}

	@Override
	public void onCreate(){
		super.onCreate();
		instance = new WeakReference<>(this);
		initLibs();
	}

	private void initLibs(){}

	/**
	 * @return 当前app是否是调试开发模式
	 */
	public static boolean isDebug(){
		return BuildConfig.DEBUG;
	}

	/**
	 * @return 返回当前Application实例，可能返回null（通常不会）。
	 * @see #requireInstance()
	 */
	@Nullable
	public static MyApp getInstance(){
		return instance.get();
	}

	/**
	 * @return 返回当前Application实例，不存在时抛出异常，不会返回null。
	 * @see #getInstance()
	 */
	@NonNull
	public static MyApp requireInstance(){
		MyApp app = instance.get();
		if (app == null) {
			throw new NullPointerException();
		}
		return app;
	}
}
