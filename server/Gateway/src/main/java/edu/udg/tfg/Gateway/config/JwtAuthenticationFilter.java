package edu.udg.tfg.Gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.udg.tfg.Gateway.responses.UserAuthResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;

@Component
public class JwtAuthenticationFilter implements GatewayFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String X_USER_ID_HEADER = "X-User-Id";
    private static final String X_CONNECTION_ID_HEADER = "X-Connection-Id";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${access.token.name}")
    private String accessTokenName;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    public JwtAuthenticationFilter(@LoadBalanced WebClient.Builder webClientBuilder, @Value("${auth.service.url}") String authServiceUrl, ObjectMapper objectMapper1) {
        this.authServiceUrl = authServiceUrl;
        this.webClient = webClientBuilder.baseUrl("http://UserAuthentication").build();
        this.objectMapper = objectMapper1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();
        String authHeader = "Bearer ";
        String p = request.getPath().toString();

        if(p.toString().startsWith("/websocket/web")) {
            authHeader = "Bearer ".concat(request.getQueryParams().getFirst("token"));
        } else {

            if (!headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                logger.warn("Missing Authorization header");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("Invalid Authorization header");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        }

        String accessToken = authHeader.substring(7); 

        return webClient.method(HttpMethod.POST)
                .uri("/users/auth/check")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken) 
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    logger.error("Invalid token response from auth service");
                    return Mono.error(new InvalidTokenException());
                })
                .bodyToMono(UserAuthResponse.class)
                .flatMap(userAuthResponse -> {
                    if (userAuthResponse.getId() == null || userAuthResponse.getRole() == null) {
                        logger.warn("User {} with role {} attempted to access admin route {}", userAuthResponse.getUsername(), userAuthResponse.getRole(), request.getPath());
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        return exchange.getResponse().setComplete();
                    }
                    if (request.getPath().toString().startsWith("/admin/") && !("ADMIN".equals(userAuthResponse.getRole()) || "SUPER_ADMIN".equals(userAuthResponse.getRole()))) {
                        logger.warn("User {} with role {} attempted to access admin route {}", userAuthResponse.getUsername(), userAuthResponse.getRole(), request.getPath());
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        return exchange.getResponse().setComplete();
                    }

                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header(X_USER_ID_HEADER, userAuthResponse.getId().toString())
                            .header(X_CONNECTION_ID_HEADER, userAuthResponse.getConnectionId())
                            .build();
                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                })
                .onErrorResume(e -> {
                    logger.error("Error during authentication process", e);
                    exchange.getResponse().setStatusCode(
                            e instanceof InvalidTokenException ? HttpStatus.UNAUTHORIZED : HttpStatus.INTERNAL_SERVER_ERROR
                    );
                    return exchange.getResponse().setComplete();
                });
    }
}
