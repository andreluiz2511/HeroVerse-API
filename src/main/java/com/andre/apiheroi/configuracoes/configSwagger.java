package com.andre.apiheroi.configuracoes;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class configSwagger {

    @Bean
    public OpenAPI customOpenAPI(@Value("${springdoc.version:2.0}") String appVersion) {
        return new OpenAPI()
                .info(new Info()
                        .title("HeroVerse — Universo de Heróis")
                        .version(appVersion)
                        .description("Central de gerenciamento de perfis, heróis, poderes, fraquezas e equipes. " +
                                "O guia de uso completo, com exemplos e a lista de erros, está em /ajuda.html.")
                        .termsOfService("/termos.html")
                        .license(new License().name("MIT").url("https://mit-license.org/")))
                .externalDocs(new ExternalDocumentation()
                        .description("Guia completo de uso da API")
                        .url("/ajuda.html"))
                .tags(List.of(
                        new Tag().name("Versionamento por header").description("GET /api/herois e /api/equipes com X-API-Version: 1 ou 2."),
                        new Tag().name("Autenticação").description("Login e logout do perfil. O login gera a chave X-API-Key, exigida nas operações protegidas."),
                        new Tag().name("v1 - Perfis").description("Perfis, que são os usuários da API. Cada perfil só altera e exclui o próprio cadastro."),
                        new Tag().name("v1 - Heróis").description("Heróis. Cada herói pertence a um perfil (N:1) e só o dono altera."),
                        new Tag().name("v1 - Poderes").description("Poderes. Cada herói tem no máximo um poder (1:1)."),
                        new Tag().name("v1 - Fraquezas").description("Fraquezas. Cada herói tem no máximo uma fraqueza (1:1)."),
                        new Tag().name("v1 - Equipes").description("Equipes de um perfil, com vários heróis (N:N)."),
                        new Tag().name("v2 - Heróis").description("Heróis resumidos e com o campo forca (0 a 100). Consulta, cadastro e alteração."),
                        new Tag().name("v2 - Equipes").description("Equipes com quantidadeHerois e forcaMedia. Só consulta.")
                ));
    }
}
