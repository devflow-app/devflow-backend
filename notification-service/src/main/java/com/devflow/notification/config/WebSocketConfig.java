package com.devflow.notification.config;

import com.devflow.notification.security.JwtUtils;
import com.devflow.notification.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtils jwtUtils;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilita un broker simple para enviar mensajes a destinos prefijados con /topic o /queue
        config.enableSimpleBroker("/topic", "/queue");
        // Prefijo para los mensajes que se dirigen a controladores anotados con @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
        // Prefijo para enviar mensajes específicos a un usuario (e.g. /user/queue/notifications)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-notifications")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new WebSocketHandshakeInterceptor());
        registry.addEndpoint("/ws-notifications")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new WebSocketHandshakeInterceptor())
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = null;
                    
                    // 1. Extraer del header STOMP 'Authorization'
                    List<String> authorization = accessor.getNativeHeader("Authorization");
                    if (authorization != null && !authorization.isEmpty()) {
                        String bearerToken = authorization.get(0);
                        if (bearerToken.startsWith("Bearer ")) {
                            token = bearerToken.substring(7);
                        }
                    }
                    
                    // 2. Extraer del header STOMP 'token'
                    if (token == null) {
                        List<String> tokenHeader = accessor.getNativeHeader("token");
                        if (tokenHeader != null && !tokenHeader.isEmpty()) {
                            token = tokenHeader.get(0);
                        }
                    }
                    
                    // 3. Extraer de los atributos de la sesión (recuperado por HandshakeInterceptor)
                    if (token == null && accessor.getSessionAttributes() != null) {
                        token = (String) accessor.getSessionAttributes().get("token");
                    }

                    if (token != null && jwtUtils.isTokenValid(token)) {
                        try {
                            String email = jwtUtils.extractEmail(token);
                            UUID userId = jwtUtils.extractUserId(token);
                            String role = jwtUtils.extractRole(token);
                            
                            UserPrincipal principal = new UserPrincipal(userId, email, role);
                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                            
                            accessor.setUser(auth);
                            log.info("WebSocket connection authenticated for user: {}", email);
                        } catch (Exception e) {
                            log.error("Failed to authenticate WebSocket connection", e);
                            throw new IllegalArgumentException("Authentication failed: " + e.getMessage());
                        }
                    } else {
                        log.warn("Rejected WebSocket connection: Token is missing or invalid");
                        throw new IllegalArgumentException("Unauthorized: Token is missing or invalid");
                    }
                }
                return message;
            }
        });
    }

    private static class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
            if (request instanceof ServletServerHttpRequest) {
                ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                String token = servletRequest.getServletRequest().getParameter("token");
                if (token != null && !token.trim().isEmpty()) {
                    attributes.put("token", token);
                }
            }
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
        }
    }
}
