package com.andre.apiheroi.service;

import com.andre.apiheroi.excecao.badRequestException;
import com.andre.apiheroi.excecao.conflictException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Evita que dois POSTs com a mesma chave criem o mesmo recurso duas vezes.
 *
 * A chave e separada por recurso e por perfil autenticado. O cadastro de perfil
 * usa um escopo publico, pois o usuario ainda nao tem uma X-API-Key.
 * O resumo do corpo permite recusar reutilizacao da chave com dados diferentes.
 *
 * Armazenamento em memoria: as chaves existem enquanto a aplicacao estiver ligada.
 */
@Service
public class IdempotencyService {

    private final ConcurrentMap<Chave, CompletableFuture<Registro>> operacoes = new ConcurrentHashMap<>();

    public Resultado executar(String recurso, String escopo, String chave, Object dados, Supplier<Long> criacao) {
        if (chave == null || chave.isBlank() || chave.length() > 200) {
            throw new badRequestException("Envie X-Idempotency-Key com 1 a 200 caracteres");
        }
        if (escopo == null || escopo.isBlank()) {
            throw new badRequestException("Nao foi possivel identificar o responsavel pela operacao");
        }

        Chave identificador = new Chave(recurso, escopo, chave.trim());
        String hashCorpo = resumir(dados);

        // Apenas a primeira requisicao insere a tarefa e realiza o cadastro.
        // Requisicoes simultaneas aguardam o resultado dessa mesma tarefa.
        CompletableFuture<Registro> minhaTarefa = new CompletableFuture<>();
        CompletableFuture<Registro> tarefaExistente = operacoes.putIfAbsent(identificador, minhaTarefa);

        if (tarefaExistente == null) {
            try {
                Long id = criacao.get();
                minhaTarefa.complete(new Registro(hashCorpo, id));
                return new Resultado(id, true);
            } catch (RuntimeException | Error erro) {
                // Uma tentativa com erro nao reserva a chave para sempre: pode ser refeita.
                minhaTarefa.completeExceptionally(erro);
                operacoes.remove(identificador, minhaTarefa);
                throw erro;
            }
        }

        Registro anterior;
        try {
            anterior = tarefaExistente.join();
        } catch (CompletionException erro) {
            if (erro.getCause() instanceof RuntimeException causa) {
                throw causa;
            }
            if (erro.getCause() instanceof Error causa) {
                throw causa;
            }
            throw erro;
        }

        if (!anterior.hashCorpo().equals(hashCorpo)) {
            throw new conflictException("Esta X-Idempotency-Key ja foi utilizada com dados diferentes. Gere uma chave nova para outro cadastro.");
        }
        return new Resultado(anterior.idCriado(), false);
    }

    // Armazena apenas o hash, sem manter o corpo original ou a senha em memoria.
    private String resumir(Object dados) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(String.valueOf(dados).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException("SHA-256 nao disponivel", erro);
        }
    }

    private record Chave(String recurso, String escopo, String valor) {}
    private record Registro(String hashCorpo, Long idCriado) {}

    public record Resultado(Long idCriado, boolean criadoAgora) {}
}
