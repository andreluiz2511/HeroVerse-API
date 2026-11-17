package com.andre.apiheroi.dto.response;

import com.andre.apiheroi.model.HeroiModel;
import com.andre.apiheroi.model.NivelForca;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

import java.util.List;

@Relation(itemRelation = "heroi", collectionRelation = "herois")
@Schema(name = "HeroiV2ResponseDTO", example = HeroiV2ResponseDTO.EXEMPLO)
public record HeroiV2ResponseDTO(
        Long id,
        String nomeHeroico,
        String nome,
        NivelForca nivelForca,
        Integer forca,
        String poder,
        String fraqueza,
        List<String> equipes,
        Long perfilId,
        String perfil
) {
    public static final String EXEMPLO = """
            {
              "id": 1,
              "nomeHeroico": "Faísca Azul",
              "nome": "Lia Moreira",
              "nivelForca": "MUTANTE",
              "forca": 78,
              "poder": "Rajada Elétrica",
              "fraqueza": "Água Salgada",
              "equipes": [
                "Liga do Amanhecer"
              ],
              "perfilId": 1,
              "perfil": "Líder Alfa",
              "_links": {
                "self": {
                  "href": "http://localhost:8080/api/herois/v2/1",
                  "type": "GET"
                },
                "herois": {
                  "href": "http://localhost:8080/api/herois/v2",
                  "type": "GET"
                },
                "buscar-por-nivel": {
                  "href": "http://localhost:8080/api/herois/v1/nivel/MUTANTE",
                  "type": "GET"
                },
                "criar": {
                  "href": "http://localhost:8080/api/herois/v2",
                  "type": "POST"
                },
                "atualizar": {
                  "href": "http://localhost:8080/api/herois/v2/1",
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
                "v1": {
                  "href": "http://localhost:8080/api/herois/v1/1",
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
                    "nomeHeroico": "Faísca Azul",
                    "nome": "Lia Moreira",
                    "nivelForca": "MUTANTE",
                    "forca": 78,
                    "poder": "Rajada Elétrica",
                    "fraqueza": "Água Salgada",
                    "equipes": [
                      "Liga do Amanhecer"
                    ],
                    "perfilId": 1,
                    "perfil": "Líder Alfa",
                    "_links": {
                      "self": {
                        "href": "http://localhost:8080/api/herois/v2/1",
                        "type": "GET"
                      },
                      "herois": {
                        "href": "http://localhost:8080/api/herois/v2",
                        "type": "GET"
                      },
                      "buscar-por-nivel": {
                        "href": "http://localhost:8080/api/herois/v1/nivel/MUTANTE",
                        "type": "GET"
                      },
                      "criar": {
                        "href": "http://localhost:8080/api/herois/v2",
                        "type": "POST"
                      },
                      "atualizar": {
                        "href": "http://localhost:8080/api/herois/v2/1",
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
                      "v1": {
                        "href": "http://localhost:8080/api/herois/v1/1",
                        "type": "GET"
                      }
                    }
                  }
                ]
              },
              "_links": {
                "first": {
                  "href": "http://localhost:8080/api/herois/v2?page=0&size=10"
                },
                "self": {
                  "href": "http://localhost:8080/api/herois/v2?page=0&size=10"
                },
                "last": {
                  "href": "http://localhost:8080/api/herois/v2?page=0&size=10"
                }
              },
              "page": {
                "size": 10,
                "totalElements": 1,
                "totalPages": 1,
                "number": 0
              }
            }""";

    public HeroiV2ResponseDTO(HeroiModel heroi) {
        this(heroi.getId(), heroi.getNomeHeroico(), heroi.getNome(), heroi.getNivelForca(), heroi.getForca(),
                heroi.getPoder() != null ? heroi.getPoder().getNome() : null,
                heroi.getFraqueza() != null ? heroi.getFraqueza().getNome() : null,
                heroi.getEquipes().stream().map(equipe -> equipe.getNome()).sorted().toList(),
                heroi.getPerfil().getId(), heroi.getPerfil().getNome());
    }
}
