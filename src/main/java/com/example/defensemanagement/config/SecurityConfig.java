package com.example.defensemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF
                .csrf().disable()
                // 禁用 X-Frame-Options（允许iframe嵌入）
                .headers().frameOptions().disable()
                .and()
                // 放行所有请求，权限由应用内 Session 逻辑控制
                .authorizeHttpRequests(authz -> authz
                        .antMatchers("/**").permitAll()
                        .anyRequest().permitAll())
                // 允许匿名访问
                .anonymous().and()
                // 禁用表单登录
                .formLogin().disable()
                // 禁用 HTTP Basic 认证
                .httpBasic().disable()
                // 配置登出
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll());

        return http.build();
    }
}