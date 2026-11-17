package com.andre.apiheroi.assembler;

import com.andre.apiheroi.controller.HeroiController;
import com.andre.apiheroi.controller.PoderController;
import com.andre.apiheroi.dto.response.PoderResponseDTO;
import com.andre.apiheroi.model.PoderModel;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class PoderAssembler implements RepresentationModelAssembler<PoderModel, EntityModel<PoderResponseDTO>> {

    @Override
    public EntityModel<PoderResponseDTO> toModel(PoderModel poder) {
        return EntityModel.of(new PoderResponseDTO(poder),
                linkTo(methodOn(PoderController.class).findById(poder.getId())).withSelfRel().withType("GET"),
                linkTo(methodOn(PoderController.class).findAll(Pageable.unpaged())).withRel("poderes").withType("GET"),
                linkTo(methodOn(PoderController.class).create(null, null, null)).withRel("criar").withType("POST"),
                linkTo(methodOn(PoderController.class).update(poder.getId(), null, null)).withRel("atualizar").withType("PUT"),
                linkTo(methodOn(PoderController.class).delete(poder.getId(), null)).withRel("excluir").withType("DELETE"),
                linkTo(methodOn(HeroiController.class).findById(poder.getHeroi().getId())).withRel("heroi").withType("GET"));
    }
}
