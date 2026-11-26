package br.edu.unifsa.sinasc.usuario;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unifsa.sinasc.usuario.AuthDtos.LoginRequest;
import br.edu.unifsa.sinasc.usuario.AuthDtos.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Login por e-mail e senha (JWT). Contas são criadas por um gestor/auditor")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Login", description = "Devolve um token JWT para usar em Authorization: Bearer <token>")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest dados) {
        return authService.login(dados.email(), dados.senha());
    }
}
