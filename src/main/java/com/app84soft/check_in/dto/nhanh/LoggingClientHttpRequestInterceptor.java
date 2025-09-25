package com.app84soft.check_in.dto.nhanh;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
public class LoggingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        if (log.isDebugEnabled()) {
            log.debug(">>> {} {}", request.getMethod(), request.getURI());
            log.debug(">>> Headers: {}", request.getHeaders());
            if (body != null && body.length > 0) {
                log.debug(">>> Body: {}", new String(body, StandardCharsets.UTF_8));
            }
        }

        ClientHttpResponse resp = execution.execute(request, body);

        byte[] bytes = readAll(resp.getBody());
        if (log.isDebugEnabled()) {
            log.debug("<<< Status: {} {}", resp.getStatusCode().value(), resp.getStatusText());
            log.debug("<<< Headers: {}", resp.getHeaders());
            log.debug("<<< Body: {}", new String(bytes, StandardCharsets.UTF_8));
        }

        return new BufferedClientHttpResponse(resp, bytes);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        if (in == null)
            return new byte[0];
        try (InputStream input = in; ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int r;
            while ((r = input.read(buf)) != -1)
                bos.write(buf, 0, r);
            return bos.toByteArray();
        }
    }
}
