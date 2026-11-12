package br.com.santoandreplacas.apisantoandreplacas.usuario;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    // Identidade de quem entra no sistema (login de rede, ex.: "bruna.silva"). Sempre em
    // minúsculas. A coluna aceita nulo só para o banco já existente receber o campo antes
    // do preenchimento (UsuarioLoginMigracao); o serviço nunca cria usuário sem login.
    @Column(unique = true)
    private String login;

    // Opcional: só os donos (ADMIN) precisam ter um e-mail real. Nulo é permitido, e a
    // restrição única do banco aceita vários nulos.
    @Column(unique = true)
    private String email;

    // Nunca serializar o hash: se alguma rota devolver a entidade (direto ou por
    // um relacionamento, como criadoPorUsuario em Cliente), o hash iria no corpo.
    // Já aconteceu — ver B19 no CONTEXT.md. Não afeta a leitura em Java.
    @JsonIgnore
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    private Papel papel;

    private boolean ativo;

    private LocalDateTime ultimoAcessoEm;

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login == null ? null : login.trim().toLowerCase();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public Papel getPapel() {
        return papel;
    }

    public void setPapel(Papel papel) {
        this.papel = papel;
    }

    public LocalDateTime getUltimoAcessoEm() {
        return ultimoAcessoEm;
    }

    public void setUltimoAcessoEm(LocalDateTime ultimoAcessoEm) {
        this.ultimoAcessoEm = ultimoAcessoEm;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}