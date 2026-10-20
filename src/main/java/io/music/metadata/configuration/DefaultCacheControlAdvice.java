package io.music.metadata.configuration;

import org.springframework.core.MethodParameter;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Default is {@code Cache-Control: no-cache} for API responses under {@code /api/v1}.
 * Controller overrides to set its own policy.
 */
@ControllerAdvice
class DefaultCacheControlAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        if (request.getURI().getPath().startsWith(PathConfig.API_BASE_PATH)
                && response.getHeaders().getCacheControl() == null) {
            response.getHeaders().setCacheControl(CacheControl.noCache());
        }
        return body;
    }

}
