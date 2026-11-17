package com.andre.apiheroi.service;

import com.andre.apiheroi.dto.response.LoginResponseDTO;
import com.andre.apiheroi.excecao.unauthorizedException;
import com.andre.apiheroi.model.PerfilModel;
import com.andre.apiheroi.repositorio.RepositoryPerfil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final RepositoryPerfil repositoryPerfil;
    private final PasswordEncoder passwordEncoder;

    public AuthService(RepositoryPerfil repositoryPerfil, PasswordEncoder passwordEncoder) {
        this.repositoryPerfil = repositoryPerfil;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public LoginResponseDTO login(String email, String senha) {
        PerfilModel perfil = repositoryPerfil.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new unauthorizedException("E-mail ou senha inválidos"));
        if (perfil.getSenha() == null || !passwordEncoder.matches(senha, perfil.getSenha())) {
            throw new unauthorizedException("E-mail ou senha inválidos");
        }
        String apiKey = UUID.randomUUID().toString();
        perfil.setApiKey(apiKey);
        return new LoginResponseDTO(apiKey, perfil.getId(), perfil.getNome(), perfil.getEmail(),
                "Login realizado. Envie a apiKey no header X-API-Key nas operações protegidas.");
    }

    public PerfilModel validar(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new unauthorizedException("Esta operação exige login: envie o header X-API-Key (obtenha a chave em POST /api/auth/login)");
        }
        return repositoryPerfil.findByApiKey(apiKey)
                .orElseThrow(() -> new unauthorizedException("X-API-Key inválida. Faça login novamente em POST /api/auth/login"));
    }

    @Transactional
    public void logout(String apiKey) {
        validar(apiKey).setApiKey(null);
    }
}
