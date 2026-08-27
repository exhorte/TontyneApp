package sn.isi.tontyn.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import sn.isi.tontyn.dto.ApiError;
import sn.isi.tontyn.security.JwtAuthenticationFilter;

@Configuration
@EnableMethodSecurity   // active @PreAuthorize sur les controleurs
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final ObjectMapper objectMapper;

    /** La console H2 n'est exposee que sur le profil h2. */
    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          CorsConfigurationSource corsConfigurationSource,
                          ObjectMapper objectMapper) {
        this.jwtFilter = jwtFilter;
        this.corsConfigurationSource = corsConfigurationSource;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e
                    .authenticationEntryPoint(authenticationEntryPoint())
                    .accessDeniedHandler(accessDeniedHandler()))
            .authorizeHttpRequests(auth -> {
                // /me requiert un jeton valide : declare AVANT le permitAll /api/auth/**
                auth.requestMatchers("/api/auth/me", "/api/auth/email/**", "/api/auth/email")
                        .authenticated();
                auth.requestMatchers("/api/auth/**").permitAll();
                // Protege par son propre jeton secret (X-Reset-Token) ou par le role
                // administrateur, verifies manuellement dans le controleur : voir
                // DevJeuDeDonneesController.
                auth.requestMatchers("/api/dev/jeu-de-donnees/**").permitAll();
                if (h2ConsoleEnabled) {
                    auth.requestMatchers("/h2-console/**").permitAll();
                }
                auth.anyRequest().authenticated();
            })
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        if (h2ConsoleEnabled) {
            // La console H2 s'affiche dans une frame de meme origine.
            http.headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        }
        return http.build();
    }

    /** 401 JSON pour une requete non authentifiee (jeton absent ou invalide). */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) -> ecrire(response, HttpStatus.UNAUTHORIZED,
                "Authentification requise : jeton JWT absent, invalide ou expire.",
                request.getRequestURI());
    }

    /** 403 JSON pour un utilisateur authentifie mais sans le role requis. */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> ecrire(response, HttpStatus.FORBIDDEN,
                "Acces refuse : privileges insuffisants pour cette operation.",
                request.getRequestURI());
    }

    private void ecrire(jakarta.servlet.http.HttpServletResponse response, HttpStatus statut,
                        String message, String chemin) throws java.io.IOException {
        response.setStatus(statut.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiError.of(statut.value(), statut.getReasonPhrase(), message, chemin));
    }
}
