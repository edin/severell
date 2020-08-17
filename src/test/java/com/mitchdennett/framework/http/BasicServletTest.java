package com.mitchdennett.framework.http;

import com.mitchdennett.framework.container.Container;
import com.mitchdennett.framework.middleware.MiddlewareMapper;
import org.apache.commons.dbcp2.BasicDataSource;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BasicServletTest {

    @Test
    public void testDoGet() throws IOException, InvocationTargetException, IllegalAccessException {
        Container c = mock(Container.class);
        Router r = mock(Router.class);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        setUpRequest(c, r, req, "GET", new ArrayList<>());

        BasicServlet servlet = new BasicServlet(c, r);
        servlet.doGet(req, resp);

        ArgumentCaptor<Object> objectCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Method> methodCaptor = ArgumentCaptor.forClass(Method.class);
        ArgumentCaptor<Request> reqCaptor = ArgumentCaptor.forClass(Request.class);
        ArgumentCaptor<Response> respCaptor = ArgumentCaptor.forClass(Response.class);

        verify(c).invoke(reqCaptor.capture(), respCaptor.capture(), methodCaptor.capture(), objectCaptor.capture());

        assertEquals("GET", reqCaptor.getValue().getMethod());


    }

    @Test
    public void testDoPost() throws InvocationTargetException, IllegalAccessException, IOException {
        Container c = mock(Container.class);
        Router r = mock(Router.class);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        setUpRequest(c, r, req, "POST",new ArrayList<>());

        BasicServlet servlet = new BasicServlet(c, r);
        servlet.doPost(req, resp);

        ArgumentCaptor<Object> objectCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Method> methodCaptor = ArgumentCaptor.forClass(Method.class);
        ArgumentCaptor<Request> reqCaptor = ArgumentCaptor.forClass(Request.class);
        ArgumentCaptor<Response> respCaptor = ArgumentCaptor.forClass(Response.class);

        verify(c).invoke(reqCaptor.capture(), respCaptor.capture(), methodCaptor.capture(), objectCaptor.capture());

        assertEquals("POST", reqCaptor.getValue().getMethod());

    }

    @Test
    public void testDoGetWithBeforeMiddleware() throws InvocationTargetException, IllegalAccessException, IOException {
        Container c = mock(Container.class);
        Router r = mock(Router.class);
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);

        doMiddlewareTest(c, r, req, resp, "DefaultMiddlewareBefore");

        doMiddlewareTest(c, r, req, resp, "DefaultMiddlewareAfter");
    }

    private void doMiddlewareTest(Container c, Router r, HttpServletRequest req, HttpServletResponse resp, String middlewareType) throws IOException, InvocationTargetException, IllegalAccessException {
        MiddlewareMapper mapper = mock(MiddlewareMapper.class);
        ArrayList<MiddlewareMapper> beforeList = new ArrayList<>();
        beforeList.add(mapper);

        MiddlewareMapper mapperTwo = mock(MiddlewareMapper.class);
        ArrayList<MiddlewareMapper> defaultList = new ArrayList<>();
        defaultList.add(mapperTwo);
        given(c.make(middlewareType, ArrayList.class)).willReturn(defaultList);

        setUpRequest(c, r, req, "GET", beforeList);

        BasicServlet servlet = new BasicServlet(c, r);
        servlet.doPost(req, resp);

        ArgumentCaptor<Container> contCaptor = ArgumentCaptor.forClass(Container.class);
        ArgumentCaptor<Request> reqCaptor = ArgumentCaptor.forClass(Request.class);
        ArgumentCaptor<Response> respCaptor = ArgumentCaptor.forClass(Response.class);

        verify(mapper).run(contCaptor.capture(), reqCaptor.capture(), respCaptor.capture());

        ArgumentCaptor<Container> contCaptor2 = ArgumentCaptor.forClass(Container.class);
        ArgumentCaptor<Request> reqCaptor2 = ArgumentCaptor.forClass(Request.class);
        ArgumentCaptor<Response> respCaptor2 = ArgumentCaptor.forClass(Response.class);

        verify(mapperTwo).run(contCaptor2.capture(), reqCaptor2.capture(), respCaptor2.capture());
    }

    private void setUpRequest(Container c, Router r, HttpServletRequest req, String method, ArrayList<MiddlewareMapper> middleware) throws IOException {
        BasicDataSource ds = mock(BasicDataSource.class);
        given(c.make(BasicDataSource.class)).willReturn(ds);
        given(req.getMethod()).willReturn(method);
        given(req.getRequestURI()).willReturn("/");
        Route route = mock(Route.class);
        given(r.lookup(any(String.class), any(HttpMethod.class), any(Request.class))).willReturn(route);
        given(route.getMiddlewareBefore()).willReturn(middleware);
//        given(route.getMiddlewareAfter()).willReturn(middleware);
        BufferedReader reader = mock(BufferedReader.class);
        given(req.getReader()).willReturn(reader);
    }
}