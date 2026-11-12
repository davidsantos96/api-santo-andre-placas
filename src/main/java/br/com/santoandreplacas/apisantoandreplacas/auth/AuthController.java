package br.com.santoandreplacas.apisantoandreplacas.auth;

import br.com.santoandreplacas.apisantoandreplacas.common.FusoHorario;
import br.com.santoandreplacas.apisantoandreplacas.security.JwtService;
import br.com.santoandreplacas.apisantoandreplacas.usuario.Usuario;
import br.com.santoandreplacas.apisantoandreplacas.usuario.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

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
        if (request.login() == null || request.senha() == null) {
            throw new IllegalArgumentException("Informe usuário e senha.");
        }

        Usuario usuario = usuarioRepository.buscarPorIdentificador(request.login())
                .orElseThrow(() -> new IllegalArgumentException("Usuário ou senha inválidos"));

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new IllegalArgumentException("Usuário ou senha inválidos");
        }

        if (!usuario.isAtivo()) {
            throw new IllegalArgumentException("Usuário inativo. Contate um administrador.");
        }

        usuario.setUltimoAcessoEm(LocalDateTime.now(FusoHorario.SAO_PAULO));
        usuarioRepository.save(usuario);

        String token = jwtService.gerarToken(usuario.getLogin(),usuario.getPapel().name());

        return new LoginResponse(token, usuario.getPapel().name(), usuario.getNome(), usuario.getLogin());
    }
}