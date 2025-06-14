package com.saveetha.LeaveManagement.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.addAllowedOrigin("http://localhost:5173"); // frontend origin
		configuration.addAllowedMethod("*");
		configuration.addAllowedHeader("*");
		configuration.setAllowCredentials(true); // allow cookies/sessions if needed

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.cors(withDefaults())
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/auth/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/departments/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/departments/**").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/departments/active/**").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/departments/**").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/departments/update/**").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/departments/delete/**").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/departments/{id}/status").hasAuthority("ADMIN")


						.requestMatchers(HttpMethod.GET, "/api/leave-types").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.GET,"/api/leave-types/active").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/leave-types/create").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/leave-types/{id}").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/leave-types/{id}/status").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/leave-types/**").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.PATCH, "/api/leave-types/deactivate/**").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.PATCH, "/api/leave-types/activate/**").hasAuthority("ADMIN")

						.requestMatchers(HttpMethod.GET, "/api/approval-flows").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/approval-flows/active").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/approval-flows/create").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/approval-flows/**").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/approval-flows/**").hasAuthority("ADMIN")


						.requestMatchers(HttpMethod.GET, "/api/approval-flow-levels/active").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/approval-flow-levels/all").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/approval-flow-levels/flow/**").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/approval-flow-levels/").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/approval-flow-levels/create").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/approval-flow-levels/**").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/approval-flow-levels/**").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.PATCH, "/api/approval-flow-levels/**").hasAuthority("ADMIN")

						.requestMatchers(HttpMethod.GET ,"/api/employees/active").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/employees/update/**").authenticated()
						.requestMatchers(HttpMethod.POST, "/api/employees/create").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/employees").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/employees/{empId}").permitAll()
						.requestMatchers(HttpMethod.DELETE, "/api/employees/{empId}").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.PATCH ,"/api/employees/activate/**").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.PATCH ,"/api/employees/deactivate/**").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.POST ,"/api/employees/upload-picture/{empId}/**").authenticated()
						.requestMatchers(HttpMethod.PUT, "/api/employees/assign-approval-flow").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.GET,"/api/employees/search").hasAuthority("ADMIN")

						.requestMatchers(HttpMethod.GET, "/api/notifications/{empId}").authenticated()
						.requestMatchers(HttpMethod.POST, "/api/leave").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/leave-request/search").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/leave/apply").hasAnyAuthority("EMPLOYEE", "ADMIN")

						.requestMatchers(HttpMethod.POST,"/api/leave-balance/{empId}").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.GET,"/api/leave-balance/me").permitAll()

						.requestMatchers(HttpMethod.POST, "/api/leave-request/**").authenticated()
						.requestMatchers(HttpMethod.PATCH, "/api/leave-request/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/leave-request/all").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/leave-request/{id}").permitAll()
						.requestMatchers(HttpMethod.DELETE, "/api/leave-request/{id}").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.POST, "api/leave-request/upload-file/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/leave-request/leave-history").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/leave-request/withdraw/{leaveRequestId}").permitAll()


						.requestMatchers(HttpMethod.POST, "/api/leave-alteration/**").permitAll()
						.requestMatchers(HttpMethod.PATCH, "/api/leave-alteration/**").permitAll()
						.requestMatchers(HttpMethod.PATCH, "/api/leave-alteration/reject/**").permitAll()
						.requestMatchers(HttpMethod.PATCH, "/api/leave-alteration/approve/**").permitAll()
						.requestMatchers(HttpMethod.PATCH, "/api/leave-approval/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/leave-alteration/{id}").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/leave-alteration/notification-status/**").permitAll()
						.requestMatchers(HttpMethod.PUT, "/api/leave-alteration/update/{id}").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/leave-alteration/all").hasAuthority("ADMIN")

						.requestMatchers(HttpMethod.GET, "/api/notifications/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/leave-approval/all").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/leave-approval/{approvalId}").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/leave-approval/status/**").permitAll()
						.requestMatchers(HttpMethod.DELETE, "/api/leave-approval/delete/{approvalId}").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/leave-approval/update/{approvalId}").hasAuthority("ADMIN")
						.requestMatchers(HttpMethod.PATCH, "/api/leave-approval/process/{approvalId}").authenticated()
						.requestMatchers(HttpMethod.POST, "/api/leave-approval/initiate/{leaveRequestId}").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/leave-approval/approver/pending-requests").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/leave-balance-all").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/leave-balance/me").authenticated()
						.anyRequest().authenticated()
				)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}