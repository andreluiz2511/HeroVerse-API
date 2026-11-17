package com.andre.apiheroi.service;

import com.andre.apiheroi.dto.request.PerfilRequestDTO;
import com.andre.apiheroi.excecao.badRequestException;
import com.andre.apiheroi.excecao.conflictException;
import com.andre.apiheroi.excecao.forbiddenException;
import com.andre.apiheroi.excecao.notFoundException;
import com.andre.apiheroi.model.EquipeModel;
import com.andre.apiheroi.model.HeroiModel;
import com.andre.apiheroi.model.PerfilModel;
import com.andre.apiheroi.repositorio.RepositoryPerfil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
public class PerfilService {

    private final RepositoryPerfil repository;
    private final PasswordEncoder passwordEncoder;
    private final HeroiService heroiService;
    private final EquipeService equipeService;

    public PerfilService(RepositoryPerfil repository, PasswordEncoder passwordEncoder,
                         HeroiService heroiService, EquipeService equipeService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.heroiService = heroiService;
        this.equipeService = equipeService;
    }

    public Page<PerfilModel> findAll(Pageable pagina) {
        return repository.findAll(pagina);
    }

    public Page<PerfilModel> findByEmail(String email, Pageable pagina) {
        return repository.findByEmailContainingIgnoreCase(email, pagina);
    }

    public PerfilModel findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new notFoundException("Perfil não encontrado, ID: " + id));
    }

    @Transactional
    public PerfilModel create(PerfilRequestDTO dto) {
        if (dto.senha() == null || dto.senha().isBlank()) {
            throw new badRequestException("senha: A senha é obrigatória no cadastro");
        }
        if (repository.existsByEmailIgnoreCase(dto.email().trim())) {
            throw new conflictException("email: Este e-mail já está cadastrado");
        }
        return repository.save(new PerfilModel(dto.nome().trim(), dto.email().trim(), passwordEncoder.encode(dto.senha())));
    }

    @Transactional
    public PerfilModel update(Long id, PerfilRequestDTO dto, Long idPerfilLogado) {
        PerfilModel perfil = findById(id);
        conferirDono(perfil, idPerfilLogado);
        if (repository.existsByEmailIgnoreCaseAndIdNot(dto.email().trim(), id)) {
            throw new conflictException("email: Este e-mail já está cadastrado");
        }
        perfil.setNome(dto.nome().trim());
        perfil.setEmail(dto.email().trim());
        if (dto.senha() != null && !dto.senha().isBlank()) {
            perfil.setSenha(passwordEncoder.encode(dto.senha()));
        }
        return perfil;
    }

    @Transactional
    public void delete(Long id, Long idPerfilLogado) {
        PerfilModel perfil = findById(id);
        conferirDono(perfil, idPerfilLogado);
        for (EquipeModel equipe : new ArrayList<>(perfil.getEquipes())) {
            equipeService.removerComHerois(equipe);
        }
        perfil.getEquipes().clear();
        for (HeroiModel heroi : new ArrayList<>(perfil.getHerois())) {
            heroiService.removerComTudo(heroi);
        }
        perfil.getHerois().clear();
        repository.delete(perfil);
        repository.flush();
    }

    private void conferirDono(PerfilModel perfil, Long idPerfilLogado) {
        if (!perfil.getId().equals(idPerfilLogado)) {
            throw new forbiddenException("Você só pode alterar ou excluir o seu próprio perfil");
        }
    }
}
