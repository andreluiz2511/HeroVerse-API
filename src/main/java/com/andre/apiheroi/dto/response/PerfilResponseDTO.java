package com.andre.apiheroi.dto.response;

import com.andre.apiheroi.model.PerfilModel;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

@Relation(itemRelation = "perfil", collectionRelation = "perfis")
@Schema(name = "PerfilResponseDTO", example = PerfilResponseDTO.EXEMPLO)
public record PerfilResponseDTO(
        Long id,
        String nome,
        String email,
        int quantidadeHerois,
        int quantidadeEquipes
) {
    public static final String EXEMPLO = """
            {
              "id": 1,
              "nome": "Líder Alfa",
              "email": "lider.alfa@exemplo.com",
              "quantidadeHerois": 3,
              "quantidadeEquipes": 1,
              "_links": {
                "self": {
                  "href": "http://localhost:8080/api/perfis/v1/1",
                  "type": "GET"
                },
                "perfis": {
                  "href": "http://localhost:8080/api/perfis/v1",
                  "type": "GET"
                },
                "buscar-por-email": {
                  "href": "http://localhost:8080/api/perfis/v1/busca?email=lider.alfa%40exemplo.com",
                  "type": "GET"
                },
                "criar": {
                  "href": "http://localhost:8080/api/perfis/v1",
                  "type": "POST"
                },
                "atualizar": {
                  "href": "http://localhost:8080/api/perfis/v1/1",
                  "type": "PUT"
                },
                "excluir": {
                  "href": "http://localhost:8080/api/perfis/v1/1",
                  "type": "DELETE"
                },
                "herois": {
                  "href": "http://localhost:8080/api/herois/v1/perfil/1",
                  "type": "GET"
                },
                "equipes": {
                  "href": "http://localhost:8080/api/equipes/v1/perfil/1",
                  "type": "GET"
                },
                "login": {
                  "href": "http://localhost:8080/api/auth/login",
                  "type": "POST"
                }
              }
            }""";

    public static final String EXEMPLO_PAGINA = """
            {
              "_embedded": {
                "perfis": [
                  {
                    "id": 1,
                    "nome": "Líder Alfa",
                    "email": "lider.alfa@exemplo.com",
                    "quantidadeHerois": 3,
                    "quantidadeEquipes": 1,
                    "_links": {
                      "self": {
                        "href": "http://localhost:8080/api/perfis/v1/1",
                        "type": "GET"
                      },
                      "perfis": {
                        "href": "http://localhost:8080/api/perfis/v1",
                        "type": "GET"
                      },
                      "buscar-por-email": {
                        "href": "http://localhost:8080/api/perfis/v1/busca?email=lider.alfa%40exemplo.com",
                        "type": "GET"
                      },
                      "criar": {
                        "href": "http://localhost:8080/api/perfis/v1",
                        "type": "POST"
                      },
                      "atualizar": {
                        "href": "http://localhost:8080/api/perfis/v1/1",
                        "type": "PUT"
                      },
                      "excluir": {
                        "href": "http://localhost:8080/api/perfis/v1/1",
                        "type": "DELETE"
                      },
                      "herois": {
                        "href": "http://localhost:8080/api/herois/v1/perfil/1",
                        "type": "GET"
                      },
                      "equipes": {
                        "href": "http://localhost:8080/api/equipes/v1/perfil/1",
                        "type": "GET"
                      },
                      "login": {
                        "href": "http://localhost:8080/api/auth/login",
                        "type": "POST"
                      }
                    }
                  }
                ]
              },
              "_links": {
                "first": {
                  "href": "http://localhost:8080/api/perfis/v1?page=0&size=10"
                },
                "self": {
                  "href": "http://localhost:8080/api/perfis/v1?page=0&size=10"
                },
                "last": {
                  "href": "http://localhost:8080/api/perfis/v1?page=0&size=10"
                }
              },
              "page": {
                "size": 10,
                "totalElements": 1,
                "totalPages": 1,
                "number": 0
              }
            }""";

    public PerfilResponseDTO(PerfilModel perfil) {
        this(perfil.getId(), perfil.getNome(), perfil.getEmail(), perfil.getHerois().size(), perfil.getEquipes().size());
    }
}
