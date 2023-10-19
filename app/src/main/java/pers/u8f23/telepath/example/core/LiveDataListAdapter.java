package pers.u8f23.telepath.example.core;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 基于{@link ListAdapter}的实现，集成了{@link ViewBinding}和{@link LiveData}，性能不错<s>，并且应该没有bug</s>。
 * 使用Builder模式构造<s>，现在再也不用写子类实现了</s>。示例用法如下：
 * <h2>示例代码</h2>
 * <h3>实体类 {@code ExampleItem}</h3>
 * <pre><code>
 * &#064;Getter &#064;Setter
 * &#064;NoArgsConstructor &#064;AllArgsConstructor
 * public class ExampleItem{
 *   private String id;
 *   private String desc;
 *
 *   // 判断两项是否为同一项。
 *   public static boolean areSame(ExampleItem a, ExampleItem b){
 *     return a != null
 *       && b != null
 *       && a.id != null
 *       && a.id.equals(b.id);
 *   }
 *
 *   // 判断两项是否完全一致。
 *   public static boolean areEqual(ExampleItem a,ExampleItem b){
 *     return a != null
 *       && b != null
 *       && a.id != null
 *       && a.id.equals(b.id)
 *       && a.desc!=null
 *       && a.desc.equals(b.desc);
 *   }
 * }
 * </code></pre>
 * <hr/>
 * <h3>ViewModel实现 {@code ExampleViewModel}</h3>
 * <pre><code>
 * public class ExampleViewModel extends ViewModel{
 *   &#064;Getter
 *   private final MutableLiveData&lt;List&lt;ExampleItem&gt;&gt; dataSource
 *     = new MutableLiveData&lt;&gt;(null);
 *
 *   public void refreshAsync(){
 *     // 在此模拟异步更新数据。
 *     Completable.timer(3, TimeUnit.SECONDS)
 *       .andThen((CompletableSource) co ->
 *         dataSource.postValue(provideExample()))
 *       .subscribeOn(Schedulers.io())
 *       .subscribe();
 *   }
 *
 *   public void refreshSync(){
 *     // 在此模拟同步更新数据。
 *     if (Looper.getMainLooper() != Looper.myLooper()) {
 *       throw new IllegalStateException("应从主线程调用此方法！");
 *     }
 *     dataSource.setValue(provideExample());
 *   }
 *
 *   // 返回一个长度为10的List用于示例，其中各项的 id ∈ [0, 20) 且唯一，顺序则随机。
 *   // 每次生成每项的desc均服从Gaussian分布。
 *   // 此处新建一份List传入作为新表。
 *   // 如果需要在原始List上直接修改，
 *   // 请在Adapter的对应Builder中设置createListCopies = true，
 *   // 否则Adapter无法正确渲染内容。
 *   private static List&lt;ExampleItem&gt; provideExample(){
 *     Random random = new Random();
 *     List&lt;ExampleItem&gt; temp = new ArrayList&lt;&gt;();
 *     for (int i = 0; i &lt; 20; i++) {
 *       temp.add(new ExampleItem(
 *         Integer.toString(i),
 *         Double.toString(random.nextGaussian())
 *       ));
 *     }
 *     Collections.shuffle(temp);
 *     return temp.subList(0, 10);
 *   }
 * }
 * </code></pre>
 * <hr/>
 *
 * <h3>Fragment实现 {@code ExampleFragment}</h3>
 * <pre><code>
 * public class ExampleFragment
 *   extends BaseFragment&lt;FragmentExampleBinding&gt;{
 *   private ExampleViewModel viewModel;
 *
 *   &#064;NonNull &#064;Override
 *   protected FragmentExampleBinding viewBindingInflate(
 *     LayoutInflater inflater,ViewGroup container){
 *     return FragmentExampleBinding.inflate(getLayoutInflater());
 *   }
 *
 *   &#064;Override public void onCreate(&#064;Nullable Bundle savedInstanceState){
 *     super.onCreate(savedInstanceState);
 *     this.viewModel = new ViewModelProvider(requireActivity())
 *     .get(ExampleViewModel.class);
 *   }
 *
 *   &#064;Override public void onStart(){
 *     super.onStart();
 *     this.binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
 *     // 使用Builder构造Adapter实例，注意Builder需要指定具体的类型参数。
 *     LiveDataListAdapter&lt;AdapterExampleBinding, ExampleItem&gt; adapter =
 *       new LiveDataListAdapter.Builder&lt;AdapterExampleBinding, ExampleItem&gt;()
 *         .areEqual(ExampleItem::areEqual)
 *         .areSame(ExampleItem::areSame)
 *         .layoutInflater(this.getLayoutInflater())
 *         .bindingInflater(AdapterExampleBinding::inflate)
 *         .itemBinder((binding, data, position) -&gt {
 *           binding.id.setText(data.getId());
 *           binding.desc.setText(data.getDesc());
 *         })
 *         .build();
 *     // 设置Adapter。
 *     this.binding.list.setAdapter(adapter);
 *     // 注册列表LiveData监听。
 *     adapter.registerDataSource(viewModel.getDataSource(), this.getViewLifecycleOwner());
 *     // 注册ViewModel更新事件。
 *     this.binding.refreshAsync.setOnClickListener(v -&gt viewModel.refreshAsync());
 *     this.binding.refreshSync.setOnClickListener(v -&gt viewModel.refreshSync());
 *   }
 * }
 * </code></pre>
 *
 * @author 8f23
 * @create 2023/7/26-14:30
 * @see androidx.recyclerview.widget.RecyclerView
 * @see androidx.recyclerview.widget.ListAdapter
 * @see androidx.recyclerview.widget.RecyclerView.Adapter
 * @see LiveData
 * @see Builder
 */
