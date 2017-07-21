package process;

import com.annotation.Seriable;
import com.google.auto.service.AutoService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
public class BeanProcessor extends AbstractProcessor { // 元素操作的辅助类
    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Seriable.class.getCanonicalName());
    }

    @Override public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
    private void log(String message) {
        messager.printMessage(Diagnostic.Kind.WARNING, message);
        //messager.printMessage(Diagnostic.Kind.ERROR, message);
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        // 元素操作的辅助类
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        StringBuffer stringBuffer = new StringBuffer();

        Set<? extends Element> elememts = roundEnv
                .getElementsAnnotatedWith(Seriable.class);
        TypeElement classElement = null;
        List<VariableElement> fields = null;
        Map<String, List<VariableElement>> maps = new HashMap<String, List<VariableElement>>();
        // 遍历
        for (Element ele : elememts)
            if (ele.getKind() == ElementKind.CLASS) {
                classElement = (TypeElement) ele;
                maps.put(classElement.getQualifiedName().toString(),
                        fields = new ArrayList<VariableElement>());
                stringBuffer.append("\nclassElement = "+classElement.getQualifiedName());

            } else if (ele.getKind() == ElementKind.FIELD) // 判断该元素是否为成员变量
            {
                VariableElement varELe = (VariableElement) ele;
                TypeElement enclosingElement = (TypeElement) varELe
                        .getEnclosingElement();
                // 拿到key
                String key = enclosingElement.getQualifiedName().toString();
                fields = maps.get(key);
                stringBuffer.append("\nkey = "+key);
                if (fields == null) {
                    maps.put(key, fields = new ArrayList<VariableElement>());
                }
                fields.add(varELe);
            }

        for (String key : maps.keySet()) {
            if (maps.get(key).size() == 0) {
                TypeElement typeElement = elementUtils.getTypeElement(key);
                List<? extends Element> allMembers = elementUtils
                        .getAllMembers(typeElement);
                if (allMembers.size() > 0) {
                    maps.get(key).addAll(ElementFilter.fieldsIn(allMembers));
                }
            }
        }

        generateCodes(maps);
        log("debug log  = "+stringBuffer.toString());
        return true;
    }

    private void generateCodes(Map<String, List<VariableElement>> maps) {
        //File dir = new File("f://apt_test");
        String userdir = System.getProperty("user.dir");
        System.out.println("userdir = "+userdir);
        File dir = new File(userdir,"autoByProcessor");
        if (!dir.exists())
            dir.mkdirs();
        // 遍历map
        for (String key : maps.keySet()) {

            // 创建文件
            File file = new File(dir, key.replaceAll("\\.", "_") + ".txt");
            try {
                /**
                 * 编写json文件内容
                 */
                FileWriter fw = new FileWriter(file);
                fw.append("{").append("class:").append("\"" + key + "\"")
                        .append(",\n ");
                fw.append("fields:\n {\n");
                List<VariableElement> fields = maps.get(key);

                for (int i = 0; i < fields.size(); i++) {
                    VariableElement field = fields.get(i);
                    fw.append("  ").append(field.getSimpleName()).append(":")
                            .append("\"" + field.asType().toString() + "\"");
                    if (i < fields.size() - 1) {
                        fw.append(",");
                        fw.append("\n");
                    }
                }
                fw.append("\n }\n");
                fw.append("}");
                fw.flush();
                fw.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}