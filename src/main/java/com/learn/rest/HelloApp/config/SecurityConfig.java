package com.learn.rest.HelloApp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Spring Security Configuration
 * Reads static user credentials from application.properties and configures security settings.
 * All endpoints require authentication EXCEPT H2 console.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.user.name}")
    private String username;

    @Value("${spring.security.user.password}")
    private String password;

    private final AuthLoggingFilter authLoggingFilter;

    public SecurityConfig(AuthLoggingFilter authLoggingFilter) {
        this.authLoggingFilter = authLoggingFilter;
    }

    /**
     * Define user details service with static credentials from application.properties
     *
     * @return UserDetailsService with in-memory user
     */
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
                .username(username)
                .password(passwordEncoder().encode(password))
                .roles("ADMIN", "USER")
                .build();

        return new InMemoryUserDetailsManager(user);
    }

    /**
     * Password encoder bean using BCrypt
     *
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configure HTTP security
     * - H2 console: No authentication required
     * - All other endpoints: Authentication required
     *
     * @param http HttpSecurity object
     * @return SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .addFilterBefore(authLoggingFilter, BasicAuthenticationFilter.class)
                .authorizeHttpRequests(authz -> authz
                        // Allow H2 console without authentication
                        .requestMatchers("/h2-console", "/h2-console/**").permitAll()
                        // Allow welcome UI page and static resources without authentication
                        .requestMatchers("/welcome-dashboard", "/welcome.html", "/css/**", "/js/**", "/images/**").permitAll()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> {})
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable())); // Allow H2 console frames

        return http.build();
    }
}
