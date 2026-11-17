package com.andre.apiheroi.dto.response;

import com.andre.apiheroi.model.PoderModel;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

@Relation(itemRelation = "poder", collectionRelation = "poderes")
@Schema(name = "PoderResponseDTO", example = PoderResponseDTO.EXEMPLO)
public record PoderResponseDTO(
        Long id,
        String nome,
        String descricao,
        Long heroiId,
        String heroiNomeHeroico
) {
    public static final String EXEMPLO = """
            {
              "id": 1,
              "nome": "Rajada Elétrica",
              "descricao": "Dispara descargas elétricas pelas mãos.",
              "heroiId": 1,
              "heroiNomeHeroico": "Faísca Azul",
              "_links": {
                "self": {
                  "href": "http://localhost:8080/api/poderes/v1/1",
                  "type": "GET"
                },
                "poderes": {
                  "href": "http://localhost:8080/api/poderes/v1",
                  "type": "GET"
                },
                "criar": {
                  "href": "http://localhost:8080/api/poderes/v1",
                  "type": "POST"
                },
                "atualizar": {
                  "href": "http://localhost:8080/api/poderes/v1/1",
                  "type": "PUT"
                },
                "excluir": {
                  "href": "http://localhost:8080/api/poderes/v1/1",
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
                "poderes": [
                  {
                    "id": 1,
                    "nome": "Rajada Elétrica",
                    "descricao": "Dispara descargas elétricas pelas mãos.",
                    "heroiId": 1,
                    "heroiNomeHeroico": "Faísca Azul",
                    "_links": {
                      "self": {
                        "href": "http://localhost:8080/api/poderes/v1/1",
                        "type": "GET"
                      },
                      "poderes": {
                        "href": "http://localhost:8080/api/poderes/v1",
                        "type": "GET"
                      },
                      "criar": {
                        "href": "http://localhost:8080/api/poderes/v1",
                        "type": "POST"
                      },
                      "atualizar": {
                        "href": "http://localhost:8080/api/poderes/v1/1",
                        "type": "PUT"
                      },
                      "excluir": {
                        "href": "http://localhost:8080/api/poderes/v1/1",
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
                  "href": "http://localhost:8080/api/poderes/v1?page=0&size=10"
                },
                "self": {
                  "href": "http://localhost:8080/api/poderes/v1?page=0&size=10"
                },
                "last": {
                  "href": "http://localhost:8080/api/poderes/v1?page=0&size=10"
                }
              },
              "page": {
                "size": 10,
                "totalElements": 1,
                "totalPages": 1,
                "number": 0
              }
            }""";

    public PoderResponseDTO(PoderModel poder) {
        this(poder.getId(), poder.getNome(), poder.getDescricao(), poder.getHeroi().getId(), poder.getHeroi().getNomeHeroico());
    }
}
