package uz.ekoulash.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import uz.ekoulash.security.JwtAuthFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(corsSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ── Auth ─────────────────────────────────────────
                        .requestMatchers("/api/auth/**").permitAll()

                        // ── Products — MUHIM: aniq endpointlar avval keladi ──
                        // Chat va mark-sold — authenticated (avval)
                        .requestMatchers(HttpMethod.GET,  "/api/products/*/messages").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/products/*/messages").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/products/*/mark-sold").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/products/*/like").authenticated()

                        // Umumiy product ko'rish — public (keyin)
                        .requestMatchers(HttpMethod.GET, "/api/products").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/*").permitAll()

                        // ── Boshqa public endpointlar ─────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/stats").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/rating").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/branches").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/leaderboard").permitAll() // <-- SHU QATORNI QO'SHING
                        .requestMatchers("/uploads/**").permitAll()

                        // ── Admin ─────────────────────────────────────────
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ── Qolgan hamma narsa — authenticated ────────────
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        var config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}