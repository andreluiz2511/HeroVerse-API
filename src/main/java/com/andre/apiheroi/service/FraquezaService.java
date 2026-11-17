package com.andre.apiheroi.service;

import com.andre.apiheroi.dto.request.FraquezaRequestDTO;
import com.andre.apiheroi.excecao.conflictException;
import com.andre.apiheroi.excecao.notFoundException;
import com.andre.apiheroi.model.HeroiModel;
import com.andre.apiheroi.model.FraquezaModel;
import com.andre.apiheroi.repositorio.RepositoryFraqueza;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraquezaService {

    private final RepositoryFraqueza repository;
    private final HeroiService heroiService;

    public FraquezaService(RepositoryFraqueza repository, HeroiService heroiService) {
        this.repository = repository;
        this.heroiService = heroiService;
    }

    public Page<FraquezaModel> findAll(Pageable pagina) {
        return repository.findAll(pagina);
    }

    public FraquezaModel findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new notFoundException("Fraqueza não encontrada, ID: " + id));
    }

    public Page<FraquezaModel> findByName(String nome, Pageable pagina) {
        return repository.findByNomeContainingIgnoreCase(nome, pagina);
    }

    public FraquezaModel findByHeroi(Long heroiId) {
        heroiService.findById(heroiId);
        return repository.findByHeroiId(heroiId)
                .orElseThrow(() -> new notFoundException("O herói " + heroiId + " ainda não tem fraqueza"));
    }

    @Transactional
    public FraquezaModel create(FraquezaRequestDTO dto, Long idPerfilLogado) {
        HeroiModel heroi = heroiService.findById(dto.heroiId());
        heroiService.conferirDono(heroi, idPerfilLogado);
        if (repository.existsByHeroiId(heroi.getId())) {
            throw new conflictException("heroiId: Este herói já tem uma fraqueza (cada herói pode ter só uma)");
        }
        FraquezaModel fraqueza = repository.save(new FraquezaModel(dto.nome(), dto.descricao(), heroi));
        heroi.setFraqueza(fraqueza);
        return fraqueza;
    }

    @Transactional
    public FraquezaModel update(Long id, FraquezaRequestDTO dto, Long idPerfilLogado) {
        FraquezaModel fraqueza = findById(id);
        heroiService.conferirDono(fraqueza.getHeroi(), idPerfilLogado);
        if (!fraqueza.getHeroi().getId().equals(dto.heroiId())) {
            HeroiModel novoHeroi = heroiService.findById(dto.heroiId());
            heroiService.conferirDono(novoHeroi, idPerfilLogado);
            if (repository.existsByHeroiId(novoHeroi.getId())) {
                throw new conflictException("heroiId: Este herói já tem uma fraqueza (cada herói pode ter só uma)");
            }
            fraqueza.getHeroi().setFraqueza(null);
            fraqueza.setHeroi(novoHeroi);
            novoHeroi.setFraqueza(fraqueza);
        }
        fraqueza.setNome(dto.nome());
        fraqueza.setDescricao(dto.descricao());
        return fraqueza;
    }

    @Transactional
    public void delete(Long id, Long idPerfilLogado) {
        FraquezaModel fraqueza = findById(id);
        heroiService.conferirDono(fraqueza.getHeroi(), idPerfilLogado);
        fraqueza.getHeroi().setFraqueza(null);
        repository.delete(fraqueza);
        repository.flush();
    }
}
