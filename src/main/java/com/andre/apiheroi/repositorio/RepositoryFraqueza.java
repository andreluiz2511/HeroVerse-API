package com.andre.apiheroi.repositorio;

import com.andre.apiheroi.model.FraquezaModel;

import java.util.Optional;

public interface RepositoryFraqueza extends RepositoryBase<FraquezaModel> {

    Optional<FraquezaModel> findByHeroiId(Long heroiId);

    boolean existsByHeroiId(Long heroiId);
}
