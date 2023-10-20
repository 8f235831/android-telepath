package pers.u8f23.telepath;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

import lombok.Getter;
import lombok.NonNull;

/**
 * @author 8f23
 * @create 2022/12/20-20:12
 */
public class NavAptMapperNode implements Comparable<Object>{
	@Getter
	private final String path;
	@Getter
	private final boolean prefix;
	@Getter
	private final Element methodElement;
	@Getter
	private final String description;
	private String sortedParams = null;

	@Override
	public int compareTo(@NonNull Object o){
		if (o instanceof NavAptMapperNode) {
			return this.path.compareTo(((NavAptMapperNode) o).path);
		}
		else if (o instanceof String) {
			return this.path.compareTo((String) o);
		}
		throw new UnsupportedOperationException();
	}

	@Override public String toString(){
		return "Node{path='" + path + "', isPrefix=" + prefix + '}';
	}

	@Override public boolean equals(Object obj){
		if (!(obj instanceof NavAptMapperNode)) {
			return false;
		}
		return this.methodElement == ((NavAptMapperNode) obj).methodElement;
	}

	public NavAptMapperNode(String path, boolean prefix, Element methodElement, String description){
		this.path = path;
		this.prefix = prefix;
		this.methodElement = methodElement;
		this.description = description;
	}

	public boolean check(Messager messager,final boolean DEBUG_MODE){
		if (!(methodElement instanceof ExecutableElement)) {
			messager.printMessage(
				Diagnostic.Kind.ERROR,
				"Intent nav APT: Element object is NOT the implement of javax.lang.model.element" +
					".ExecutableElement!",
				methodElement
			);
			return true;
		}
		List<? extends VariableElement> paramElements = ((ExecutableElement) methodElement).getParameters();
		int controllerParamIndex = -1;
		int intentDataParamIndex = -1;
		int pathDataParamIndex = -1;
		if (DEBUG_MODE) {
			messager.printMessage(
				Diagnostic.Kind.NOTE,
				"found param size: " + paramElements.size(),
				methodElement
			);
		}
		VariableElement[] paramElementArray = paramElements.toArray(new VariableElement[0]);
		for (int i = 0; i < paramElementArray.length; i++) {
			VariableElement param = paramElementArray[i];
			IntentNavController controllerAnnotation = param.getAnnotation(IntentNavController.class);
			IntentNavPathData pathDataAnnotation = param.getAnnotation(IntentNavPathData.class);
			IntentNavFullData intentDataAnnotation = param.getAnnotation(IntentNavFullData.class);
			int paramAnnotationReferredCounter = 0;
			if (controllerAnnotation != null) {
				paramAnnotationReferredCounter++;
				controllerParamIndex = i;
			}
			if (pathDataAnnotation != null) {
				paramAnnotationReferredCounter++;
				pathDataParamIndex = i;
			}
			if (intentDataAnnotation != null) {
				paramAnnotationReferredCounter++;
				intentDataParamIndex = i;
			}
			if (paramAnnotationReferredCounter < 1) {
				messager.printMessage(
					Diagnostic.Kind.ERROR,
					"Intent nav APT: Param should be annotated.",
					param
				);
				return true;
			}
			if (paramAnnotationReferredCounter > 1) {
				messager.printMessage(
					Diagnostic.Kind.ERROR,
					"Intent nav APT: Param should be annotated for only once.",
					param
				);
				return true;
			}
		}
		if (controllerParamIndex == -1) {
			messager.printMessage(
				Diagnostic.Kind.ERROR,
				"Intent nav APT: Method should have a annotated controller param.",
				methodElement
			);
			return true;
		}
		LinkedList<ParamSortPack> paramPacks = new LinkedList<>();
		paramPacks.add(new ParamSortPack("controller", controllerParamIndex));
		if (pathDataParamIndex > -1) {
			paramPacks.add(new ParamSortPack("path", pathDataParamIndex));
		}
		if (intentDataParamIndex > -1) {
			paramPacks.add(new ParamSortPack("intent", intentDataParamIndex));
		}
		Collections.sort(paramPacks);
		Iterator<ParamSortPack> paramIterator = paramPacks.iterator();
		StringBuilder sortedParamsStringBuilder = new StringBuilder();
		sortedParamsStringBuilder.append(paramIterator.next().name);
		while (paramIterator.hasNext()) {
			sortedParamsStringBuilder
				.append(", ")
				.append(paramIterator.next().name);
		}
		sortedParams = sortedParamsStringBuilder.toString();
		return false;
	}

	private static class ParamSortPack implements Comparable<ParamSortPack>{
		public final String name;
		public final int index;

		private ParamSortPack(String name, int index){
			this.name = name;
			this.index = index;
		}

		@Override public int compareTo(@NonNull NavAptMapperNode.ParamSortPack ano){
			return Integer.compare(this.index, ano.index);
		}
	}

	public String getSortedParams(){
		String s = sortedParams;
		if (s == null) {
			throw new IllegalStateException("Unchecked!");
		}
		return s;
	}

	public String getMethodName(){
		return methodElement.getSimpleName().toString();
	}
}
