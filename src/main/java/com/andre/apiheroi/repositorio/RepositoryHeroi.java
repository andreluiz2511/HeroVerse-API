package com.andre.apiheroi.repositorio;

import com.andre.apiheroi.model.HeroiModel;
import com.andre.apiheroi.model.NivelForca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositoryHeroi extends RepositoryBase<HeroiModel> {

    @Query("SELECT h FROM HeroiModel h WHERE LOWER(h.nome) LIKE LOWER(CONCAT('%', :texto, '%')) " +
            "OR LOWER(h.nomeHeroico) LIKE LOWER(CONCAT('%', :texto, '%'))")
    Page<HeroiModel> buscarPorNomeOuNomeHeroico(@Param("texto") String texto, Pageable pageable);

    Page<HeroiModel> findByNivelForca(NivelForca nivelForca, Pageable pageable);

    @Query("SELECT h FROM HeroiModel h WHERE h.perfil.id = :perfilId")
    Page<HeroiModel> buscarPorPerfil(@Param("perfilId") Long perfilId, Pageable pageable);

    @Query("SELECT h FROM HeroiModel h JOIN h.equipes e WHERE e.id = :equipeId")
    Page<HeroiModel> buscarPorEquipe(@Param("equipeId") Long equipeId, Pageable pageable);
}
