package com.ethicssra.service;

import com.ethicssra.domain.Role;
import com.ethicssra.domain.User;
import com.ethicssra.dto.AuthResponse;
import com.ethicssra.dto.LoginRequest;
import com.ethicssra.dto.RegisterRequest;
import com.ethicssra.repository.UserRepository;
import com.ethicssra.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User u = new User();
        u.setUsername(req.username());
        u.setPassword(passwordEncoder.encode(req.password()));
        u.setRole(Role.STUDENT);
        u.setDisplayName(req.displayName() != null ? req.displayName() : req.username());
        u = userRepository.save(u);
        auditService.log(u.getId(), "REGISTER", "User", u.getId(), null);
        String token = jwtService.generateToken(u.getId(), u.getUsername(), u.getRole());
        return new AuthResponse(token, u.getId(), u.getUsername(), u.getRole(), u.getDisplayName());
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password())
        );
        User u = userRepository.findByUsername(req.username()).orElseThrow();
        String token = jwtService.generateToken(u.getId(), u.getUsername(), u.getRole());
        auditService.log(u.getId(), "LOGIN", "User", u.getId(), null);
        return new AuthResponse(token, u.getId(), u.getUsername(), u.getRole(), u.getDisplayName());
    }
}
