package edu.udg.tfg.Gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder, JwtAuthenticationFilter jwtAuthenticationFilter) {
        return builder.routes()

                .route("files_root_get", r -> r.path("/files/root")
                        .and().method(HttpMethod.GET, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter))
                        .uri("lb://FileManagement"))

                .route("files_trash_full_get", r -> r.path("/files/trash/full")
                        .and().method(HttpMethod.GET, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter)
                                .rewritePath("/files/trash/full", "/trash/root"))
                        .uri("lb://TRASHSERVICE"))

                .route("files_element_get", r -> r.path("/files/{elementId}")
                        .and().method(HttpMethod.GET, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter)
                                .rewritePath("/files/(?<elementId>.*)", "/files/${elementId}"))
                        .uri("lb://FileManagement"))

                .route("files_element_download_get", r -> r.path("/files/{elementId}/download")
                        .and().method(HttpMethod.GET, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter)
                                .rewritePath("/files/(?<elementId>.*)/download", "/files/${elementId}/download"))
                        .uri("lb://FileManagement"))

                .route("files_get_full", r -> r.path("/files/{folderId}/full")
                        .and().method(HttpMethod.GET, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter))
                        .uri("lb://FileManagement"))

                .route("files_post", r -> r.path("/files")
                        .and().method(HttpMethod.POST, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter))
                        .uri("lb://FileManagement"))

                .route("files_upload", r -> r.path("/upload")
                        .and().method(HttpMethod.POST, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter)
                                .rewritePath("/upload", "/files/upload")
                        )
                        .uri("lb://FileManagement"))

                .route("files_element_put", r -> r.path("/files/{elementId}")
                        .and().method(HttpMethod.PUT, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter)
                                .rewritePath("/files/(?<elementId>.*)", "/files/${elementId}"))
                        .uri("lb://FileManagement"))

                .route("files_move_put", r -> r.path("/files/{elementId}/move/{folderId}")
                        .and().method(HttpMethod.PUT, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter)
                                .rewritePath("/files/(?<elementId>.*)/move/(?<folderId>.*)", "/files/${elementId}/move/${folderId}"))
                        .uri("lb://FileManagement"))

                .route("files_element_delete", r -> r.path("/files/{elementId}")
                        .and().method(HttpMethod.DELETE, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter)
                                .rewritePath("/files/(?<elementId>.*)", "/trash/move/${elementId}"))
                        .uri("lb://TRASHSERVICE"))

                .route("files_copy_element_post", r -> r.path("/files/{elementId}/copy/{newParentId}")
                        .and().method(HttpMethod.POST, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter)
                                .rewritePath("/files/(?<elementId>.*)/copy/(?<newParentId>.*)", "/files/${elementId}/copy/${newParentId}"))
                        .uri("lb://FileManagement"))

                .route("share_get_users", r -> r.path("/share/user/{elementId}")
                        .and().method(HttpMethod.GET)
                        .filters(f -> f.filter(jwtAuthenticationFilter)
                                .rewritePath("/share/user/(?<elementId>.*)", "/shares/user/${elementId}"))
                        .uri("lb://FILESHARING"))

                .route("share_post", r -> r.path("/share")
                        .and().method(HttpMethod.POST, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter))
                        .uri("lb://FILESHARING"))

                .route("share_put", r -> r.path("/share")
                        .and().method(HttpMethod.PUT, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter))
                        .uri("lb://FILESHARING"))

                .route("share_delete", r -> r.path("/share/{elementId}/user/{username}")
                        .and().method(HttpMethod.DELETE, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter)
                                .rewritePath("/share/(?<elementId>.*)/user/(?<username>.*)", "/shares/${elementId}/users/${username}"))
                        .uri("lb://FILESHARING"))

                .route("share_root_get", r -> r.path("/share/root")
                        .and().method(HttpMethod.GET, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter))
                        .uri("lb://FILESHARING"))

                .route("trash_root_get", r -> r.path("/trash/root")
                        .and().method(HttpMethod.GET, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter))
                        .uri("lb://TRASHSERVICE"))

                .route("trash_restore_put", r -> r.path("/trash/{elementId}/restore")
                        .and().method(HttpMethod.PUT, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter))
                        .uri("lb://TRASHSERVICE"))

                .route("trash_element_delete", r -> r.path("/trash/{elementId}")
                        .and().method(HttpMethod.DELETE, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter))
                        .uri("lb://TRASHSERVICE"))

                .route("user_login_post", r -> r.path("/users/auth/login")
                        .and().method(HttpMethod.POST, HttpMethod.OPTIONS)
                        .uri("lb://USERAUTHENTICATION"))

                .route("user_register_post", r -> r.path("/users/auth/register")
                        .and().method(HttpMethod.POST, HttpMethod.OPTIONS)
                        .filters(f -> f.removeRequestHeader(HttpHeaders.AUTHORIZATION))
                        .uri("lb://USERAUTHENTICATION"))

                .route("user_keep_alive_post", r -> r.path("/users/auth/keep-alive")
                        .and().method(HttpMethod.POST, HttpMethod.OPTIONS)
                        .uri("lb://USERAUTHENTICATION"))

                .route("user_check_post", r -> r.path("/users/auth/check")
                        .and().method(HttpMethod.POST, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter))
                        .uri("lb://USERAUTHENTICATION"))

                .route("user_password_put", r -> r.path("/users/password")
                        .and().method(HttpMethod.PUT, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter)
                                .rewritePath("/users/password", "/users/auth/password"))
                        .uri("lb://USERAUTHENTICATION"))

                .route("user_get", r -> r.path("/users")
                        .and().method(HttpMethod.GET, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter))
                        .uri("lb://UserManagement"))

                .route("admin_users_get", r -> r.path("/admin/users")
                        .and().method(HttpMethod.GET, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter))
                        .uri("lb://UserManagement"))

                .route("admin_user_get", r -> r.path("/admin/users/{username}")
                        .and().method(HttpMethod.GET, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter)
                                .rewritePath("/admin/users/(?<username>.*)", "/admin/users/${username}"))
                        .uri("lb://UserManagement"))

                .route("admin_user_put", r -> r.path("/admin/users/{userId}")
                        .and().method(HttpMethod.PUT, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter)
                                .rewritePath("/admin/users/(?<userId>.*)", "/admin/users/${userId}"))
                        .uri("lb://UserManagement"))

                .route("admin_user_delete", r -> r.path("/admin/users/{username}")
                        .and().method(HttpMethod.DELETE, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter)
                                .rewritePath("/admin/users/(?<username>.*)", "/admin/users/${username}"))
                        .uri("lb://UserManagement"))

                .route("users_search_get", r -> r.path("/users/search")
                        .and().method(HttpMethod.GET, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter))
                        .uri("lb://UserManagement"))

                .route("user_check_email_get", r -> r.path("/users/check-email")
                        .and().method(HttpMethod.GET, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter))
                        .uri("lb://UserManagement"))

                .route("user_check_username_get", r -> r.path("/users/check-username")
                        .and().method(HttpMethod.GET, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter)
                                .rewritePath("/users/check-username", "/users/auth/check-username"))
                        .uri("lb://USERAUTHENTICATION"))

                .route("user_id_get", r -> r.path("/users/{username}/id")
                        .and().method(HttpMethod.GET, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter)
                                .rewritePath("/users/(?<username>.*)/id", "/users/auth/${username}/id"))
                        .uri("lb://USERAUTHENTICATION"))

                .route("user_profile_put", r -> r.path("/users/profile")
                        .and().method(HttpMethod.PUT, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter)
                                .rewritePath("/users/profile", "/users"))
                        .uri("lb://UserManagement"))

                .route("user_delete", r -> r.path("/users")
                        .and().method(HttpMethod.DELETE, HttpMethod.OPTIONS)
                        .filters(f -> f.filter(jwtAuthenticationFilter)
                                .rewritePath("/users", "/users"))
                        .uri("lb://UserManagement"))

                .route("websocket", r -> r.path("/websocket")
                        .and().method(HttpMethod.GET)
                        .filters(f -> f.setRequestHeader(HttpHeaders.UPGRADE, "websocket")
                                .setRequestHeader(HttpHeaders.CONNECTION, "Upgrade")
                                .filter(jwtAuthenticationFilter))
                        .uri("lb://SyncService"))

                .route("websocket_web", r -> r.path("/websocket/web")
                        .and().method(HttpMethod.GET)
                        .filters(f -> f.setRequestHeader(HttpHeaders.UPGRADE, "websocket")
                                .setRequestHeader(HttpHeaders.CONNECTION, "Upgrade")
                                .filter(jwtAuthenticationFilter))
                        .uri("lb://SyncService"))

                .build();
    }
}
