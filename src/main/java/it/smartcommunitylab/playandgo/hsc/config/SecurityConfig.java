package it.smartcommunitylab.playandgo.hsc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;

import it.smartcommunitylab.playandgo.hsc.security.JwtAudienceValidator;


@SuppressWarnings("deprecation")
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${spring.security.oauth2.client.provider.oauthprovider.issuer-uri}")
    private String jwtIssuerUri;

    @Value("${spring.security.oauth2.client.registration.oauthprovider.client-id}")
    private String jwtAudience;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
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
		            "/web/**"
		            ).permitAll()
		        .anyRequest().authenticated().and()
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
    }
    
    /*
     * JWT decoder with audience validation
     */

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
