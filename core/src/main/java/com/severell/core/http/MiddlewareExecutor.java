package com.severell.core.http;

import com.severell.core.container.Container;

/**
 * This class holds the reference to the {@link MiddlewareFunction}.
 */
public class MiddlewareExecutor {

    @FunctionalInterface
    public interface MiddlewareFunction {
        void apply(Request request, Response response, Container container, MiddlewareChain chain) throws Exception;
    }

    private final MiddlewareFunction func;

    public MiddlewareExecutor(MiddlewareFunction func) {
        this.func = func;
    }

    public void execute(Request request, Response response, Container c, MiddlewareChain chain) throws Exception {
        func.apply(request, response, c, chain);
    }
}
