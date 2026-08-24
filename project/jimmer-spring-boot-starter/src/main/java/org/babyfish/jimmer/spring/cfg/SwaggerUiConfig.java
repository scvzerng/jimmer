package org.babyfish.jimmer.spring.cfg;

import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Enaium
 */
public class SwaggerUiConfig implements WebMvcConfigurer {
    private final String path;
    private final String version;

    public SwaggerUiConfig(String uiPath, @Nullable String version) {
        int secondSlash = uiPath.indexOf("/", 1);
        this.path = secondSlash == -1 ? uiPath : uiPath.substring(0, secondSlash);
        this.version = version == null || version.isEmpty() ? SwaggerUiVersion.DEFAULT_VALUE : version;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(
                "/swagger-ui.css",
                "/swagger-ui-bundle.js",
                "/swagger-ui-standalone-preset.js",
                "/index.css",
                "/favicon-32x32.png",
                "/favicon-16x16.png"
        ).addResourceLocations("classpath:META-INF/resources/webjars/swagger-ui/" + version + "/");
    }
}
