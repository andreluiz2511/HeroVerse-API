package com.andre.apiheroi.service;

import com.andre.apiheroi.dto.request.PoderRequestDTO;
import com.andre.apiheroi.excecao.conflictException;
import com.andre.apiheroi.excecao.notFoundException;
import com.andre.apiheroi.model.HeroiModel;
import com.andre.apiheroi.model.PoderModel;
import com.andre.apiheroi.repositorio.RepositoryPoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PoderService {

    private final RepositoryPoder repository;
    private final HeroiService heroiService;

    public PoderService(RepositoryPoder repository, HeroiService heroiService) {
        this.repository = repository;
        this.heroiService = heroiService;
    }

    public Page<PoderModel> findAll(Pageable pagina) {
        return repository.findAll(pagina);
    }

    public PoderModel findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new notFoundException("Poder não encontrado, ID: " + id));
    }

    public Page<PoderModel> findByName(String nome, Pageable pagina) {
        return repository.findByNomeContainingIgnoreCase(nome, pagina);
    }

    public PoderModel findByHeroi(Long heroiId) {
        heroiService.findById(heroiId);
        return repository.findByHeroiId(heroiId)
                .orElseThrow(() -> new notFoundException("O herói " + heroiId + " ainda não tem poder"));
    }

    @Transactional
    public PoderModel create(PoderRequestDTO dto, Long idPerfilLogado) {
        HeroiModel heroi = heroiService.findById(dto.heroiId());
        heroiService.conferirDono(heroi, idPerfilLogado);
        if (repository.existsByHeroiId(heroi.getId())) {
            throw new conflictException("heroiId: Este herói já tem um poder (cada herói pode ter só um)");
        }
        PoderModel poder = repository.save(new PoderModel(dto.nome(), dto.descricao(), heroi));
        heroi.setPoder(poder);
        return poder;
    }

    @Transactional
    public PoderModel update(Long id, PoderRequestDTO dto, Long idPerfilLogado) {
        PoderModel poder = findById(id);
        heroiService.conferirDono(poder.getHeroi(), idPerfilLogado);
        if (!poder.getHeroi().getId().equals(dto.heroiId())) {
            HeroiModel novoHeroi = heroiService.findById(dto.heroiId());
            heroiService.conferirDono(novoHeroi, idPerfilLogado);
            if (repository.existsByHeroiId(novoHeroi.getId())) {
                throw new conflictException("heroiId: Este herói já tem um poder (cada herói pode ter só um)");
            }
            poder.getHeroi().setPoder(null);
            poder.setHeroi(novoHeroi);
            novoHeroi.setPoder(poder);
        }
        poder.setNome(dto.nome());
        poder.setDescricao(dto.descricao());
        return poder;
    }

    @Transactional
    public void delete(Long id, Long idPerfilLogado) {
        PoderModel poder = findById(id);
        heroiService.conferirDono(poder.getHeroi(), idPerfilLogado);
        poder.getHeroi().setPoder(null);
        repository.delete(poder);
        repository.flush();
    }
}
