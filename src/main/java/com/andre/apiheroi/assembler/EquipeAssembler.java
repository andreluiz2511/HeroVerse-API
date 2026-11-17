package com.andre.apiheroi.assembler;

import com.andre.apiheroi.controller.EquipeController;
import com.andre.apiheroi.controller.EquipeV2Controller;
import com.andre.apiheroi.controller.HeroiController;
import com.andre.apiheroi.controller.PerfilController;
import com.andre.apiheroi.dto.response.EquipeResponseDTO;
import com.andre.apiheroi.dto.response.EquipeV2ResponseDTO;
import com.andre.apiheroi.model.EquipeModel;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class EquipeAssembler implements RepresentationModelAssembler<EquipeModel, EntityModel<EquipeResponseDTO>> {

    @Override
    public EntityModel<EquipeResponseDTO> toModel(EquipeModel equipe) {
        return EntityModel.of(new EquipeResponseDTO(equipe),
                linkTo(methodOn(EquipeController.class).findById(equipe.getId())).withSelfRel().withType("GET"),
                linkTo(methodOn(EquipeController.class).findAll(Pageable.unpaged())).withRel("equipes").withType("GET"),
                linkTo(methodOn(EquipeController.class).create(null, null, null)).withRel("criar").withType("POST"),
                linkTo(methodOn(EquipeController.class).update(equipe.getId(), null, null)).withRel("atualizar").withType("PUT"),
                linkTo(methodOn(EquipeController.class).delete(equipe.getId(), null)).withRel("excluir").withType("DELETE"),
                linkTo(methodOn(PerfilController.class).findById(equipe.getPerfil().getId())).withRel("perfil").withType("GET"),
                linkTo(methodOn(HeroiController.class).findByEquipe(equipe.getId(), Pageable.unpaged())).withRel("herois").withType("GET"),
                linkTo(methodOn(EquipeV2Controller.class).findById(equipe.getId())).withRel("v2").withType("GET"));
    }

    public EntityModel<EquipeV2ResponseDTO> toV2Model(EquipeModel equipe) {
        return EntityModel.of(new EquipeV2ResponseDTO(equipe),
                linkTo(methodOn(EquipeV2Controller.class).findById(equipe.getId())).withSelfRel().withType("GET"),
                linkTo(methodOn(EquipeV2Controller.class).findAll(Pageable.unpaged())).withRel("equipes").withType("GET"),
                linkTo(methodOn(EquipeController.class).create(null, null, null)).withRel("criar").withType("POST"),
                linkTo(methodOn(EquipeController.class).update(equipe.getId(), null, null)).withRel("atualizar").withType("PUT"),
                linkTo(methodOn(EquipeController.class).delete(equipe.getId(), null)).withRel("excluir").withType("DELETE"),
                linkTo(methodOn(PerfilController.class).findById(equipe.getPerfil().getId())).withRel("perfil").withType("GET"),
                linkTo(methodOn(HeroiController.class).findByEquipe(equipe.getId(), Pageable.unpaged())).withRel("herois").withType("GET"),
                linkTo(methodOn(EquipeController.class).findById(equipe.getId())).withRel("v1").withType("GET"));
    }
}
