package br.com.santoandreplacas.apisantoandreplacas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Atalhos para os testes de integração chamarem a API como o front chama:
 * login, depois requisições JSON com o token. Devolve status + corpo já
 * interpretado, para o teste ficar só com o que interessa conferir.
 */
class ApiDeTeste {

    record Resposta(int status, JsonNode corpo, String texto) {
        String mensagem() {
            return corpo != null && corpo.has("mensagem") ? corpo.get("mensagem").asText() : null;
        }

        long id() {
            return corpo.get("id").asLong();
        }
    }

    private final MockMvc mvc;
    private final ObjectMapper json = new ObjectMapper();

    ApiDeTeste(MockMvc mvc) {
        this.mvc = mvc;
    }

    String login(String login, String senha) throws Exception {
        Resposta r = chamar(MockMvcRequestBuilders.post("/api/auth/login"), null,
                "{\"login\":\"" + login + "\",\"senha\":\"" + senha + "\"}");
        if (r.status() != 200) {
            throw new AssertionError("Login de " + login + " falhou: " + r.status() + " " + r.texto());
        }
        return "Bearer " + r.corpo().get("token").asText();
    }

    String loginAdmin() throws Exception {
        return login("admin", "admin123");
    }

    Resposta get(String rota, String token) throws Exception {
        return chamar(MockMvcRequestBuilders.get(rota), token, null);
    }

    Resposta post(String rota, String token, String corpo) throws Exception {
        return chamar(MockMvcRequestBuilders.post(rota), token, corpo);
    }

    Resposta put(String rota, String token, String corpo) throws Exception {
        return chamar(MockMvcRequestBuilders.put(rota), token, corpo);
    }

    Resposta patch(String rota, String token, String corpo) throws Exception {
        return chamar(MockMvcRequestBuilders.patch(rota), token, corpo);
    }

    Resposta delete(String rota, String token) throws Exception {
        return chamar(MockMvcRequestBuilders.delete(rota), token, null);
    }

    private Resposta chamar(MockHttpServletRequestBuilder requisicao, String token, String corpo) throws Exception {
        if (token != null) {
            requisicao.header("Authorization", token);
        }
        if (corpo != null) {
            requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }
        var resposta = mvc.perform(requisicao).andReturn().getResponse();
        String texto = resposta.getContentAsString(StandardCharsets.UTF_8);
        JsonNode no = null;
        try {
            no = texto.isBlank() ? null : json.readTree(texto);
        } catch (Exception e) {
            // corpo que não é JSON: fica só o texto
        }
        return new Resposta(resposta.getStatus(), no, texto);
    }
}
