package com.andre.apiheroi.repositorio;

import com.andre.apiheroi.model.PerfilModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepositoryPerfil extends JpaRepository<PerfilModel, Long> {

    Page<PerfilModel> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    Optional<PerfilModel> findByEmailIgnoreCase(String email);

    Optional<PerfilModel> findByApiKey(String apiKey);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
