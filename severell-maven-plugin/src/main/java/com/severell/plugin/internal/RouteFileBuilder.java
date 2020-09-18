package com.severell.plugin.internal;

import com.severell.core.container.Container;
import com.severell.core.http.*;
import com.squareup.javapoet.*;
import org.apache.maven.shared.utils.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public class RouteFileBuilder {

    public static Path build(Container container, String basePackage) throws IOException {
        Router router = new Router();
        ArrayList<Route> routes = router.getRoutes();

        MethodSpec.Builder builder = getBuildMethodBuilder(container, routes);

        //We now need to compile the default middleware. This middleware gets executed on every route

        MethodSpec.Builder middlewareBuilder = MethodSpec.methodBuilder("buildDefaultMiddleware");
        middlewareBuilder.addModifiers(Modifier.PUBLIC);
        TypeName listOfMethodExecutor = ParameterizedTypeName.get(ArrayList.class, MiddlewareExecutor.class);
        middlewareBuilder.returns(listOfMethodExecutor);
        Class[] middleware = container.make("_MiddlewareList", Class[].class);
        buildMiddleware(container, middlewareBuilder, new ArrayList<Class>(Arrays.asList(middleware)), "defaultMiddleware");
        middlewareBuilder.addStatement("return defaultMiddleware");


        TypeSpec helloWorld = TypeSpec.classBuilder("RouteBuilder")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(builder.build())
                .addMethod(middlewareBuilder.build())
                .build();



        JavaFile javaFile = JavaFile.builder(basePackage, helloWorld)
                .build();

        Path sourceFile   = Files.createTempDirectory("severell");

        try {
            javaFile.writeTo(sourceFile.toFile());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to create migration");
        }

        return sourceFile;
    }

    @NotNull
    private static MethodSpec.Builder getBuildMethodBuilder(Container c, ArrayList<Route> routes) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("build");
        builder.addModifiers(Modifier.PUBLIC);
        TypeName listOfRouteExecutor = ParameterizedTypeName.get(ArrayList.class, RouteExecutor.class);
        builder.returns(listOfRouteExecutor);

        builder.addStatement("$T<$T> list = new $T<>()", ArrayList.class, RouteExecutor.class, ArrayList.class);
        int routeIndex = 0;
        for(Route r : routes) {
            String middlwareListName = "middlewareList" + routeIndex;
            builder.addCode("\n // ********* ROUTE: $S - $S ********* \n", r.getHttpMethod(), r.getPath());
            CodeBlock.Builder LambdaBuilder = CodeBlock.builder()
                    .add("(request, response, container) -> {\n").indent()
                    .addStatement("$T cont = new $T()", r.getMethod().getDeclaringClass(),r.getMethod().getDeclaringClass());
            ArrayList<String> paramList = new ArrayList<String>();

            Class[] params = r.getMethod().getParameterTypes();
            resolve(c, LambdaBuilder, paramList, params);

            CodeBlock Lambda = LambdaBuilder.addStatement("cont.$L($L)", r.getMethod().getName(), StringUtils.join(paramList.iterator(), ","))
                    .unindent().add("}")
                    .build();

            //We need to instantiate and resolve middleware here.
            buildMiddleware(c, builder, r.getMiddlewareClassList(), middlwareListName);

            builder.addStatement("list.add(new $T($S, $S, $L, $L))", RouteExecutor.class, r.getPath(), r.getHttpMethod(), middlwareListName, Lambda.toString());
            routeIndex++;
        }

        builder.addStatement("return list");
        return builder;
    }

    private static void buildMiddleware(Container c, MethodSpec.Builder builder, ArrayList<Class> classList, String middlwareListName) {
        builder.addStatement("$T<$T> " + middlwareListName + " = new $T<>()",ArrayList.class, MiddlewareExecutor.class, ArrayList.class);
        if(classList != null) {
            for (Class mid : classList) {
                CodeBlock.Builder middlewareBuilder = CodeBlock.builder();
                Constructor constr = mid.getConstructors()[0];
                middlewareBuilder
                        .add("(request, response, container, chain) -> {\n").indent();
                Class[] parameters = constr.getParameterTypes();
                ArrayList<String> middlwareParamList = new ArrayList<>();
                resolve(c, middlewareBuilder, middlwareParamList, parameters);
                middlewareBuilder.addStatement("$T middleware = new $T($L)", mid, mid, StringUtils.join(middlwareParamList.iterator(), ","));
                middlewareBuilder.addStatement("middleware.handle(request, response, chain)");
                builder.addStatement(middlwareListName + ".add(new $T($L))", MiddlewareExecutor.class, middlewareBuilder.unindent().add("}").build().toString());
            }

        }
    }

    private static void resolve(Container c, CodeBlock.Builder lambdaBuilder, ArrayList<String> paramList, Class[] params) {
        int count = 0;
        for(Class p : params) {
            if (p == Request.class) {
                paramList.add("request");
            } else if (p == Response.class){
                paramList.add("response");
            } else {
                Object obj = c.make(p);
                if(obj instanceof NeedsRequest) {
                    lambdaBuilder.addStatement("$T p" + count + " = container.make($L)", p, p.getName() + ".class");
                    lambdaBuilder.addStatement("(($T) p" + count + ").setRequest(request)", NeedsRequest.class);
                } else {
                    lambdaBuilder.addStatement("$T p" + count + " = container.make($L)", p, p.getName() + ".class");
                }

                paramList.add("p" + count);
            }
            count++;
        }
    }

}