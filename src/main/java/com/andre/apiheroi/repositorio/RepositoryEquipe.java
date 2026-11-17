package com.andre.apiheroi.repositorio;

import com.andre.apiheroi.model.EquipeModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositoryEquipe extends RepositoryBase<EquipeModel> {

    @Query("SELECT e FROM EquipeModel e WHERE e.perfil.id = :perfilId")
    Page<EquipeModel> buscarPorPerfil(@Param("perfilId") Long perfilId, Pageable pageable);

    @Query("SELECT e FROM EquipeModel e JOIN e.herois h WHERE h.id = :heroiId")
    Page<EquipeModel> buscarPorHeroi(@Param("heroiId") Long heroiId, Pageable pageable);
}
