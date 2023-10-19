package pers.u8f23.telepath;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * 用于实现外部intent导航跳转的 APT Processor。
 *
 * @author 8f23
 * @create 2022/12/18-17:43
 * @see IntentNavMethod
 * @see IntentNavController
 * @see IntentNavFullData
 * @see IntentNavPathData
 */
@AutoService (Processor.class)
public class NavProcessor extends AbstractProcessor{
	private static final boolean DEBUG_MODE = false;
	private static final boolean MANIFEST_PRINT_MODE = true;

	private static final String GENERATED_NAV_MANIFEST_FILE_NAME = "nav_manifest.txt";
	private static final String GENERATED_CLASS_PACKAGE_NAME = "pers.u8f23.telepath";
	private static final String GENERATED_NAV_MAPPER_HOLDER_CLASS_NAME = "TelepathMapperHolder";
	private static final String GENERATED_NAV_PERFORMER_CLASS_NAME = "TelepathPerformer";
	private static final String GENERATED_NAV_MAPPER_NODE_CLASS_NAME = "TelepathMapperNode";
	private static final String NAV_CONTROLLER_PACKAGE_NAME = "androidx.navigation";
	private static final String NAV_CONTROLLER_CLASS_NAME = "NavController";
	private static final String INTENT_PACKAGE_NAME = "android.content";
	private static final String INTENT_CLASS_NAME = "Intent";

	private Types mTypeUtils;
	private Elements mElementUtils;
	private Filer mFiler;
	private Messager mMessage;

	private boolean processedFlag = false;

	private Date processTime;

	private final TreeSet<NavAptMapperNode> mapperNodeSet = new TreeSet<>();
	private TypeSpec homePageMethodClass;
	private TypeSpec errorPageMethodClass;

	@Override public synchronized void init(ProcessingEnvironment processingEnv){
		super.init(processingEnv);
		mTypeUtils = processingEnv.getTypeUtils();
		mElementUtils = processingEnv.getElementUtils();
		mFiler = processingEnv.getFiler();
		mMessage = processingEnv.getMessager();
		processTime = new Date();

		if (DEBUG_MODE) {
			mMessage.printMessage(Diagnostic.Kind.NOTE, "Intent nav APT initialized.");
		}
	}

	@Override public Set<String> getSupportedAnnotationTypes(){
		Set<String> annotations = new LinkedHashSet<>();
		annotations.add(IntentNavMethod.class.getCanonicalName());
		return annotations;
	}

	@Override
	public SourceVersion getSupportedSourceVersion(){
		return SourceVersion.latestSupported();
	}

