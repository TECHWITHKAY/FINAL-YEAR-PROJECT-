package com.ghana.commoditymonitor.security;

import com.ghana.commoditymonitor.entity.User;
import com.ghana.commoditymonitor.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Argument resolver for {@link CurrentUser} annotation.
 * <p>
 * This resolver extracts the JWT token from the Authorization header,
 * validates it, and resolves the authenticated user. If no token is present
 * or the token is invalid, it returns null (guest user).
 * </p>
 * 
 * <p>This resolver never throws exceptions - guests are valid callers.</p>
 * 
 * <p>Usage pattern in controllers:</p>
 * <pre>
 * {@code
 * @GetMapping("/trends/{id}")
 * public ResponseEntity<?> getTrend(@PathVariable Long id,
 *                                    @CurrentUser UserPrincipal principal) {
 *     // principal is null for guests, populated for authenticated users
 *     return ResponseEntity.ok(analyticsService.getTrend(id, principal));
 * }
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    /**
     * Determines if this resolver supports the given method parameter.
     * 
     * @param parameter the method parameter to check
     * @return true if the parameter is annotated with @CurrentUser
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class);
    }

    /**
     * Resolves the method parameter to a UserPrincipal instance.
     * <p>
     * Extraction logic:
     * <ol>
     *   <li>Extract Bearer token from Authorization header (may be absent)</li>
     *   <li>If token is absent or invalid: return null (guest)</li>
     *   <li>If token is valid: extract username, load user from UserRepository, return UserPrincipal</li>
     * </ol>
     * </p>
     * 
     * @param parameter the method parameter to resolve
     * @param mavContainer the ModelAndViewContainer for the current request
     * @param webRequest the current request
     * @param binderFactory a factory for creating WebDataBinder instances
     * @return UserPrincipal if authenticated, null if guest
     */
    @Override
    public Object resolveArgument(MethodParameter parameter,
                                   ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest,
                                   WebDataBinderFactory binderFactory) {
        try {
            HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
            if (request == null) {
                log.debug("No HttpServletRequest available, returning null (guest)");
                return null;
            }

            String token = extractTokenFromRequest(request);
            if (token == null) {
                log.debug("No token found in request, returning null (guest)");
                return null;
            }

            if (!jwtTokenProvider.validateToken(token)) {
                log.debug("Invalid token, returning null (guest)");
                return null;
            }

            String username = jwtTokenProvider.getUsernameFromToken(token);
            if (username == null) {
                log.debug("Could not extract username from token, returning null (guest)");
                return null;
            }

            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                log.debug("User not found for username: {}, returning null (guest)", username);
                return null;
            }

            if (!user.isActive()) {
                log.debug("User {} is inactive, returning null (guest)", username);
                return null;
            }

            log.debug("Resolved authenticated user: {} with role: {}", username, user.getRole());
            return new UserPrincipal(user.getId(), user.getUsername(), user.getRole());

        } catch (Exception e) {
            log.debug("Exception while resolving current user, returning null (guest): {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts the JWT token from the Authorization header.
     * 
     * @param request the HTTP request
     * @return the JWT token, or null if not present or invalid format
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
