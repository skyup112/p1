package com.example.p1.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // HttpMethod 임포트 추가
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.p1.repository.MemberRepository;
import com.example.p1.domain.Member; // Member 클래스 임포트 추가

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final MemberRepository memberRepository;

    public SecurityConfig(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/status",
                                "/api/auth/check-username",
                                "/api/auth/check-email"
                        ).permitAll()
                        // 경기 관련 API (조회는 permitAll)
                        .requestMatchers("/api/games").permitAll()
                        .requestMatchers("/api/games/**").permitAll()

                        // 팀 관련 API (조회는 permitAll)
                        .requestMatchers("/api/teams").permitAll()
                        .requestMatchers("/api/teams/**").permitAll()

                        // 댓글 관련 API (인증 필요)
                        .requestMatchers("/api/comments/**").authenticated()

                        // Admin specific endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // NEW: 팀 순위 API 권한 설정
                        // GET 요청은 모든 사용자에게 허용
                        .requestMatchers(HttpMethod.GET, "/api/rankings").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/rankings/**").permitAll()
                        // POST, PUT, DELETE 요청은 ADMIN 역할만 허용
                        .requestMatchers(HttpMethod.POST, "/api/rankings").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/rankings/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/rankings/**").hasRole("ADMIN")
                        // /api/rankings/calculate는 이미 @PreAuthorize로 ADMIN만 허용되어 있지만, 여기서도 명시적으로 설정
                        .requestMatchers(HttpMethod.POST, "/api/rankings/calculate").hasRole("ADMIN")


                        // All other requests must be authenticated
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) -> {
                            response.setStatus(HttpStatus.OK.value());
                            response.setContentType("application/json;charset=UTF-8");

                            Map<String, Object> responseBody = new HashMap<>();
                            responseBody.put("message", "Login successful");
                            responseBody.put("username", authentication.getName());

                            String role = authentication.getAuthorities().stream()
                                    .map(GrantedAuthority::getAuthority)
                                    .filter(a -> a.startsWith("ROLE_"))
                                    .map(a -> a.substring(5))
                                    .findFirst()
                                    .orElse("USER");

                            responseBody.put("role", role);

                            memberRepository.findByUsername(authentication.getName()).ifPresent(member -> {
                                responseBody.put("nickname", member.getNickname());
                            });

                            ObjectMapper objectMapper = new ObjectMapper();
                            response.getWriter().write(objectMapper.writeValueAsString(responseBody));
                            response.getWriter().flush();
                        })
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"message\": \"Login failed: " + exception.getMessage() + "\"}");
                            response.getWriter().flush();
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpStatus.OK.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"message\": \"Logout successful\"}");
                            response.getWriter().flush();
                        })
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"message\": \"Access Denied: You do not have sufficient permissions.\"}");
                            response.getWriter().flush();
                        })
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
