package com.andre.apiheroi.dto.response;

import com.andre.apiheroi.model.HeroiModel;
import com.andre.apiheroi.model.NivelForca;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

import java.util.List;

@Relation(itemRelation = "heroi", collectionRelation = "herois")
@Schema(name = "HeroiResponseDTO", example = HeroiResponseDTO.EXEMPLO)
public record HeroiResponseDTO(
        Long id,
        String nome,
        String nomeHeroico,
        NivelForca nivelForca,
        Long perfilId,
        String perfilNome,
        Long poderId,
        String poderNome,
        Long fraquezaId,
        String fraquezaNome,
        List<EquipeResumo> equipes
) {
    public static final String EXEMPLO = """
            {
              "id": 1,
              "nome": "Lia Moreira",
              "nomeHeroico": "Faísca Azul",
              "nivelForca": "MUTANTE",
              "perfilId": 1,
              "perfilNome": "Líder Alfa",
              "poderId": 1,
              "poderNome": "Rajada Elétrica",
              "fraquezaId": 1,
              "fraquezaNome": "Água Salgada",
              "equipes": [
                {
                  "id": 1,
                  "nome": "Liga do Amanhecer"
                }
              ],
              "_links": {
                "self": {
                  "href": "http://localhost:8080/api/herois/v1/1",
                  "type": "GET"
                },
                "herois": {
                  "href": "http://localhost:8080/api/herois/v1",
                  "type": "GET"
                },
                "buscar-por-nivel": {
                  "href": "http://localhost:8080/api/herois/v1/nivel/MUTANTE",
                  "type": "GET"
                },
                "criar": {
                  "href": "http://localhost:8080/api/herois/v1",
                  "type": "POST"
                },
                "atualizar": {
                  "href": "http://localhost:8080/api/herois/v1/1",
                  "type": "PUT"
                },
                "excluir": {
                  "href": "http://localhost:8080/api/herois/v1/1",
                  "type": "DELETE"
                },
                "perfil": {
                  "href": "http://localhost:8080/api/perfis/v1/1",
                  "type": "GET"
                },
                "poder": {
                  "href": "http://localhost:8080/api/poderes/v1/1",
                  "type": "GET"
                },
                "fraqueza": {
                  "href": "http://localhost:8080/api/fraquezas/v1/1",
                  "type": "GET"
                },
                "equipes": {
                  "href": "http://localhost:8080/api/equipes/v1/heroi/1",
                  "type": "GET"
                },
                "v2": {
                  "href": "http://localhost:8080/api/herois/v2/1",
                  "type": "GET"
                }
              }
            }""";

    public static final String EXEMPLO_PAGINA = """
            {
              "_embedded": {
                "herois": [
                  {
                    "id": 1,
                    "nome": "Lia Moreira",
                    "nomeHeroico": "Faísca Azul",
                    "nivelForca": "MUTANTE",
                    "perfilId": 1,
                    "perfilNome": "Líder Alfa",
                    "poderId": 1,
                    "poderNome": "Rajada Elétrica",
                    "fraquezaId": 1,
                    "fraquezaNome": "Água Salgada",
                    "equipes": [
                      {
                        "id": 1,
                        "nome": "Liga do Amanhecer"
                      }
                    ],
                    "_links": {
                      "self": {
                        "href": "http://localhost:8080/api/herois/v1/1",
                        "type": "GET"
                      },
                      "herois": {
                        "href": "http://localhost:8080/api/herois/v1",
                        "type": "GET"
                      },
                      "buscar-por-nivel": {
                        "href": "http://localhost:8080/api/herois/v1/nivel/MUTANTE",
                        "type": "GET"
                      },
                      "criar": {
                        "href": "http://localhost:8080/api/herois/v1",
                        "type": "POST"
                      },
                      "atualizar": {
                        "href": "http://localhost:8080/api/herois/v1/1",
                        "type": "PUT"
                      },
                      "excluir": {
                        "href": "http://localhost:8080/api/herois/v1/1",
                        "type": "DELETE"
                      },
                      "perfil": {
                        "href": "http://localhost:8080/api/perfis/v1/1",
                        "type": "GET"
                      },
                      "poder": {
                        "href": "http://localhost:8080/api/poderes/v1/1",
                        "type": "GET"
                      },
                      "fraqueza": {
                        "href": "http://localhost:8080/api/fraquezas/v1/1",
                        "type": "GET"
                      },
                      "equipes": {
                        "href": "http://localhost:8080/api/equipes/v1/heroi/1",
                        "type": "GET"
                      },
                      "v2": {
                        "href": "http://localhost:8080/api/herois/v2/1",
                        "type": "GET"
                      }
                    }
                  }
                ]
              },
              "_links": {
                "first": {
                  "href": "http://localhost:8080/api/herois/v1?page=0&size=10"
                },
                "self": {
                  "href": "http://localhost:8080/api/herois/v1?page=0&size=10"
                },
                "last": {
                  "href": "http://localhost:8080/api/herois/v1?page=0&size=10"
                }
              },
              "page": {
                "size": 10,
                "totalElements": 1,
                "totalPages": 1,
                "number": 0
              }
            }""";

    public record EquipeResumo(Long id, String nome) {
    }

    public HeroiResponseDTO(HeroiModel heroi) {
        this(heroi.getId(), heroi.getNome(), heroi.getNomeHeroico(), heroi.getNivelForca(),
                heroi.getPerfil().getId(), heroi.getPerfil().getNome(),
                heroi.getPoder() != null ? heroi.getPoder().getId() : null,
                heroi.getPoder() != null ? heroi.getPoder().getNome() : null,
                heroi.getFraqueza() != null ? heroi.getFraqueza().getId() : null,
                heroi.getFraqueza() != null ? heroi.getFraqueza().getNome() : null,
                heroi.getEquipes().stream().map(equipe -> new EquipeResumo(equipe.getId(), equipe.getNome())).toList());
    }
}
