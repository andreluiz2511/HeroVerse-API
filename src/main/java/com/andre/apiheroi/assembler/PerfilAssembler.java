package com.andre.apiheroi.assembler;

import com.andre.apiheroi.controller.AuthController;
import com.andre.apiheroi.controller.EquipeController;
import com.andre.apiheroi.controller.HeroiController;
import com.andre.apiheroi.controller.PerfilController;
import com.andre.apiheroi.dto.response.PerfilResponseDTO;
import com.andre.apiheroi.model.PerfilModel;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class PerfilAssembler implements RepresentationModelAssembler<PerfilModel, EntityModel<PerfilResponseDTO>> {

    @Override
    public EntityModel<PerfilResponseDTO> toModel(PerfilModel perfil) {
        return EntityModel.of(new PerfilResponseDTO(perfil),
                linkTo(methodOn(PerfilController.class).findById(perfil.getId())).withSelfRel().withType("GET"),
                linkTo(methodOn(PerfilController.class).findAll(Pageable.unpaged())).withRel("perfis").withType("GET"),
                linkTo(methodOn(PerfilController.class).findByEmail(perfil.getEmail(), Pageable.unpaged())).withRel("buscar-por-email").withType("GET"),
                linkTo(methodOn(PerfilController.class).create(null, null)).withRel("criar").withType("POST"),
                linkTo(methodOn(PerfilController.class).update(perfil.getId(), null, null)).withRel("atualizar").withType("PUT"),
                linkTo(methodOn(PerfilController.class).delete(perfil.getId(), null)).withRel("excluir").withType("DELETE"),
                linkTo(methodOn(HeroiController.class).findByPerfil(perfil.getId(), Pageable.unpaged())).withRel("herois").withType("GET"),
                linkTo(methodOn(EquipeController.class).findByPerfil(perfil.getId(), Pageable.unpaged())).withRel("equipes").withType("GET"),
                linkTo(methodOn(AuthController.class).login(null)).withRel("login").withType("POST"));
    }
}
