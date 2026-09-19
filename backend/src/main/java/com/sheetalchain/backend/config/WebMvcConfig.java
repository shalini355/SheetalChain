package com.sheetalchain.backend.config;

import com.sheetalchain.backend.security.SecurityInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Web MVC Configuration to register SecurityInterceptor.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final SecurityInterceptor securityInterceptor;

    @Autowired
    public WebMvcConfig(SecurityInterceptor securityInterceptor) {
        this.securityInterceptor = securityInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityInterceptor)
                .addPathPatterns("/api/**");
    }
}
