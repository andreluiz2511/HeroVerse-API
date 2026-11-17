package com.andre.apiheroi.assembler;

import com.andre.apiheroi.controller.HeroiController;
import com.andre.apiheroi.controller.FraquezaController;
import com.andre.apiheroi.dto.response.FraquezaResponseDTO;
import com.andre.apiheroi.model.FraquezaModel;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class FraquezaAssembler implements RepresentationModelAssembler<FraquezaModel, EntityModel<FraquezaResponseDTO>> {

    @Override
    public EntityModel<FraquezaResponseDTO> toModel(FraquezaModel fraqueza) {
        return EntityModel.of(new FraquezaResponseDTO(fraqueza),
                linkTo(methodOn(FraquezaController.class).findById(fraqueza.getId())).withSelfRel().withType("GET"),
                linkTo(methodOn(FraquezaController.class).findAll(Pageable.unpaged())).withRel("fraquezas").withType("GET"),
                linkTo(methodOn(FraquezaController.class).create(null, null, null)).withRel("criar").withType("POST"),
                linkTo(methodOn(FraquezaController.class).update(fraqueza.getId(), null, null)).withRel("atualizar").withType("PUT"),
                linkTo(methodOn(FraquezaController.class).delete(fraqueza.getId(), null)).withRel("excluir").withType("DELETE"),
                linkTo(methodOn(HeroiController.class).findById(fraqueza.getHeroi().getId())).withRel("heroi").withType("GET"));
    }
}
