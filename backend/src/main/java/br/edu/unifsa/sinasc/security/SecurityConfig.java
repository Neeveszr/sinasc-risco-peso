package br.edu.unifsa.sinasc.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import br.edu.unifsa.sinasc.usuario.UsuarioRepository;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtService jwtService,
                                           UsuarioRepository usuarioRepository) throws Exception {
        JwtAuthFilter jwtFilter = new JwtAuthFilter(jwtService, usuarioRepository);

        http
                .csrf(AbstractHttpConfigurer::disable) // API stateless com JWT no cabeçalho, sem cookies
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                escrever(res, 401, "Não autenticado", "Token ausente, inválido ou expirado"))
                        .accessDeniedHandler((req, res, ex) ->
                                escrever(res, 403, "Acesso negado", "Seu perfil não tem permissão para esta operação")))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Senhas sempre com BCrypt. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** CORS para o front-end. Origens permitidas vêm de CORS_ORIGINS (separadas por vírgula). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String[] origens) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origens).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", config);
        return fonte;
    }

    private static void escrever(HttpServletResponse res, int status, String titulo, String detalhe)
            throws IOException {
        res.setStatus(status);
        res.setContentType("application/problem+json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"title\":\"" + titulo + "\",\"status\":" + status
                + ",\"detail\":\"" + detalhe + "\"}");
    }
}
