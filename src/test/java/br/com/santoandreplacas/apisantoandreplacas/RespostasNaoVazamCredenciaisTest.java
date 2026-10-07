package br.com.santoandreplacas.apisantoandreplacas;

import br.com.santoandreplacas.apisantoandreplacas.usuario.Papel;
import br.com.santoandreplacas.apisantoandreplacas.usuario.Usuario;
import br.com.santoandreplacas.apisantoandreplacas.usuario.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Guarda de regressão do B19: POST/PUT /clientes devolviam a entidade crua e,
 * com o relacionamento de autoria (criadoPorUsuario), o corpo levava o
 * senhaHash em bcrypt — inclusive o de OUTRO usuário (um ATENDENTE editando
 * cliente criado pelo ADMIN recebia o hash do ADMIN).
 *
 * Permanente de propósito: é vazamento de credencial, não contrato de feature.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RespostasNaoVazamCredenciaisTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PasswordEncoder passwordEncoder;
    ObjectMapper json = new ObjectMapper();

    private String login(String email, String senha) throws Exception {
        return "Bearer " + json.readTree(mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andReturn().getResponse().getContentAsString()).get("token").asText();
    }

    private void exigirSemCredencial(String rota, String corpo) {
        if (corpo.contains("senhaHash") || corpo.contains("$2a$") || corpo.contains("$2b$")) {
            throw new AssertionError("Resposta de " + rota + " expõe credencial: " + corpo);
        }
    }

    /** Entidade crua traz o relacionamento; o DTO traz o id achatado (B20). */
    private void exigirFormaDeDto(String rota, String corpo) throws Exception {
        var no = json.readTree(corpo);
        if (no.has("criadoPorUsuario") || no.has("atualizadoPorUsuario")) {
            throw new AssertionError(rota + " está devolvendo a entidade crua: " + corpo);
        }
        if (!no.has("criadoPorId") || no.get("criadoPorId").isNull()) {
            throw new AssertionError(rota + " deveria trazer criadoPorId: " + corpo);
        }
    }

    @Test
    void escrita_de_cliente_nao_expoe_hash_de_senha_e_traz_os_ids_de_autoria() throws Exception {
        String admin = login("admin@santoandreplacas.com.br", "admin123");

        Usuario u = new Usuario();
        u.setNome("Atendente Seguranca");
        u.setEmail("seguranca@teste.com");
        u.setSenhaHash(passwordEncoder.encode("senha123"));
        u.setPapel(Papel.ATENDENTE);
        u.setAtivo(true);
        usuarioRepository.save(u);
        String atendente = login("seguranca@teste.com", "senha123");

        // cliente criado pelo ADMIN: o autor na resposta do PUT é outra pessoa,
        // que era justamente o caso grave do B19
        long id = json.readTree(mvc.perform(post("/api/clientes").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Cliente Seg\"}"))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        // A checagem de credencial vem antes das de formato, para a mensagem de
        // falha apontar o vazamento e não um campo ausente.
        String post = mvc.perform(post("/api/clientes").header("Authorization", atendente)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Outro Cliente\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        exigirSemCredencial("POST /clientes", post);
        exigirFormaDeDto("POST /clientes", post);

        String put = mvc.perform(put("/api/clientes/" + id).header("Authorization", atendente)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Cliente Seg Editado\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        exigirSemCredencial("PUT /clientes/{id}", put);
        exigirFormaDeDto("PUT /clientes/{id}", put);

        var corpoPut = json.readTree(put);
        if (!"Administrador".equals(corpoPut.get("criadoPor").asText())
                || corpoPut.get("atualizadoPorId").isNull()) {
            throw new AssertionError("PUT deveria manter o autor da criação e registrar quem alterou: " + put);
        }

        // rotas que já estavam corretas, para o caso de alguém trocar o DTO por entidade
        for (String rota : new String[]{"/api/clientes", "/api/clientes/" + id, "/api/veiculos", "/api/usuarios"}) {
            exigirSemCredencial("GET " + rota, mvc.perform(get(rota).header("Authorization", admin))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        }
    }
}
