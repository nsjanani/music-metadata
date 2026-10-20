package io.music.metadata.configuration;

import java.util.Arrays;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves every controller that implements a generated API interface under {@code /api/v1}.
 */
@Configuration(proxyBeanMethods = false)
class PathConfig implements WebMvcConfigurer {

    static final String API_BASE_PATH = "/api/v1";

    private static final String GENERATED_API_PACKAGE = "io.music.metadata.api";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(API_BASE_PATH, PathConfig::implementsGeneratedApi);
    }

    private static boolean implementsGeneratedApi(Class<?> controller) {
        return Arrays.stream(controller.getInterfaces())
                .anyMatch(api -> api.getPackageName().equals(GENERATED_API_PACKAGE));
    }

}
