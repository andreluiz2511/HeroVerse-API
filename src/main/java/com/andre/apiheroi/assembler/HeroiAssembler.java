package com.andre.apiheroi.assembler;

import com.andre.apiheroi.controller.EquipeController;
import com.andre.apiheroi.controller.FraquezaController;
import com.andre.apiheroi.controller.HeroiController;
import com.andre.apiheroi.controller.HeroiV2Controller;
import com.andre.apiheroi.controller.PerfilController;
import com.andre.apiheroi.controller.PoderController;
import com.andre.apiheroi.dto.response.HeroiResponseDTO;
import com.andre.apiheroi.dto.response.HeroiV2ResponseDTO;
import com.andre.apiheroi.model.HeroiModel;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class HeroiAssembler implements RepresentationModelAssembler<HeroiModel, EntityModel<HeroiResponseDTO>> {

    @Override
    public EntityModel<HeroiResponseDTO> toModel(HeroiModel heroi) {
        EntityModel<HeroiResponseDTO> modelo = EntityModel.of(new HeroiResponseDTO(heroi),
                linkTo(methodOn(HeroiController.class).findById(heroi.getId())).withSelfRel().withType("GET"),
                linkTo(methodOn(HeroiController.class).findAll(Pageable.unpaged())).withRel("herois").withType("GET"),
                linkTo(methodOn(HeroiController.class).findByNivelForca(heroi.getNivelForca(), Pageable.unpaged())).withRel("buscar-por-nivel").withType("GET"),
                linkTo(methodOn(HeroiController.class).create(null, null, null)).withRel("criar").withType("POST"),
                linkTo(methodOn(HeroiController.class).update(heroi.getId(), null, null)).withRel("atualizar").withType("PUT"),
                linkTo(methodOn(HeroiController.class).delete(heroi.getId(), null)).withRel("excluir").withType("DELETE"));
        relacionados(modelo, heroi);
        modelo.add(linkTo(methodOn(HeroiV2Controller.class).findById(heroi.getId())).withRel("v2").withType("GET"));
        return modelo;
    }

    public EntityModel<HeroiV2ResponseDTO> toV2Model(HeroiModel heroi) {
        EntityModel<HeroiV2ResponseDTO> modelo = EntityModel.of(new HeroiV2ResponseDTO(heroi),
                linkTo(methodOn(HeroiV2Controller.class).findById(heroi.getId())).withSelfRel().withType("GET"),
                linkTo(methodOn(HeroiV2Controller.class).findAll(Pageable.unpaged())).withRel("herois").withType("GET"),
                linkTo(methodOn(HeroiController.class).findByNivelForca(heroi.getNivelForca(), Pageable.unpaged())).withRel("buscar-por-nivel").withType("GET"),
                linkTo(methodOn(HeroiV2Controller.class).create(null, null, null)).withRel("criar").withType("POST"),
                linkTo(methodOn(HeroiV2Controller.class).update(heroi.getId(), null, null)).withRel("atualizar").withType("PUT"),
                linkTo(methodOn(HeroiController.class).delete(heroi.getId(), null)).withRel("excluir").withType("DELETE"));
        relacionados(modelo, heroi);
        modelo.add(linkTo(methodOn(HeroiController.class).findById(heroi.getId())).withRel("v1").withType("GET"));
        return modelo;
    }

    private void relacionados(EntityModel<?> modelo, HeroiModel heroi) {
        modelo.add(linkTo(methodOn(PerfilController.class).findById(heroi.getPerfil().getId())).withRel("perfil").withType("GET"));
        if (heroi.getPoder() != null) {
            modelo.add(linkTo(methodOn(PoderController.class).findById(heroi.getPoder().getId())).withRel("poder").withType("GET"));
        }
        if (heroi.getFraqueza() != null) {
            modelo.add(linkTo(methodOn(FraquezaController.class).findById(heroi.getFraqueza().getId())).withRel("fraqueza").withType("GET"));
        }
        modelo.add(linkTo(methodOn(EquipeController.class).findByHeroi(heroi.getId(), Pageable.unpaged())).withRel("equipes").withType("GET"));
    }
}
