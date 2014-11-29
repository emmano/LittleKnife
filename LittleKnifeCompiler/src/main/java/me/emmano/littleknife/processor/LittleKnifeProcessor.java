package me.emmano.littleknife.processor;

import com.squareup.javawriter.JavaWriter;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import me.emmano.littleknifeapi.InjectView;

// We need to tell the processor what Annotations it should process.
// getSupportedAnnotationTypes() can also be overridden for this purpose, specially if this processor handles many Annotations.
@SupportedAnnotationTypes("me.emmano.littleknifeapi.InjectView")
public class LittleKnifeProcessor extends AbstractProcessor {

    private static final String LITTLE_KNIFE_TAG = "$$LittleKnife";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        //We are only going to deal with one annotation; @InjectView, therefore we are interested in getting the Elements that are tagged with that Annotation.
        //You can think of Elements as a the components of a Java class; i.e. variables, methods, classes, package, etc.
        //In our case Elements will be the fields Annotated with @InjectView since InjectView.class has a @Target() of FIELD.
        final Set<? extends Element> annotatedElements = roundEnv
                .getElementsAnnotatedWith(InjectView.class);

        //process() gets called more than once, annotatedElements might be empty an empty Set in one of those calls( i.e. when there are no annotations to process this round).
        if (annotatedElements.size() > 0) {

            // Just get the first element out of the Set to gather information about our target; the Activity with @InjectView.
            // This information will be used when creating a new .java file during compile-time.
            final Element firstAnnotatedElement = annotatedElements.iterator().next();
            // Get the package name of the target Activity
            final String packageName = processingEnv.getElementUtils()
                    .getPackageOf(firstAnnotatedElement).toString();
            // Get the name of the target Activity.
            // Since our Element is a field, getEnclosingElement() will return an Element that represents the target class.
            final String hostActivityName = firstAnnotatedElement.getEnclosingElement()
                    .getSimpleName().toString();
            // Create the name of the compile-time generate .java file
            final String newSourceName = hostActivityName + LITTLE_KNIFE_TAG;

            try {
                // JavaWriter is a library from Square that makes creating a new Java file simpler.

                JavaWriter writer = createWriter(newSourceName);
                // Create different sections of the actual compile-time generated .java source
                createHeader(annotatedElements, packageName, writer);
                beginType(newSourceName, writer);
                beginMethod(hostActivityName, writer);
                emitStatements(annotatedElements, hostActivityName, writer);
                emitClose(writer);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
              // return false so other rounds can process this Annotation if needed.
        return false;
    }

    private JavaWriter createWriter(String newSourceName) throws IOException {
        final JavaFileObject sourceFile = processingEnv.getFiler()
                .createSourceFile(newSourceName);
        return new JavaWriter(sourceFile.openWriter());
    }

    private void createHeader(Set<? extends Element> annotatedElements, String packageName,
            JavaWriter writer)
            throws IOException {
        writer.emitPackage(packageName);
        // Map is created to include imports for the same type once on the compile-time generated file.
        //i.e.
        /*
        * @InjectView(R.id.text)
        * TextView text;
        *
        * @InjectView(R.id.text)
        * TextView text1
        *
        * would produce on compile-time generated source:
        *
        * import android.widget.TextView
        * import android.widget.TextView
        *
        * if we do not filter duplicates, JavaWriter will throw an IllegalArgumentException.
        * */
        Map<String, Element> nonRepeatedImports = new HashMap<>();

        for (Element element : annotatedElements) {
            TypeMirror elementType = element.asType();
            // Check if we are a subtype of View. @InjectView can only be used if the Annotated field is of type View.
            if (isSubtypeOfType(elementType, "android.view.View")) {
                nonRepeatedImports.put(element.asType().toString(), element);
            } else {
                processingEnv.getMessager()
                        .printMessage(Diagnostic.Kind.ERROR, String.format(
                                "Variable: %s, is not of a type that subclasses android.view.View. @%s can only be used with Views",
                                element.getSimpleName().toString(),
                                InjectView.class.getSimpleName()));
            }
        }
        for (String importString : nonRepeatedImports.keySet()) {
            writer.emitImports(importString);
        }
    }

    private boolean isSubtypeOfType(TypeMirror typeMirror, String otherType) {


        if (typeMirror instanceof NoType) {
            // NoType is returned if we reached java.langObject on the previous recursion. Not a subtype of android.view.View
            return false;
        }

        if (otherType.equals(typeMirror.toString())) {
            // Annotated field is a child of android.view.View
            return true;
        }

        DeclaredType declaredType = (DeclaredType) typeMirror;
        Element element = declaredType.asElement();
        TypeElement typeElement = (TypeElement) element;
        TypeMirror superType = typeElement.getSuperclass();
        if (isSubtypeOfType(superType, otherType)) {
            // This current superType is not android.view.View, but we can keep looking.
            return true;
        }
        return false;
    }

    private void beginType(String newSourceName, JavaWriter writer) throws IOException {
        writer.beginType(newSourceName, "class");
    }

    private void beginMethod(String hostActivityname, JavaWriter writer) throws IOException {
        writer.beginMethod("void", "inject", EnumSet.of(Modifier.PUBLIC), hostActivityname,
                "target");
    }

    private void emitStatements(Set<? extends Element> annotatedElements, String hostActivityname,
            JavaWriter writer) throws IOException {
        for (Element element : annotatedElements) {
            writer.emitStatement("((" + hostActivityname + ")target)." + element
                    .getSimpleName().toString() + " = " + "(" + element.asType()
                    .toString() + ")target.findViewById(" + element.getAnnotation(InjectView.class)
                    .value() + ")");
        }
    }

    private void emitClose(JavaWriter writer) throws IOException {
        writer.endMethod();
        writer.endType();
        writer.close();
    }
}
