package com.andre.apiheroi.service;

import com.andre.apiheroi.dto.request.HeroiRequestDTO;
import com.andre.apiheroi.dto.request.HeroiV2RequestDTO;
import com.andre.apiheroi.excecao.forbiddenException;
import com.andre.apiheroi.excecao.notFoundException;
import com.andre.apiheroi.model.EquipeModel;
import com.andre.apiheroi.model.HeroiModel;
import com.andre.apiheroi.model.NivelForca;
import com.andre.apiheroi.model.PerfilModel;
import com.andre.apiheroi.repositorio.RepositoryEquipe;
import com.andre.apiheroi.repositorio.RepositoryFraqueza;
import com.andre.apiheroi.repositorio.RepositoryHeroi;
import com.andre.apiheroi.repositorio.RepositoryPerfil;
import com.andre.apiheroi.repositorio.RepositoryPoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
public class HeroiService {

    private final RepositoryHeroi repository;
    private final RepositoryPerfil repositoryPerfil;
    private final RepositoryEquipe repositoryEquipe;
    private final RepositoryPoder repositoryPoder;
    private final RepositoryFraqueza repositoryFraqueza;

    public HeroiService(RepositoryHeroi repository, RepositoryPerfil repositoryPerfil, RepositoryEquipe repositoryEquipe,
                        RepositoryPoder repositoryPoder, RepositoryFraqueza repositoryFraqueza) {
        this.repository = repository;
        this.repositoryPerfil = repositoryPerfil;
        this.repositoryEquipe = repositoryEquipe;
        this.repositoryPoder = repositoryPoder;
        this.repositoryFraqueza = repositoryFraqueza;
    }

    public Page<HeroiModel> findAll(Pageable pagina) {
        return repository.findAll(pagina);
    }

    public HeroiModel findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new notFoundException("Herói não encontrado, ID: " + id));
    }

    public Page<HeroiModel> findByName(String texto, Pageable pagina) {
        return repository.buscarPorNomeOuNomeHeroico(texto, pagina);
    }

    public Page<HeroiModel> findByNivelForca(NivelForca nivelForca, Pageable pagina) {
        return repository.findByNivelForca(nivelForca, pagina);
    }

    public Page<HeroiModel> findByPerfil(Long perfilId, Pageable pagina) {
        if (!repositoryPerfil.existsById(perfilId)) {
            throw new notFoundException("Perfil não encontrado, ID: " + perfilId);
        }
        return repository.buscarPorPerfil(perfilId, pagina);
    }

    public Page<HeroiModel> findByEquipe(Long equipeId, Pageable pagina) {
        if (!repositoryEquipe.existsById(equipeId)) {
            throw new notFoundException("Equipe não encontrada, ID: " + equipeId);
        }
        return repository.buscarPorEquipe(equipeId, pagina);
    }

    @Transactional
    public HeroiModel create(HeroiRequestDTO dto, PerfilModel dono) {
        return repository.save(new HeroiModel(dto.nome(), dto.nomeHeroico(), dto.nivelForca(), null, dono));
    }

    @Transactional
    public HeroiModel createV2(HeroiV2RequestDTO dto, PerfilModel dono) {
        return repository.save(new HeroiModel(dto.nome(), dto.nomeHeroico(), dto.nivelForca(), dto.forca(), dono));
    }

    @Transactional
    public HeroiModel update(Long id, HeroiRequestDTO dto, Long idPerfilLogado) {
        HeroiModel heroi = findById(id);
        conferirDono(heroi, idPerfilLogado);
        heroi.setNome(dto.nome());
        heroi.setNomeHeroico(dto.nomeHeroico());
        heroi.setNivelForca(dto.nivelForca());
        return heroi;
    }

    @Transactional
    public HeroiModel updateV2(Long id, HeroiV2RequestDTO dto, Long idPerfilLogado) {
        HeroiModel heroi = findById(id);
        conferirDono(heroi, idPerfilLogado);
        heroi.setNome(dto.nome());
        heroi.setNomeHeroico(dto.nomeHeroico());
        heroi.setNivelForca(dto.nivelForca());
        heroi.setForca(dto.forca());
        return heroi;
    }

    @Transactional
    public void delete(Long id, Long idPerfilLogado) {
        HeroiModel heroi = findById(id);
        conferirDono(heroi, idPerfilLogado);
        removerComTudo(heroi);
        repository.flush();
    }

    void removerComTudo(HeroiModel heroi) {
        for (EquipeModel equipe : new ArrayList<>(heroi.getEquipes())) {
            equipe.getHerois().remove(heroi);
        }
        heroi.getEquipes().clear();
        if (heroi.getPoder() != null) {
            repositoryPoder.delete(heroi.getPoder());
            heroi.setPoder(null);
        }
        if (heroi.getFraqueza() != null) {
            repositoryFraqueza.delete(heroi.getFraqueza());
            heroi.setFraqueza(null);
        }
        repository.delete(heroi);
    }

    public void conferirDono(HeroiModel heroi, Long idPerfilLogado) {
        if (!heroi.getPerfil().getId().equals(idPerfilLogado)) {
            throw new forbiddenException("Este herói pertence a outro perfil: você só pode alterar ou excluir os seus");
        }
    }
}
