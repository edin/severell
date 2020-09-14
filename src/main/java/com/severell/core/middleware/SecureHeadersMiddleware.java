package com.severell.core.middleware;

import com.severell.core.exceptions.ControllerException;
import com.severell.core.exceptions.MiddlewareException;
import com.severell.core.http.MiddlewareChain;
import com.severell.core.http.Request;
import com.severell.core.http.Response;

import java.util.HashMap;

/**
 * Set secure headers on the response. As
 */
public class SecureHeadersMiddleware implements Middleware{

    private final HashMap<String, String> headers;

    public SecureHeadersMiddleware() {
        headers = new HashMap<>();
        headers.put("Strict-Transport-Security", "max-age=63072000; includeSubdomains");
        headers.put("X-Frame-Options", "SAMEORIGIN");
        headers.put("X-XSS-Protection", "1; mode=block");
        headers.put("X-Content-Type-Options", "nosniff");
        headers.put("Referrer-Policy", "no-referrer, strict-origin-when-cross-origin");
        headers.put("Cache-control", "no-cache, no-store, must-revalidate");
        headers.put("Pragma", "no-cache");
    }

    @Override
    public void handle(Request request, Response response, MiddlewareChain chain) throws MiddlewareException, ControllerException {
        response.headers(this.headers);
        chain.next();
    }
}
