package com.smartexpense.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，校验路由权限
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                // 拦截所有路径
                .addPathPatterns("/**")
                // 排除登录、Swagger、静态资源等
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/dept/list",
                        "/dept/tree",
                        "/doc.html",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/favicon.ico",
                        "/error",
                        "/files/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 发票图片本地存储的静态资源映射：/files/** -> {uploadDir}/
        String absolutePath = new File(uploadDir).getAbsolutePath();
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}
