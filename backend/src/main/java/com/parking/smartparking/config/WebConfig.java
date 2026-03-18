package com.parking.smartparking.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // We lock EVERY endpoint in /api/parking, but we deliberately omit /api/auth so users can login!
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/parking/**");
    }
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Global CORS allowing Frontend requests from everywhere safely.
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
