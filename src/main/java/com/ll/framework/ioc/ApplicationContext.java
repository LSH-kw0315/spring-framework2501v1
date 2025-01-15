package com.ll.framework.ioc;

import com.ll.standard.util.Ut;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class ApplicationContext {

    Map<String, Object> beanFactory = new HashMap<>();

    public void SourceLoad() {
        String sourceDir = "C:\\Users\\LSH\\Desktop\\멋사\\spring-framework2501v1\\src\\main\\java"; // .java 파일이 있는 디렉토리 경로
        
        try {
            // .java 파일로 된 파일 목록 가져오기
            List<File> javaFiles = findJavaFiles(new File(sourceDir));
            List<Class<?>> classList = new ArrayList<>();


            // forName을 통해 Class 정보를 획득
            for (File javaFile : javaFiles) {
                classList.add(getClass(javaFile, sourceDir));
            }

            for (Class<?> _class : classList) {
                // 모든 생성자 확인
                Constructor<?>[] constructors = _class.getDeclaredConstructors();

                //등록되어 있으면 스킵함.
                if(beanFactory.containsKey(extractBeanName(_class.getName()))) continue;

                //빈 팩토리 등록과 리턴을 동시에 하는 것은 아쉬운 점.
                generateObjectFromConstructor(_class,constructors);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //빈에 저장될 때 이름 형식으로 텍스트 가공
    private String extractBeanName(String fullName) {
        int lastSeparator = fullName.lastIndexOf(".");
        return Ut.str.lcfirst(fullName.substring(lastSeparator + 1));
    }

    //생성자 주입을 상정하므로, 파라미터 타입에 있는 모든 걸 빈 팩토리에 등록하는 걸 목적으로 하는 함수임을 알아줬으면 함.
    private Object generateObjectFromConstructor(Class<?> _class, Constructor<?>[] args) throws InvocationTargetException, InstantiationException, IllegalAccessException {

        String className = extractBeanName(_class.getName());

        //빈 팩토리에 있는 경우 그대로 반환해준다. 어느 실행 흐름을 타더라도 결국 빈 팩토리에 등록된 것을 반환하게 된다.
        if (beanFactory.containsKey(className)) {
            return beanFactory.get(className);
        }

        for (Constructor<?> constructor : args) {
            //public이 아닌 생성자를 위해서
            constructor.setAccessible(true);

            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length == 0 && args.length == 1) {
                Object result=constructor.newInstance();
                beanFactory.put(className,result); //기본 생성자인 경우 등록 즉시 put. 위에서 빈 팩토리에 있는지 여부를 미리 검사하므로 상관 없다.
                return result;
            }

            if (paramTypes.length > 0) { //생성자의 인자가 하나 이상인 경우
                Object[] params = new Object[paramTypes.length];
                for (int i = 0; i < paramTypes.length; i++) { //각 파라미터를 빈 팩토리에서 받아온 것으로 등록할 수 있도록 한다.
                    params[i] = generateObjectFromConstructor(paramTypes[i], paramTypes[i].getConstructors());
                }
                Object result =constructor.newInstance(params);
                beanFactory.put(className,result); //위 과정을 완료한 후 즉시 put.
                return result;
            }
        }

        throw new RuntimeException("스캔되지 않은 파일이 있습니다."); //
    }

    // .java 파일 검색하되, domain 이하의 파일로 한정한다.
    public static List<File> findJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    javaFiles.addAll(findJavaFiles(file));
                } else if (file.getName().endsWith(".java") && file.getPath().contains("domain")) {
                    javaFiles.add(file);
                }
            }
        }
        return javaFiles;
    }

    //.java 파일을 이용해 import하는 형태의 클래스 이름을 얻을 수 있으므로 forName으로 클래스 정보를 얻음
    private Class<?> getClass(File javaFile, String sourceDir) throws Exception {
        String className = extractClassName(javaFile, sourceDir);
        return Class.forName(className);
        
    }

    // .java 파일에서 클래스 이름 추출. 실제로 코드에서 쓸 때 import하는 형태로 추출되므로 패키지명+클래스 형태
    private  String extractClassName(File javaFile, String sourceDir) {
        String filePath = javaFile.getAbsolutePath();
        String relativePath = filePath.substring(sourceDir.length()+ 1); // 루트 경로 제외
        return relativePath.replace(File.separatorChar, '.').replaceAll("\\.java$", "");
    }

    public ApplicationContext() {
        SourceLoad();
    }

    public <T> T genBean(String beanName) {
        return (T) beanFactory.get(beanName);
    }
}
