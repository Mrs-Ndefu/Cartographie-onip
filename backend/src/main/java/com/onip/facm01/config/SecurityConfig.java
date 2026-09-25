package com.onip.facm01.config;

import com.onip.facm01.security.JwtAuthFilter;
import com.onip.facm01.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            org.springframework.core.env.Environment env) {
        CorsConfiguration configuration = new CorsConfiguration();
        String origins = env.getProperty("app.cors.allowed-origins", "http://localhost:5173");
        configuration.setAllowedOrigins(List.of(origins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (req, res, ex) -> res.sendError(401, "Non authentifié")));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain dashboardFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/dashboard/**", "/login")
                // Droits par rôle (cf. AgentRole) :
                // - SUPER_ADMIN : voit tout, ne pose aucune action sur les ménages ; garde la
                //   gestion des comptes.
                // - ADMIN : tout, dont retirer/restaurer un ménage et définir les zones.
                // - SUPERVISEUR : voit, modifie et restaure les ménages ; gère les agents qui
                //   lui sont affectés et leurs zones.
                // - DIRECTION_GENERALE : uniquement la page de statistiques.
                // - AGENT : pas d'accès au tableau de bord web (app terrain uniquement).
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login").permitAll()
                        .requestMatchers("/dashboard/stats", "/dashboard/profile", "/dashboard/profile/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN", "SUPERVISEUR", "DIRECTION_GENERALE")
                        .requestMatchers(HttpMethod.POST, "/dashboard/households/*/archive")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/dashboard/households/*/restore")
                        .hasAnyRole("ADMIN", "SUPERVISEUR")
                        .requestMatchers("/dashboard/households/*/edit", "/dashboard/households/*/edit-form")
                        .hasAnyRole("ADMIN", "SUPERVISEUR")
                        .requestMatchers(HttpMethod.POST, "/dashboard/zones", "/dashboard/zones/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/dashboard/agents", "/dashboard/agents/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN", "SUPERVISEUR")
                        .requestMatchers(HttpMethod.GET, "/dashboard", "/dashboard/**")
                        .hasAnyRole("SUPER_ADMIN", "ADMIN", "SUPERVISEUR")
                        .anyRequest().denyAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        // Page d'accueil selon le rôle : la DIRECTION_GENERALE n'a accès qu'aux
                        // statistiques, les autres rôles arrivent sur le tableau de bord.
                        .successHandler((request, response, authentication) -> {
                            boolean statsOnly = authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_DIRECTION_GENERALE"));
                            response.sendRedirect(statsOnly ? "/dashboard/stats" : "/dashboard");
                        })
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout"));
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http) throws Exception {
        // Console H2 : uniquement active quand le profil "dev" (base embarquée) tourne.
        // Sans intérêt/inaccessible en profil normal (PostgreSQL), donc sans risque de laisser
        // la route permissive ici.
        http
                .securityMatcher("/h2-console/**")
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(4)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // /error : page vers laquelle Spring renvoie en cas d'erreur serveur. Interdite,
                        // elle transformait n'importe quelle erreur en "403 accès refusé".
                        .requestMatchers("/", "/css/**", "/js/**", "/img/**", "/favicon.ico", "/error").permitAll()
                        .anyRequest().denyAll())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
