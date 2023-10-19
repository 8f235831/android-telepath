package pers.u8f23.telepath.example.core;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import java.util.List;
import java.util.Objects;

/**
 * @author 8f23
 * @create 2023/8/4-13:51
 */
public final class ArrayListAdapter<Binding extends ViewBinding, Type>
	extends RecyclerView.Adapter<ArrayListAdapter.ViewHolder<Binding>>{
	@NonNull
	private final LayoutInflater inflater;
	@NonNull
	private final BindingInflater<Binding> bindingInflater;
	@Nullable
	private final ItemBinder<Binding, ? super Type> itemBinder;
	@Nullable
	private final ItemRecycler<Binding> itemRecycler;
	@NonNull
	private final List<? extends Type> dataList;
	private final boolean isRecyclable;

	private ArrayListAdapter(
		@NonNull LayoutInflater inflater,
		@NonNull BindingInflater<Binding> bindingInflater,
		@Nullable ItemBinder<Binding, ? super Type> itemBinder,
		@Nullable ItemRecycler<Binding> itemRecycler,
		@NonNull List<? extends Type> dataList, boolean isRecyclable){
		this.inflater = inflater;
		this.bindingInflater = bindingInflater;
		this.itemBinder = itemBinder;
		this.itemRecycler = itemRecycler;
		this.dataList = dataList;
		this.isRecyclable = isRecyclable;
	}

	@Override public void onBindViewHolder(@NonNull ViewHolder<Binding> holder, int position){
		if (itemBinder == null) {
			return;
		}
		Type data = null;
		try {
			data = dataList.get(position);
		}
		catch (Throwable ignored) {
		}
		if (data == null) {
			return;
		}
		itemBinder.bindView(holder.binding, data, position);
	}

	@Override public void onViewRecycled(@NonNull ViewHolder<Binding> holder){
		if (itemRecycler == null) {
			return;
		}
		itemRecycler.recycle(holder.binding);
	}

	@NonNull @Override public ViewHolder<Binding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
		Binding binding = this.bindingInflater.inflate(inflater);
		ViewHolder<Binding> holder = new ViewHolder<>(binding);
		holder.setIsRecyclable(isRecyclable);
		return holder;
	}

	@Override public int getItemCount(){
		return dataList.size();
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
	 * 构造器。
	 *
	 * @param <Binding> 组件{@link ViewBinding}。
	 * @param <Type>    元素数据类型。
	 */
	public static class Builder<Binding extends ViewBinding, Type>{
		@Nullable
		private LayoutInflater inflater;
		@Nullable
		private BindingInflater<Binding> bindingInflater;
		@Nullable
		private ItemBinder<Binding, ? super Type> itemBinder;
		@Nullable
		private ItemRecycler<Binding> itemRecycler;
		@Nullable
		private List<? extends Type> dataList;
		private boolean isRecyclable = true;

		/**
		 * 指定{@link LayoutInflater}实例。
		 */
		public Builder<Binding, Type> inflater(LayoutInflater inflater){
			this.inflater = inflater;
			return this;
		}

		/**
		 * 指定如何构造{@link ViewBinding}。
		 */
		public Builder<Binding, Type> bindingInflater(BindingInflater<Binding> bindingInflater){
			this.bindingInflater = bindingInflater;
			return this;
		}

		/**
		 * 指定如何展示对应项组件。
		 */
		public Builder<Binding, Type> itemBinder(ItemBinder<Binding, ? super Type> itemBinder){
			this.itemBinder = itemBinder;
			return this;
		}

		/**
		 * 指定如何回收组件。
		 */
		public Builder<Binding, Type> itemRecycler(ItemRecycler<Binding> itemRecycler){
			this.itemRecycler = itemRecycler;
			return this;
		}

		/**
		 * 指定数据集。
		 */
		public Builder<Binding, Type> dataList(List<? extends Type> dataList){
			this.dataList = dataList;
			return this;
		}

		/**
		 * 指定是否回收组件。默认会回收组件。
		 */
		public Builder<Binding, Type> isRecyclable(boolean isRecyclable){
			this.isRecyclable = isRecyclable;
			return this;
		}

		/**
		 * 构造实例。
		 */
		public ArrayListAdapter<Binding, Type> build(){
			return new ArrayListAdapter<>(
				Objects.requireNonNull(inflater),
				Objects.requireNonNull(bindingInflater),
				itemBinder,
				itemRecycler,
				Objects.requireNonNull(dataList),
				isRecyclable);
		}
	}

	public static class ViewHolder<Binding extends ViewBinding> extends RecyclerView.ViewHolder{
		public final Binding binding;

		public ViewHolder(@NonNull Binding binding){
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
