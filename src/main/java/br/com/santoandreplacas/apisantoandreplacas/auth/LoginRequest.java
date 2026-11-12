package br.com.santoandreplacas.apisantoandreplacas.auth;

import com.fasterxml.jackson.annotation.JsonAlias;

// "login" aceita o login da pessoa ou o e-mail dela (se tiver @). O alias "email"
// mantém funcionando quem ainda manda o corpo no formato antigo.
public record LoginRequest(@JsonAlias("email") String login, String senha) {}
