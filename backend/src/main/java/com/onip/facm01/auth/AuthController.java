package com.onip.facm01.auth;

import com.onip.facm01.agent.Agent;
import com.onip.facm01.agent.AgentRepository;
import com.onip.facm01.agent.dto.AgentDto;
import com.onip.facm01.auth.dto.LoginRequest;
import com.onip.facm01.auth.dto.LoginResponse;
import com.onip.facm01.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AgentRepository agentRepository;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, AgentRepository agentRepository, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.agentRepository = agentRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Agent agent = agentRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalStateException("Agent introuvable après authentification"));

        String token = jwtService.generateToken(agent.getId(), agent.getUsername(), agent.getRole().name());
        return ResponseEntity.ok(new LoginResponse(token, jwtService.getExpirationMinutes(), AgentDto.from(agent)));
    }
}
