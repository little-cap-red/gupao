package springframework.v2.core.servlet;

import springframework.v2.core.annotation.GPController;
import springframework.v2.core.annotation.GPRequestParam;
import springframework.v2.core.annotation.GPService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class DispatchServlet extends HttpServlet {

    //保存application.properties 中的内容
    private Properties properties = new Properties();

    //保存扫描到的内容
    private List<String> classNames = new ArrayList<>();

    //IOC容器
    private Map<String,Object> ioc = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //6.调用
        doDispatch();

    }

    private void doDispatch() {

    }

    //初始化阶段
    @Override
    public void init(ServletConfig config) throws ServletException {
        //1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2.扫描相关的类
        doScanner(properties.getProperty("scanPackage"));

        //3.初始化扫描到的类，并将它们放入IOC容器中
        doInstance();

        //4.完成依赖注入
        doAutowired();

        //5.初始化handlerMapping
        initHandlerMapping();
        System.out.println("init complete");
    }

    private void initHandlerMapping() {

    }

    private void doAutowired() {

    }

    private void doInstance() {
        //初始化。为DI做准备
        if(classNames.isEmpty()){ return;}
        try{
            for(String className : classNames){
                Class<?> clazz = Class.forName(className);
                //加了注解的类才初始化
                if(clazz.isAnnotationPresent(GPController.class)){
                    Object instance = clazz.newInstance();
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName,instance);
                }else if(clazz.isAnnotationPresent(GPService.class)){
                    GPService service = clazz.getAnnotation(GPService.class);
                    //1.自定义的beanName
                    String beanName = service.value();
                    if("".equals(beanName.trim())){
                        //2.默认类名首字母小写
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName,instance);
                    //3.很久类型自动赋值。投机取巧的方式
                    for(Class<?> i : clazz.getInterfaces()){
                        if(ioc.containsKey(i.getName())){
                            throw new Exception("the beanName :" + i.getName()+"is exists");
                        }
                        ioc.put(i.getName(),instance);
                    }


                }


            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //将类名首字母小写，当做ioc容器中的beanName
    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    //扫描出相关的类
    private void doScanner(String scanPackage) {
        //通过配置文件得到的是类路径 springframework.v3 需要把他转出包路径 springframework/v3
        URL url = this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.","/"));
        File classpath = new File(url.getFile());
        for (File file : classpath.listFiles()) {
            if(file.isDirectory()){ //如果是文件夹，递归
                doScanner(scanPackage+"."+file.getName());
            }else{
                if(!file.getName().endsWith(".class")){ continue; }
                String calssName = (scanPackage+"."+file.getName()).replace(".class","");
                classNames.add(calssName);
            }
        }
    }

    //加载配置文件
    private void doLoadConfig(String contextConfigLocation) {

        //直接从类路径下找到spring主配置文件所在的路径
        //并且将其读取出来放到properties对象中
        //相当于将 scanPackage=springframework.v3 从配置文件中转移到内存中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(in!=null){
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
