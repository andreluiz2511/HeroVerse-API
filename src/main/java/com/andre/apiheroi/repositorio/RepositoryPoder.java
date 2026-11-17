package com.andre.apiheroi.repositorio;

import com.andre.apiheroi.model.PoderModel;

import java.util.Optional;

public interface RepositoryPoder extends RepositoryBase<PoderModel> {

    Optional<PoderModel> findByHeroiId(Long heroiId);

    boolean existsByHeroiId(Long heroiId);
}
