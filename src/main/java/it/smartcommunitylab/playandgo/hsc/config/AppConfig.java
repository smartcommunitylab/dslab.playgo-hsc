/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.playandgo.hsc.config;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.swagger.annotations.ApiParam;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Contact;
import springfox.documentation.service.HttpAuthenticationScheme;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * @author raman
 *
 */
@Configuration
@EnableWebMvc
public class AppConfig implements WebMvcConfigurer {

	@Value("${playandgo.engine.uri}")
	private String engineUri;


	@Bean
	OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository clientRegistrationRepository,
			OAuth2AuthorizedClientRepository authorizedClientRepository) {
		OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
				.clientCredentials().build();
		DefaultOAuth2AuthorizedClientManager authorizedClientManager = new DefaultOAuth2AuthorizedClientManager(
				clientRegistrationRepository, authorizedClientRepository);
		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

		return authorizedClientManager;
	}

//	@Bean
//	WebClient webClient(OAuth2AuthorizedClientManager authorizedClientManager) {
//		ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client = new ServletOAuth2AuthorizedClientExchangeFilterFunction(
//				authorizedClientManager);
//		return WebClient.builder().baseUrl(engineUri).apply(oauth2Client.oauth2Configuration()).build();
//	}
//
//	
//	public class LoggingClientCredentialsTokenResponseClient
//			implements OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> {
//
//		private DefaultClientCredentialsTokenResponseClient delegate = new DefaultClientCredentialsTokenResponseClient();
//		private final Logger log = LoggerFactory.getLogger(LoggingClientCredentialsTokenResponseClient.class);
//
//		@Override
//		public OAuth2AccessTokenResponse getTokenResponse(
//				OAuth2ClientCredentialsGrantRequest clientCredentialsGrantRequest) {
//
//			synchronized (delegate) {
//				log.error("Sending request {}", clientCredentialsGrantRequest);
//				OAuth2AccessTokenResponse response = delegate.getTokenResponse(clientCredentialsGrantRequest);
//				log.error("Received response {}", response.getAccessToken().getTokenValue());
//				return response;
//			}
//		}
//	}
	
	@Bean // with spring-boot-starter-web
	WebClient webClient(
	    ClientRegistrationRepository clientRegistrationRepository,
	    OAuth2AuthorizedClientService authorizedClientService
	) {
		
//		OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
//		        .clientCredentials(builder ->
//		            builder.accessTokenResponseClient(
//		                new LoggingClientCredentialsTokenResponseClient()))
//		        .build();

			AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
		        new AuthorizedClientServiceOAuth2AuthorizedClientManager(
		            clientRegistrationRepository, authorizedClientService);
//		    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

		ServletOAuth2AuthorizedClientExchangeFilterFunction oauth = new ServletOAuth2AuthorizedClientExchangeFilterFunction(
				authorizedClientManager
	    );
	    return WebClient.builder()
	      .baseUrl(engineUri)
	      .apply(oauth.oauth2Configuration())
	      .build();
	}
	
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedMethods("*");
	}

	@Bean(name = "messageSource")
	public ResourceBundleMessageSource getResourceBundleMessageSource() {
		ResourceBundleMessageSource source = new ResourceBundleMessageSource();
		source.setBasename("Messages");
		return source;
	}
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
		registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
		registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
	}

	@Bean
	public Docket api() {
		return new Docket(DocumentationType.OAS_30).select()
				.apis(RequestHandlerSelectors.basePackage("it.smartcommunitylab.playandgo.hsc.rest"))
				.paths(PathSelectors.ant("/**/api/**")).build()
				.directModelSubstitute(Pageable.class, SwaggerPageable.class).apiInfo(apiInfo())
				.securitySchemes(Arrays.asList(securitySchema())).securityContexts(Arrays.asList(securityContext()));
	}

	private SecurityScheme securitySchema() {
		return HttpAuthenticationScheme.JWT_BEARER_BUILDER.name("JWT").build();
	}

	private SecurityContext securityContext() {
		return SecurityContext.builder().securityReferences(defaultAuth()).build();
	}

	private List<SecurityReference> defaultAuth() {
		AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
		AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
		authorizationScopes[0] = authorizationScope;
		return Arrays.asList(new SecurityReference("JWT", authorizationScopes));
	}

	private ApiInfo apiInfo() {
		return new ApiInfoBuilder().title("Play&Go HSC Project").version("2.0").license("Apache License Version 2.0")
				.licenseUrl("https://www.apache.org/licenses/LICENSE-2.0").contact(new Contact("SmartCommunityLab",
						"https://http://www.smartcommunitylab.it/", "info@smartcommunitylab.it"))
				.build();
	}

	private static class SwaggerPageable {

		@ApiParam(required = true, value = "Number of records per page", example = "0")
		public int size;

		@ApiParam(required = true, value = "Results page you want to retrieve (0..N)", example = "0")
		public int page;

		@ApiParam(required = false, value = "Sorting option: field,[asc,desc]", example = "nickname,desc")
		public String sort;

	}

}
