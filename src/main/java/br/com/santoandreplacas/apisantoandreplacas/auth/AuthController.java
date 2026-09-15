package br.com.santoandreplacas.apisantoandreplacas.auth;

import br.com.santoandreplacas.apisantoandreplacas.security.JwtService;
import br.com.santoandreplacas.apisantoandreplacas.usuario.Usuario;
import br.com.santoandreplacas.apisantoandreplacas.usuario.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Email ou senha inválidos"));

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new IllegalArgumentException("Email ou senha inválidos");
        }

        String token = jwtService.gerarToken(usuario.getEmail(), usuario.getPapel().name());

        return new LoginResponse(token, usuario.getPapel().name());
    }
}