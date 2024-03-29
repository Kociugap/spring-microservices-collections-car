package pl.pakoc.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import pl.pakoc.dto.UserDto;
import pl.pakoc.model.User;
import pl.pakoc.service.UserService;

public class AuthenticationFilter extends UsernamePasswordAuthenticationFilter {
	
	private UserService userService;
	private Environment environment;
	
	public AuthenticationFilter(UserService userService, Environment environment, AuthenticationManager authenticationManager) {
		this.userService = userService;
		this.environment = environment;
		super.setAuthenticationManager(authenticationManager);
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException {
		
		try {
			
			User credentionals = new ObjectMapper().readValue(request.getInputStream(), User.class);
			
			return getAuthenticationManager().authenticate(
					new UsernamePasswordAuthenticationToken
					(credentionals.getEmail(), credentionals.getPassword(), new ArrayList<>()));
		} catch(IOException e) {
			throw new RuntimeException();
		}
		
	}

	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
			Authentication authResult) throws IOException, ServletException {
		
		String username = ((org.springframework.security.core.userdetails.User) authResult.getPrincipal()).getUsername();
		UserDto userDetails = userService.getUserDetailsByEmail(username);
		
		String token = Jwts.builder()
		.setSubject(userDetails.getId().toString())
		.setExpiration(new Date(System.currentTimeMillis() + Long.parseLong(environment.getProperty("token.expiration_time"))))
		.signWith(SignatureAlgorithm.HS512, environment.getProperty("token.secret"))
		.compact();
		
		 response.addHeader("token", token);
	     response.addHeader("userId", userDetails.getId().toString());
		
	}

}