	@Override public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment){
		if (processedFlag) {
			// 本 processor 无需重复执行 process 方法。
			return false;
		}
		if (DEBUG_MODE) {
			mMessage.printMessage(
				Diagnostic.Kind.NOTE,
				"Intent nav APT: Start to process."
			);
		}

		// 处理 IntentNavMethod.
		for (Element annotatedElement : roundEnvironment.getElementsAnnotatedWith(IntentNavMethod.class)) {
			boolean b = handleNormalMethod(annotatedElement);
			if (b) {
				return true;
			}
		}
		if (DEBUG_MODE) {
			mMessage.printMessage(
				Diagnostic.Kind.NOTE,
				"Found " + mapperNodeSet.size() + " method(s) annotated with " + IntentNavMethod.class.getSimpleName()
			);
		}
		if (buildMappingClass()) {
			return true;
		}
		if (buildOtherClasses()) {
			return true;
		}
		if (MANIFEST_PRINT_MODE) {
			if (printManifest()) {
				return true;
			}
		}
		processedFlag = true;
		return false;
	}

	/** 自动构建类。 */
	private boolean buildMappingClass(){
		if (DEBUG_MODE) {
			mMessage.printMessage(
				Diagnostic.Kind.NOTE,
				"Intent nav APT: Start to build navigation class file."
			);
		}
		try {
			ClassName mapperNodeClassName = ClassName.get(
				GENERATED_CLASS_PACKAGE_NAME, GENERATED_NAV_MAPPER_NODE_CLASS_NAME);
			ClassName performerClassName = ClassName.get(
				GENERATED_CLASS_PACKAGE_NAME, GENERATED_NAV_PERFORMER_CLASS_NAME);
			ClassName controllerClassName = ClassName.get(NAV_CONTROLLER_PACKAGE_NAME, NAV_CONTROLLER_CLASS_NAME);
			ClassName nonNullClassName = ClassName.get("androidx.annotation", "NonNull");
			ClassName uriClassName = ClassName.get("android.net", "Uri");
			ClassName intentClassName = ClassName.get(INTENT_PACKAGE_NAME, INTENT_CLASS_NAME);

			FieldSpec performerSet = FieldSpec.builder(
					ParameterizedTypeName.get(ClassName.get(ArrayList.class), mapperNodeClassName),
					"PERFORMER_LIST",
					Modifier.STATIC,
					Modifier.FINAL,
					Modifier.PRIVATE
				)
				.initializer("new $T<$T>()", ArrayList.class, mapperNodeClassName)
				.addJavadoc("一般页面跳转规则。")
				.build();
			FieldSpec homePagePerformer = FieldSpec.builder(
					performerClassName,
					"HOME_PAGE_PERFORMER",
					Modifier.STATIC,
					Modifier.FINAL,
					Modifier.PRIVATE
				)
				.initializer("$L", homePageMethodClass)
				.addJavadoc("首页跳转规则。")
				.build();
			FieldSpec errorPagePerformer = FieldSpec.builder(
					performerClassName,
					"ERROR_PAGE_PERFORMER",
					Modifier.STATIC,
					Modifier.FINAL,
					Modifier.PRIVATE
				)
				.initializer("$L", errorPageMethodClass)
				.addJavadoc("错误页跳转规则。")
				.build();
			CodeBlock.Builder performerInitializeStaticBlockBuilder = CodeBlock.builder();
			performerInitializeStaticBlockBuilder.addStatement("$T performer", performerClassName);
			for (NavAptMapperNode node : this.mapperNodeSet) {
				TypeSpec innerClass = TypeSpec.anonymousClassBuilder("")
					.addSuperinterface(performerClassName)
					.addMethod(MethodSpec.methodBuilder("navigate")
						.addAnnotation(Override.class)
						.addModifiers(Modifier.PUBLIC)
						.addParameter(controllerClassName, "controller")
						.addParameter(String.class, "path")
						.addParameter(intentClassName, "intent")
						.returns(TypeName.VOID)
						.addStatement("$T.$L($L)",
							mTypeUtils.getDeclaredType((TypeElement) node.getMethodElement().getEnclosingElement()),
							node.getMethodName(),
							node.getSortedParams())
						.build())
					.build();
				performerInitializeStaticBlockBuilder.addStatement("performer = $L", innerClass);
				performerInitializeStaticBlockBuilder.addStatement("PERFORMER_LIST.add(new $T($S, $L, performer))",
					mapperNodeClassName, node.getPath(), node.isPrefix());
			}
			JavaFile navMapperHolderFile = JavaFile.builder(
				GENERATED_CLASS_PACKAGE_NAME,
				TypeSpec.classBuilder(GENERATED_NAV_MAPPER_HOLDER_CLASS_NAME)
					.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
					.addJavadoc("处理Intent页面跳转逻辑。\n")
					.addJavadoc("自动化生成文件，在编译时重置。请勿手动修改此文件。\n")
					.addJavadoc("@author 8f23\n")
					.addJavadoc(
						"@create $L\n",
						new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(processTime)
					)
					.addField(homePagePerformer)
					.addField(errorPagePerformer)
					.addField(performerSet)
					.addStaticBlock(performerInitializeStaticBlockBuilder.build())
					.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
					.addMethod(MethodSpec.methodBuilder("getPerformer")
						.addModifiers(Modifier.PRIVATE, Modifier.STATIC)
						.addParameter(ParameterSpec.builder(String.class, "path")
							.addAnnotation(nonNullClassName)
							.build())
						.addAnnotation(nonNullClassName)
						.returns(performerClassName)
						.addJavadoc("搜索跳转逻辑。\n")
						.addJavadoc("@param path 输入路径。")
						.addStatement("int binaryIndex = $T.binarySearch(PERFORMER_LIST, path)", Collections.class)
						.beginControlFlow("if (binaryIndex >= 0)")
						.addComment("直接找到匹配结果。")
						.addStatement("return PERFORMER_LIST.get(binaryIndex).getPerformer()")
						.endControlFlow()
						.addStatement("binaryIndex = -(binaryIndex + 2)")
						.beginControlFlow("if (binaryIndex == -1)")
						.addComment("无潜在前驱结果。")
						.addStatement("return ERROR_PAGE_PERFORMER")
						.endControlFlow()
						.addStatement("$T previousNode = PERFORMER_LIST.get(binaryIndex)", mapperNodeClassName)
						.beginControlFlow("if (previousNode == null)")
						.addStatement("return ERROR_PAGE_PERFORMER")
						.endControlFlow()
						.addComment("校验前驱结果是否为当前路径的可行前缀后返回结果。")
						.beginControlFlow("if (previousNode.isPrefix() && path.startsWith(previousNode.getPath()))")
						.addStatement("return previousNode.getPerformer()")
						.endControlFlow()
						.addStatement("return ERROR_PAGE_PERFORMER")
						.build())
					.addMethod(MethodSpec.methodBuilder("performIntent")
						.addModifiers(Modifier.STATIC, Modifier.PUBLIC)
						.addParameter(ParameterSpec.builder(intentClassName, "intent", Modifier.FINAL)
							.addAnnotation(nonNullClassName)
							.build())
						.addParameter(ParameterSpec.builder(controllerClassName, "controller", Modifier.FINAL)
							.addAnnotation(nonNullClassName)
							.build())
						.returns(TypeName.VOID)
						.addJavadoc("尝试执行跳转。\n")
						.addJavadoc("@param intent 传入的Intent。\n")
						.addJavadoc("@param controller 页面Controller。")
						.addStatement("$T performer = HOME_PAGE_PERFORMER", performerClassName)
						.addStatement("$T uriData = intent.getData()", uriClassName)
						.addStatement("$T fullPath = \"\"", String.class)
						.beginControlFlow("if (uriData != null)")
						.addStatement("fullPath = uriData.getPath()")
						.beginControlFlow("if (fullPath != null)")
						.addStatement("performer = getPerformer(fullPath)")
						.endControlFlow()
						.endControlFlow()
						.addStatement("performer.navigate(controller, fullPath, intent)")
						.build())
					.build()
			).build();
			navMapperHolderFile.writeTo(mFiler);
		}
		catch (IOException e) {
			// 禁止在公开类以外使用此注解。
			mMessage.printMessage(
				Diagnostic.Kind.WARNING,
				"failed to generate NavMapperHolder class."
			);
		}
		return false;
	}


	private boolean buildOtherClasses(){
		ClassName performerClassName = ClassName.get(
			GENERATED_CLASS_PACKAGE_NAME,
			GENERATED_NAV_PERFORMER_CLASS_NAME
		);
		ClassName nodeClassName = ClassName.get(
			GENERATED_CLASS_PACKAGE_NAME,
			GENERATED_NAV_MAPPER_NODE_CLASS_NAME
		);
		ClassName intentClassName = ClassName.get(
			INTENT_PACKAGE_NAME,
			INTENT_CLASS_NAME
		);
		if (DEBUG_MODE) {
			mMessage.printMessage(
				Diagnostic.Kind.NOTE,
				"Intent nav APT: Start to build NavPerformer class file."
			);
		}
		try {
			JavaFile navPerformerFile = JavaFile.builder(
				GENERATED_CLASS_PACKAGE_NAME,
				TypeSpec.interfaceBuilder(GENERATED_NAV_PERFORMER_CLASS_NAME)
					.addModifiers(Modifier.PUBLIC)
					.addJavadoc("用于处理外部intent跳转的接口。\n")
					.addJavadoc("该类及其实现、实例将通过APT自动化生成。请勿手动修改、实现此接口！\n\n")
					.addJavadoc("@author 8f23\n")
					.addJavadoc(
						"@create $L\n",
						new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(processTime)
					)
					.addMethod(MethodSpec
						.methodBuilder("navigate")
						.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
						.addParameter(
							ParameterSpec.builder(
								ClassName.get(NAV_CONTROLLER_PACKAGE_NAME, NAV_CONTROLLER_CLASS_NAME),
								"controller"
							).build()
						)
						.addParameter(ParameterSpec.builder(String.class, "path").build())
						.addParameter(ParameterSpec.builder(intentClassName, "intent").build())
						.returns(TypeName.VOID)
						.addJavadoc("跳转方法。")
						.build()
					).build()
			).build();
			navPerformerFile.writeTo(mFiler);
		}
		catch (IOException e) {
			// 禁止在公开类以外使用此注解。
			mMessage.printMessage(
				Diagnostic.Kind.WARNING,
				"failed to generate NavPerformer class."
			);
		}
		if (DEBUG_MODE) {
			mMessage.printMessage(
				Diagnostic.Kind.NOTE,
				"Intent nav APT: Start to build NavMapperNode class file."
			);
		}
		try {

			JavaFile nabMapperFile = JavaFile.builder(
				GENERATED_CLASS_PACKAGE_NAME,
				TypeSpec.classBuilder(GENERATED_NAV_MAPPER_NODE_CLASS_NAME)
					.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
					.addJavadoc("外部intent跳转的结点。\n")
					.addJavadoc("该类及其实现、实例将通过APT自动化生成。请勿手动修改此类！\n\n")
					.addJavadoc("@author 8f23\n")
					.addJavadoc(
						"@create $L\n",
						new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(processTime)
					)
					.addSuperinterface(ParameterizedTypeName.get(Comparable.class, Object.class))
					.addField(FieldSpec.builder(String.class, "path", Modifier.PRIVATE, Modifier.FINAL).build())
					.addMethod(MethodSpec.methodBuilder("getPath")
						.addModifiers(Modifier.PUBLIC)
						.returns(String.class)
						.addStatement("return this.path")
						.build())
					.addField(FieldSpec.builder(TypeName.BOOLEAN, "prefix", Modifier.PRIVATE, Modifier.FINAL).build())
					.addMethod(MethodSpec.methodBuilder("isPrefix")
						.addModifiers(Modifier.PUBLIC)
						.returns(TypeName.BOOLEAN)
						.addStatement("return this.prefix")
						.build())
					.addField(FieldSpec.builder(
						performerClassName,
						"performer",
						Modifier.PRIVATE, Modifier.FINAL
					).build())
					.addMethod(MethodSpec.methodBuilder("getPerformer")
						.addModifiers(Modifier.PUBLIC)
						.returns(performerClassName)
						.addStatement("return this.performer")
						.build())
					.addMethod(MethodSpec.constructorBuilder()
						.addParameter(String.class, "path")
						.addParameter(TypeName.BOOLEAN, "prefix")
						.addParameter(performerClassName, "performer")
						.addStatement("this.path = path")
						.addStatement("this.prefix = prefix")
						.addStatement("this.performer = performer")
						.build())
					.addMethod(MethodSpec.methodBuilder("compareTo")
						.addModifiers(Modifier.PUBLIC)
						.addParameter(Object.class, "o")
						.addAnnotation(Override.class)
						.returns(TypeName.INT)
						.beginControlFlow("if (o instanceof $T)", nodeClassName)
						.addStatement("return this.path.compareTo((($T) o).path)", nodeClassName)
						.nextControlFlow("else if (o instanceof String)")
						.addStatement("return this.path.compareTo((String) o)")
						.endControlFlow()
						.addStatement("throw new UnsupportedOperationException()")
						.build())
					.build()
			).build();
			nabMapperFile.writeTo(mFiler);
		}
		catch (IOException e) {
			// 禁止在公开类以外使用此注解。
			mMessage.printMessage(
				Diagnostic.Kind.WARNING,
				"failed to generate NavMapperNode class."
			);
		}
		return false;
	}

	/** 自动输出清单文件。 */
	private boolean printManifest(){
		try {
			if (DEBUG_MODE) {
				mMessage.printMessage(
					Diagnostic.Kind.NOTE,
					"Intent nav APT: Start to print navigation manifest file."
				);
			}
			FileObject navManifestFile =
				mFiler.createResource(StandardLocation.SOURCE_OUTPUT, "",
					GENERATED_NAV_MANIFEST_FILE_NAME);
			Writer navManifestWriter = navManifestFile.openWriter();
			navManifestWriter.write(
				"导航接口清单文件，自动生成于：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date()) +
					"\npath,\tprefix,\tdescription,\tmethod\n"
			);
			for (NavAptMapperNode mapperNode : this.mapperNodeSet) {
				Element methodElement = mapperNode.getMethodElement();
				List<? extends VariableElement> parameters = ((ExecutableElement) methodElement).getParameters();
				StringBuilder parameterText = new StringBuilder();
				for (VariableElement parameter : parameters) {
					parameterText
						.append(parameter.asType().toString())
						.append(" ")
						.append(parameter.getSimpleName())
						.append(", ");
				}
				String methodText = mElementUtils.getPackageOf(methodElement).getQualifiedName().toString() + "." +
					methodElement.getEnclosingElement().getSimpleName().toString() + "." +
					(methodElement.getSimpleName().toString()) + "(" +
					((parameterText.length() == 0) ? ("") : parameterText.substring(0, parameterText.length() - 2))
					+ ")";
				navManifestWriter.write(
					mapperNode.getPath() + ",\t" + mapperNode.isPrefix() + ",\t" +
						mapperNode.getDescription() + ",\t" + methodText + "\n");
			}
			if (DEBUG_MODE) {
				mMessage.printMessage(
					Diagnostic.Kind.NOTE,
					"Intent nav APT: Succeed to print navigation manifest file."
				);
			}
			navManifestWriter.flush();
			navManifestWriter.close();
			return false;
		}
		catch (Exception e) {
			mMessage.printMessage(
				Diagnostic.Kind.ERROR,
				"Intent nav APT: Failed to print navigation manifest file."
			);
			return true;
		}
	}

	private boolean handleNormalMethod(Element annotatedElement){
		if (annotatedElement.getKind() != ElementKind.METHOD
			|| !annotatedElement.getModifiers().contains(Modifier.PUBLIC)
			|| !annotatedElement.getModifiers().contains(Modifier.STATIC)
		) {
			// 禁止在公开静态方法以外使用此注解。
			mMessage.printMessage(
				Diagnostic.Kind.ERROR,
				IntentNavMethod.class.getSimpleName() + " should be annotated on PUBLIC and STATIC METHOD(s)!",
				annotatedElement
			);
			return true;
		}
		// 提取信息用以构建类。
		IntentNavMethod methodAnnotation = annotatedElement.getAnnotation(IntentNavMethod.class);
		String path = methodAnnotation.value();
		boolean isPrefix = methodAnnotation.isPrefix();
		String description = methodAnnotation.description();
		if (path.isEmpty() || !path.matches("/[a-zA-Z0-9/_?%]*")) {
			// 必须填写有效的path。
			mMessage.printMessage(
				Diagnostic.Kind.ERROR,
				IntentNavMethod.class.getSimpleName() + " should set value with string matching regex " +
					"(/[a-zA-Z0-9/_?%]*)",
				annotatedElement
			);
			return true;
		}
		if (description.isEmpty()) {
			// 必须填写文档。
			mMessage.printMessage(
				Diagnostic.Kind.ERROR,
				IntentNavMethod.class.getSimpleName() + " should fill its description to generate document file.",
				annotatedElement
			);
			return true;
		}
		if (DEBUG_MODE) {
			String packageName = mElementUtils.getPackageOf(annotatedElement).getQualifiedName().toString();
			String className = annotatedElement.getEnclosingElement().getSimpleName().toString();
			String methodName = annotatedElement.getSimpleName().toString();
			mMessage.printMessage(
				Diagnostic.Kind.NOTE,
				"Intent nav APT parsing annotated method: " + packageName + "." + className + ":" + methodName,
				annotatedElement
			);
		}
		NavAptMapperNode navMapperNode =
			new NavAptMapperNode(path, isPrefix, annotatedElement, description);
		if (navMapperNode.check(mMessage, DEBUG_MODE)) {
			return true;
		}
		// 检查路径冲突。
		if (searchNodeCompatibility(navMapperNode)) {
			return true;
		}
		else {
			// 加入列表。
			mapperNodeSet.add(navMapperNode);
		}
		return false;
	}

	/**
	 * 检索列表是否会发生冲突。
	 *
	 * @return 存在冲突时返回true并输出错误信息，否则返回false。
	 */
	private boolean searchNodeCompatibility(NavAptMapperNode node){
		String path = node.getPath();
		if (mapperNodeSet.isEmpty()) {
			return false;
		}
		NavAptMapperNode left = mapperNodeSet.ceiling(node);
		if ((left != null)
			&& (left.equals(node) || (left.isPrefix() && node.getPath().startsWith(left.getPath())))
		) {
			mMessage.printMessage(
				Diagnostic.Kind.ERROR,
				IntentNavMethod.class.getCanonicalName() + ": Failed to build intent nav class: same path " +
					"exists:" +
					path,
				node.getMethodElement()
			);
			mMessage.printMessage(
				Diagnostic.Kind.ERROR,
				IntentNavMethod.class.getCanonicalName() + ": Failed to build intent nav class: same path " +
					"exists:" +
					path,
				left.getMethodElement()
			);
			return true;
		}
		NavAptMapperNode right = mapperNodeSet.floor(node);
		if ((right != null)
			&& (right.equals(node) || (node.isPrefix() && right.getPath().startsWith(node.getPath())))) {
			mMessage.printMessage(
				Diagnostic.Kind.ERROR,
				IntentNavMethod.class.getCanonicalName() + ": Failed to build intent nav class: same path " +
					"exists:" +
					path,
				node.getMethodElement()
			);
			mMessage.printMessage(
				Diagnostic.Kind.ERROR,
				IntentNavMethod.class.getCanonicalName() + ": Failed to build intent nav class: same path " +
					"exists:" +
					path,
				right.getMethodElement()
			);
			return true;
		}
		return false;
	}
}
