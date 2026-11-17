package com.andre.apiheroi.dto.response;

import com.andre.apiheroi.model.EquipeModel;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

import java.util.List;

@Relation(itemRelation = "equipe", collectionRelation = "equipes")
@Schema(name = "EquipeResponseDTO", example = EquipeResponseDTO.EXEMPLO)
public record EquipeResponseDTO(
        Long id,
        String nome,
        Long perfilId,
        String perfilNome,
        List<HeroiResumo> herois
) {
    public static final String EXEMPLO = """
            {
              "id": 1,
              "nome": "Liga do Amanhecer",
              "perfilId": 1,
              "perfilNome": "Líder Alfa",
              "herois": [
                {
                  "id": 1,
                  "nomeHeroico": "Faísca Azul"
                }
              ],
              "_links": {
                "self": {
                  "href": "http://localhost:8080/api/equipes/v1/1",
                  "type": "GET"
                },
                "equipes": {
                  "href": "http://localhost:8080/api/equipes/v1",
                  "type": "GET"
                },
                "criar": {
                  "href": "http://localhost:8080/api/equipes/v1",
                  "type": "POST"
                },
                "atualizar": {
                  "href": "http://localhost:8080/api/equipes/v1/1",
                  "type": "PUT"
                },
                "excluir": {
                  "href": "http://localhost:8080/api/equipes/v1/1",
                  "type": "DELETE"
                },
                "perfil": {
                  "href": "http://localhost:8080/api/perfis/v1/1",
                  "type": "GET"
                },
                "herois": {
                  "href": "http://localhost:8080/api/herois/v1/equipe/1",
                  "type": "GET"
                },
                "v2": {
                  "href": "http://localhost:8080/api/equipes/v2/1",
                  "type": "GET"
                }
              }
            }""";

    public static final String EXEMPLO_PAGINA = """
            {
              "_embedded": {
                "equipes": [
                  {
                    "id": 1,
                    "nome": "Liga do Amanhecer",
                    "perfilId": 1,
                    "perfilNome": "Líder Alfa",
                    "herois": [
                      {
                        "id": 1,
                        "nomeHeroico": "Faísca Azul"
                      }
                    ],
                    "_links": {
                      "self": {
                        "href": "http://localhost:8080/api/equipes/v1/1",
                        "type": "GET"
                      },
                      "equipes": {
                        "href": "http://localhost:8080/api/equipes/v1",
                        "type": "GET"
                      },
                      "criar": {
                        "href": "http://localhost:8080/api/equipes/v1",
                        "type": "POST"
                      },
                      "atualizar": {
                        "href": "http://localhost:8080/api/equipes/v1/1",
                        "type": "PUT"
                      },
                      "excluir": {
                        "href": "http://localhost:8080/api/equipes/v1/1",
                        "type": "DELETE"
                      },
                      "perfil": {
                        "href": "http://localhost:8080/api/perfis/v1/1",
                        "type": "GET"
                      },
                      "herois": {
                        "href": "http://localhost:8080/api/herois/v1/equipe/1",
                        "type": "GET"
                      },
                      "v2": {
                        "href": "http://localhost:8080/api/equipes/v2/1",
                        "type": "GET"
                      }
                    }
                  }
                ]
              },
              "_links": {
                "first": {
                  "href": "http://localhost:8080/api/equipes/v1?page=0&size=10"
                },
                "self": {
                  "href": "http://localhost:8080/api/equipes/v1?page=0&size=10"
                },
                "last": {
                  "href": "http://localhost:8080/api/equipes/v1?page=0&size=10"
                }
              },
              "page": {
                "size": 10,
                "totalElements": 1,
                "totalPages": 1,
                "number": 0
              }
            }""";

    public record HeroiResumo(Long id, String nomeHeroico) {
    }

    public EquipeResponseDTO(EquipeModel equipe) {
        this(equipe.getId(), equipe.getNome(), equipe.getPerfil().getId(), equipe.getPerfil().getNome(),
                equipe.getHerois().stream().map(heroi -> new HeroiResumo(heroi.getId(), heroi.getNomeHeroico())).toList());
    }
}
