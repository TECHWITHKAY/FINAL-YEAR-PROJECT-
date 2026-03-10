package com.ghana.commoditymonitor.security;

import java.lang.annotation.*;

/**
 * Annotation to inject the currently authenticated user into controller methods.
 * <p>
 * Unlike @AuthenticationPrincipal, this annotation allows for optional authentication.
 * If no valid token is present, the parameter will be null (guest user).
 * If a valid token is present, the parameter will contain the UserPrincipal.
 * </p>
 * 
 * <p>Usage example:</p>
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
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
