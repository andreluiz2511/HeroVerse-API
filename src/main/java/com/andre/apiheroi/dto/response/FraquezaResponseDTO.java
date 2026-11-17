package com.andre.apiheroi.dto.response;

import com.andre.apiheroi.model.FraquezaModel;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

@Relation(itemRelation = "fraqueza", collectionRelation = "fraquezas")
@Schema(name = "FraquezaResponseDTO", example = FraquezaResponseDTO.EXEMPLO)
public record FraquezaResponseDTO(
        Long id,
        String nome,
        String descricao,
        Long heroiId,
        String heroiNomeHeroico
) {
    public static final String EXEMPLO = """
            {
              "id": 1,
              "nome": "Água Salgada",
              "descricao": "Perde os poderes quando molhada com água do mar.",
              "heroiId": 1,
              "heroiNomeHeroico": "Faísca Azul",
              "_links": {
                "self": {
                  "href": "http://localhost:8080/api/fraquezas/v1/1",
                  "type": "GET"
                },
                "fraquezas": {
                  "href": "http://localhost:8080/api/fraquezas/v1",
                  "type": "GET"
                },
                "criar": {
                  "href": "http://localhost:8080/api/fraquezas/v1",
                  "type": "POST"
                },
                "atualizar": {
                  "href": "http://localhost:8080/api/fraquezas/v1/1",
                  "type": "PUT"
                },
                "excluir": {
                  "href": "http://localhost:8080/api/fraquezas/v1/1",
                  "type": "DELETE"
                },
                "heroi": {
                  "href": "http://localhost:8080/api/herois/v1/1",
                  "type": "GET"
                }
              }
            }""";

    public static final String EXEMPLO_PAGINA = """
            {
              "_embedded": {
                "fraquezas": [
                  {
                    "id": 1,
                    "nome": "Água Salgada",
                    "descricao": "Perde os poderes quando molhada com água do mar.",
                    "heroiId": 1,
                    "heroiNomeHeroico": "Faísca Azul",
                    "_links": {
                      "self": {
                        "href": "http://localhost:8080/api/fraquezas/v1/1",
                        "type": "GET"
                      },
                      "fraquezas": {
                        "href": "http://localhost:8080/api/fraquezas/v1",
                        "type": "GET"
                      },
                      "criar": {
                        "href": "http://localhost:8080/api/fraquezas/v1",
                        "type": "POST"
                      },
                      "atualizar": {
                        "href": "http://localhost:8080/api/fraquezas/v1/1",
                        "type": "PUT"
                      },
                      "excluir": {
                        "href": "http://localhost:8080/api/fraquezas/v1/1",
                        "type": "DELETE"
                      },
                      "heroi": {
                        "href": "http://localhost:8080/api/herois/v1/1",
                        "type": "GET"
                      }
                    }
                  }
                ]
              },
              "_links": {
                "first": {
                  "href": "http://localhost:8080/api/fraquezas/v1?page=0&size=10"
                },
                "self": {
                  "href": "http://localhost:8080/api/fraquezas/v1?page=0&size=10"
                },
                "last": {
                  "href": "http://localhost:8080/api/fraquezas/v1?page=0&size=10"
                }
              },
              "page": {
                "size": 10,
                "totalElements": 1,
                "totalPages": 1,
                "number": 0
              }
            }""";

    public FraquezaResponseDTO(FraquezaModel fraqueza) {
        this(fraqueza.getId(), fraqueza.getNome(), fraqueza.getDescricao(), fraqueza.getHeroi().getId(), fraqueza.getHeroi().getNomeHeroico());
    }
}
