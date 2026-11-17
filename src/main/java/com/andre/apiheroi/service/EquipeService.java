package com.andre.apiheroi.service;

import com.andre.apiheroi.dto.request.EquipeRequestDTO;
import com.andre.apiheroi.excecao.forbiddenException;
import com.andre.apiheroi.excecao.notFoundException;
import com.andre.apiheroi.model.EquipeModel;
import com.andre.apiheroi.model.HeroiModel;
import com.andre.apiheroi.model.PerfilModel;
import com.andre.apiheroi.repositorio.RepositoryEquipe;
import com.andre.apiheroi.repositorio.RepositoryPerfil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipeService {

    private final RepositoryEquipe repository;
    private final RepositoryPerfil repositoryPerfil;
    private final HeroiService heroiService;

    public EquipeService(RepositoryEquipe repository, RepositoryPerfil repositoryPerfil, HeroiService heroiService) {
        this.repository = repository;
        this.repositoryPerfil = repositoryPerfil;
        this.heroiService = heroiService;
    }

    public Page<EquipeModel> findAll(Pageable pagina) {
        return repository.findAll(pagina);
    }

    public EquipeModel findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new notFoundException("Equipe não encontrada, ID: " + id));
    }

    public Page<EquipeModel> findByNome(String nome, Pageable pagina) {
        return repository.findByNomeContainingIgnoreCase(nome, pagina);
    }

    public Page<EquipeModel> findByPerfil(Long perfilId, Pageable pagina) {
        if (!repositoryPerfil.existsById(perfilId)) {
            throw new notFoundException("Perfil não encontrado, ID: " + perfilId);
        }
        return repository.buscarPorPerfil(perfilId, pagina);
    }

    public Page<EquipeModel> findByHeroi(Long heroiId, Pageable pagina) {
        heroiService.findById(heroiId);
        return repository.buscarPorHeroi(heroiId, pagina);
    }

    @Transactional
    public EquipeModel create(EquipeRequestDTO dto, PerfilModel dono) {
        return repository.save(new EquipeModel(dto.nome(), dono));
    }

    @Transactional
    public EquipeModel adicionarHeroi(Long equipeId, Long heroiId, Long idPerfilLogado) {
        EquipeModel equipe = findById(equipeId);
        conferirDono(equipe, idPerfilLogado);
        HeroiModel heroi = heroiService.findById(heroiId);
        if (!heroi.getPerfil().getId().equals(idPerfilLogado)) {
            throw new forbiddenException("Só é permitido adicionar heróis do seu próprio perfil à equipe");
        }
        boolean jaAdicionado = equipe.getHerois().stream().anyMatch(h -> h.getId().equals(heroiId));
        if (!jaAdicionado) {
            equipe.getHerois().add(heroi);
            heroi.getEquipes().add(equipe);
        }
        return equipe;
    }

    @Transactional
    public EquipeModel removerHeroi(Long equipeId, Long heroiId, Long idPerfilLogado) {
        EquipeModel equipe = findById(equipeId);
        conferirDono(equipe, idPerfilLogado);
        HeroiModel heroi = heroiService.findById(heroiId);
        if (!equipe.getHerois().remove(heroi)) {
            throw new notFoundException("O herói " + heroiId + " não faz parte desta equipe");
        }
        heroi.getEquipes().remove(equipe);
        return equipe;
    }

    @Transactional
    public EquipeModel update(Long id, EquipeRequestDTO dto, Long idPerfilLogado) {
        EquipeModel equipe = findById(id);
        conferirDono(equipe, idPerfilLogado);
        equipe.setNome(dto.nome());
        return equipe;
    }

    @Transactional
    public void delete(Long id, Long idPerfilLogado) {
        EquipeModel equipe = findById(id);
        conferirDono(equipe, idPerfilLogado);
        removerComHerois(equipe);
        repository.flush();
    }

    void removerComHerois(EquipeModel equipe) {
        for (HeroiModel heroi : equipe.getHerois()) {
            heroi.getEquipes().remove(equipe);
        }
        equipe.getHerois().clear();
        repository.delete(equipe);
    }

    private void conferirDono(EquipeModel equipe, Long idPerfilLogado) {
        if (!equipe.getPerfil().getId().equals(idPerfilLogado)) {
            throw new forbiddenException("Esta equipe pertence a outro perfil: você só pode alterar ou excluir as suas");
        }
    }
}