public final class LiveDataListAdapter<Binding extends ViewBinding, Type>
	extends ListAdapter<Type, LiveDataListAdapter.ViewHolder<Binding>>{
	@NonNull
	private final LayoutInflater layoutInflater;
	@NonNull
	private final BindingInflater<Binding> bindingInflater;
	@Nullable
	private final ItemBinder<Binding, Type> itemBinder;
	@Nullable
	private final ItemRecycler<Binding> itemRecycler;
	private final boolean createListCopies;
	@Nullable
	private DataSource<?> dataSource;

	private LiveDataListAdapter(
		@NonNull BiFunction<Type, Type, Boolean> areSame,
		@NonNull BiFunction<Type, Type, Boolean> areEqual,
		@NonNull LayoutInflater layoutInflater,
		@NonNull BindingInflater<Binding> bindingInflater,
		@Nullable ItemBinder<Binding, Type> itemBinder,
		@Nullable ItemRecycler<Binding> itemRecycler,
		boolean createListCopies){
		super(buildDifferConfig(areSame, areEqual));
		this.layoutInflater = layoutInflater;
		this.bindingInflater = bindingInflater;
		this.itemBinder = itemBinder;
		this.itemRecycler = itemRecycler;
		this.createListCopies = createListCopies;
	}

	/**
	 * 注册数据监听。
	 *
	 * @param liveData       {@link LiveData}数据源。
	 * @param lifecycleOwner 监听周期对应的{@link LifecycleOwner}实例。
	 */
	public <L extends List<Type>> void registerDataSource(
		@NonNull LiveData<L> liveData, @NonNull LifecycleOwner lifecycleOwner
	){
		this.registerDataSource(liveData, lifecycleOwner, t -> t);
	}

	/**
	 * 注册数据监听。
	 *
	 * @param liveData       {@link LiveData}数据源。
	 * @param lifecycleOwner 监听周期对应的{@link LifecycleOwner}实例。
	 * @param mapper         原始{@link LiveData}转换方式。
	 */
	public <L extends List<Type>, LiveDataType> void registerDataSource(
		@NonNull LiveData<LiveDataType> liveData,
		@NonNull LifecycleOwner lifecycleOwner,
		@NonNull Function<LiveDataType, L> mapper
	){
		this.unregisterDataSource();
		Observer<LiveDataType> observer = (data) -> {
			// 根据创建时的策略决定是否需要拷贝新的List，保证DiffUtil可以准确接收到新的List实例，
			// 从而有效处理每次提交的新List实例。
			@Nullable List<Type> submittedList = (data == null)
				? null
				: (createListCopies ? new ArrayList<>(mapper.apply(data)) : mapper.apply(data));
			this.submitList(submittedList);
		};
		liveData.observe(lifecycleOwner, observer);
		this.dataSource = new DataSource<>(liveData, observer);
	}

	/**
	 * 取消数据监听。
	 */
	public void unregisterDataSource(){
		DataSource<?> dataSource = this.dataSource;
		if (dataSource == null) {
			return;
		}
		dataSource.removeObserver();
		this.dataSource = null;
	}

	@NonNull
	@Override
	public ViewHolder<Binding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
		return new ViewHolder<>(this.bindingInflater.inflate(this.layoutInflater));
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder<Binding> holder, int position){
		Binding binding = holder.binding;
		ItemBinder<Binding, Type> itemBinder = this.itemBinder;
		if (itemBinder == null) {
			return;
		}
		Type itemData = null;
		try {
			itemData = this.getItem(position);
		}
		catch (Exception ignored) {
		}
		if (itemData == null) {
			return;
		}
		itemBinder.bindView(binding, itemData, position);
	}

	@Override public void onViewRecycled(@NonNull ViewHolder<Binding> holder){
		super.onViewRecycled(holder);
		if (itemRecycler == null) {
			return;
		}
		itemRecycler.recycle(holder.binding);
	}

	/**
	 * ViewBinding构建接口。
	 *
	 * @param <Binding> 组件Binding。
	 * @see RecyclerView.Adapter#createViewHolder(ViewGroup, int)
	 */
	public interface BindingInflater<Binding extends ViewBinding>{
		/**
		 * ViewBinding构建方法。
		 *
		 * @param inflater {@link LayoutInflater}实例。
		 * @return ViewBinding实例。
		 */
		@NonNull Binding inflate(@NonNull LayoutInflater inflater);
	}

	/**
	 * 视图绑定方法接口。
	 *
	 * @param <Binding> 组件Binding。
	 * @param <Type>    列表项的类型。
	 * @see RecyclerView.Adapter#onBindViewHolder(RecyclerView.ViewHolder, int)
	 */
	public interface ItemBinder<Binding extends ViewBinding, Type>{
		/**
		 * 视图绑定方法。
		 *
		 * @param binding  ViewBinding实例。
		 * @param data     对应项数据。
		 * @param position 所在项的位置。
		 */
		void bindView(@NonNull Binding binding, @NonNull Type data, @IntRange (from = 0) int position);
	}

	/**
	 * 视图回收方法接口。
	 *
	 * @param <Binding> 组件Binding。
	 * @see RecyclerView.Adapter#onViewRecycled(RecyclerView.ViewHolder)
	 */
	public interface ItemRecycler<Binding extends ViewBinding>{
		/**
		 * 视图回收方法。
		 *
		 * @param binding 需要回收的Binding。
		 */
		void recycle(@NonNull Binding binding);
	}

	/**
	 * {@link LiveDataListAdapter Adatper}的Builder。
	 *
	 * @param <Binding> 组件Binding。
	 * @param <Type>    列表项的类型。
	 * @see LiveDataListAdapter
	 */
	@NoArgsConstructor
	public static final class Builder<Binding extends ViewBinding, Type>{
		@Nullable private BiFunction<Type, Type, Boolean> areSame;
		@Nullable private BiFunction<Type, Type, Boolean> areEqual;
		@Nullable private LayoutInflater layoutInflater;
		@Nullable private BindingInflater<Binding> bindingInflater;
		@Nullable private ItemBinder<Binding, Type> itemBinder;
		@Nullable private ItemRecycler<Binding> itemRecycler;
		private boolean createListCopies = false;

		/**
		 * 构建Adapter实例。
		 */
		public LiveDataListAdapter<Binding, Type> build(){
			return new LiveDataListAdapter<>(
				Objects.requireNonNull(areSame, "Parameter 'isSame' is null!"),
				Objects.requireNonNull(areEqual, "Parameter 'areEqual' is null!"),
				Objects.requireNonNull(layoutInflater, "Parameter 'layoutInflater' is null!"),
				Objects.requireNonNull(bindingInflater, "Parameter 'bindingInflater' is null!"),
				itemBinder,
				itemRecycler,
				createListCopies);
		}

		/**
		 * <strong>【必填】</strong>设置项的比较方法。如果实体是同一项，则返回{@code true}，否则返回{@code false}。
		 * 一般而言，如果两个实体拥有相同的标识，则应当视为同一项。此方法用于判断旧表中项是否仍存在于新表上。
		 *
		 * @param areSame 比较方法。
		 * @return {@code this}
		 * @see DiffUtil.ItemCallback#areItemsTheSame(Object, Object)
		 */
		@NonNull
		public Builder<Binding, Type> areSame(@NonNull BiFunction<Type, Type, Boolean> areSame){
			this.areSame = areSame;
			return this;
		}

		/**
		 * <strong>【必填】</strong>设置项的比较方法。如果实体是同一项并且其内容完全相同，则返回{@code true}，否则返回{@code false}。
		 * 一般而言，如果两个实体所显示的部分完全相同，则应当视为相同的。此方法用于判断旧表中仍存在于新表上的项是否需要被重新渲染。
		 *
		 * @param areEqual 比较方法。
		 * @return {@code this}
		 * @see DiffUtil.ItemCallback#areContentsTheSame(Object, Object)
		 */
		@NonNull
		public Builder<Binding, Type> areEqual(@NonNull BiFunction<Type, Type, Boolean> areEqual){
			this.areEqual = areEqual;
			return this;
		}

		/**
		 * <strong>【必填】</strong>设置项视图的{@link LayoutInflater}。
		 *
		 * @param layoutInflater {@link LayoutInflater}实例。
		 * @return {@code this}
		 */
		@NonNull
		public Builder<Binding, Type> layoutInflater(@NonNull LayoutInflater layoutInflater){
			this.layoutInflater = layoutInflater;
			return this;
		}

		/**
		 * <strong>【必填】</strong>设置项视图的ViewBinding构建方法。
		 *
		 * @param bindingInflater 构建方法。
		 * @return {@code this}
		 */
		@NonNull
		public Builder<Binding, Type> bindingInflater(@NonNull BindingInflater<Binding> bindingInflater){
			this.bindingInflater = bindingInflater;
			return this;
		}

		/**
		 * <strong>【可选】</strong>设置项视图的绑定渲染方法。若未填写或赋值为{@code null}，则不会根据数据渲染列表各项内容。
		 *
		 * @param itemBinder 实例化方法实例。
		 * @return {@code this}
		 */
		@NonNull
		public Builder<Binding, Type> itemBinder(@Nullable ItemBinder<Binding, Type> itemBinder){
			this.itemBinder = itemBinder;
			return this;
		}

		/**
		 * <strong>【可选】</strong>设置项视图的回收方法。若未填写或赋值为{@code null}，则不会回收各项视图。
		 *
		 * @param itemRecycler 回收方法实例。
		 * @return {@code this}
		 */
		@NonNull
		public Builder<Binding, Type> itemRecycler(@Nullable ItemRecycler<Binding> itemRecycler){
			this.itemRecycler = itemRecycler;
			return this;
		}

		/**
		 * <strong>【可选】</strong>设置是否需要在{@link LiveData}发生变化时，根据新{@link List}创建新的实例用于提交更新。
		 * 默认值为{@code false}，表示不需要。
		 * 如果{@link LiveData}的更新总是会通过构建完全不同的列表实例而更新其持有的实例引用，则此值为{@code false}即可；
		 * 如果{@link LiveData}的更新可能会在其持有的旧列表引用的基础上修改，则此值应当为{@code true}以保证
		 * {@link androidx.recyclerview.widget.AsyncListDiffer}可以准确比较列表前后的变化。
		 *
		 * @param required 是否需要创建新的{@link List}实例。
		 * @return {@code this}
		 * @see ListAdapter#submitList(List)
		 */
		@NonNull
		public Builder<Binding, Type> createListCopies(boolean required){
			this.createListCopies = required;
			return this;
		}
	}

	/**
	 * {@link RecyclerView.ViewHolder}实现，集成了ViewBinding。
	 */
	final static class ViewHolder<Binding extends ViewBinding> extends RecyclerView.ViewHolder{
		@NonNull
		private final Binding binding;

		public ViewHolder(@NonNull final Binding binding){
			super(binding.getRoot());
			this.binding = binding;
		}
	}

	private static <Type> DiffUtil.ItemCallback<Type> buildDifferConfig(
		@NonNull BiFunction<Type, Type, Boolean> areSame, @NonNull BiFunction<Type, Type, Boolean> areEqual
	){
		return new DiffUtil.ItemCallback<Type>(){
			@Override
			public boolean areItemsTheSame(@NonNull Type oldItem, @NonNull Type newItem){
				return areSame.apply(oldItem, newItem);
			}

			@Override
			public boolean areContentsTheSame(@NonNull Type oldItem, @NonNull Type newItem){
				return areEqual.apply(oldItem, newItem);
			}
		};
	}

	@AllArgsConstructor
	private static class DataSource<LiveDataType>{
		@NonNull
		private final LiveData<LiveDataType> source;
		@NonNull
		private final Observer<LiveDataType> observer;

		/**
		 * 清除监听的Observer。
		 */
		void removeObserver(){
			source.removeObserver(observer);
		}
	}
}
