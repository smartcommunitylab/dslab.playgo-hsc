package it.smartcommunitylab.playandgo.hsc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;

import it.smartcommunitylab.playandgo.hsc.security.JwtAudienceValidator;


@Configuration
public class SecurityConfig  {

    @Value("${spring.security.oauth2.client.provider.oauthprovider.issuer-uri}")
    private String jwtIssuerUri;

    @Value("${spring.security.oauth2.client.registration.oauthprovider.client-id}")
    private String jwtAudience;

    @Configuration
    @Order(1)
    public class APIConfigurationAdapter {

        @Bean
        public SecurityFilterChain filterChainAPI(HttpSecurity http) throws Exception {
            http.antMatcher("/api/**")
                .authorizeRequests().anyRequest().authenticated()
                .and()
		        .oauth2ResourceServer(oauth2 ->  oauth2.jwt().decoder(jwtDecoder()))
		        // disable request cache, we override redirects but still better enforce it
		        .requestCache((requestCache) -> requestCache.disable())
		        .exceptionHandling()
		        // use 403
		        .authenticationEntryPoint(new Http403ForbiddenEntryPoint())
		        .accessDeniedPage("/accesserror")
		        .and()
		        .csrf()
		        .disable()
		        .cors();
            // we don't want a session for a REST backend
            // each request should be evaluated
            http.sessionManagement()
            		.sessionCreationPolicy(SessionCreationPolicy.STATELESS);

            return http.build();
        }
    }
    
    @Configuration
    @Order(2)
    public class WebConfigurationAdapter {
    	@Bean
        public SecurityFilterChain filterChainWeb(HttpSecurity http) throws Exception {
    		 http.authorizeRequests()
		        .antMatchers(
		        	"/v2/api-docs",
		        	"/v3/api-docs",	
		            "/configuration/ui",
		            "/swagger-resources/**",
		            "/configuration/**",
		            "/swagger-ui.html",
		            "/swagger-ui/**",
		            "/webjars/**",
		            "/publicapi/**",
		            "/login/**",
		            "/lib/**"
		            ).permitAll()
		        .anyRequest().authenticated()
		        .and().oauth2Login()
		        .and().logout().logoutSuccessUrl("/");
    		 return http.build();
        }
    }
    
    /*
     * JWT decoder with audience validation
     */

    @Bean
    public JwtDecoder jwtDecoder() {
        // build validators for issuer + timestamp (default) and audience
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtAudienceValidator(jwtAudience);
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(jwtIssuerUri);
        // build default decoder and then use custom validators
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(jwtIssuerUri);
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator));

        return jwtDecoder;
    }
    
  }
