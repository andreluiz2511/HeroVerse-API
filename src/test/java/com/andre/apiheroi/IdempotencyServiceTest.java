package com.andre.apiheroi;

import com.andre.apiheroi.excecao.conflictException;
import com.andre.apiheroi.service.IdempotencyService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class IdempotencyServiceTest {

    private final IdempotencyService service = new IdempotencyService();

    @Test
    void repeticaoDeChaveRetornaMesmoIdSemCriarOutraVez() {
        AtomicInteger criacoes = new AtomicInteger();
        IdempotencyService.Resultado primeiro = service.executar("heroi", "10", "chave-1", "Batman",
                () -> (long) criacoes.incrementAndGet());
        IdempotencyService.Resultado segundo = service.executar("heroi", "10", "chave-1", "Batman",
                () -> (long) criacoes.incrementAndGet());
        assertTrue(primeiro.criadoAgora());
        assertFalse(segundo.criadoAgora());
        assertEquals(primeiro.idCriado(), segundo.idCriado());
        assertEquals(1, criacoes.get());
    }

    @Test
    void mesmaChaveComCorpoDiferenteRetornaConflito() {
        service.executar("heroi", "10", "chave-2", "Batman", () -> 1L);
        assertThrows(conflictException.class,
                () -> service.executar("heroi", "10", "chave-2", "Flash", () -> 2L));
    }

    @Test
    void chavesSaoIsoladasPorUsuario() {
        assertTrue(service.executar("heroi", "10", "chave-3", "Batman", () -> 1L).criadoAgora());
        assertTrue(service.executar("heroi", "11", "chave-3", "Superman", () -> 2L).criadoAgora());
    }

    @Test
    void erroNaCriacaoLiberaAChaveParaNovaTentativa() {
        assertThrows(IllegalArgumentException.class,
                () -> service.executar("equipe", "10", "chave-4", "Liga", () -> {
                    throw new IllegalArgumentException("Falha de exemplo");
                }));
        assertTrue(service.executar("equipe", "10", "chave-4", "Liga", () -> 3L).criadoAgora());
    }

    @Test
    void requisicoesSimultaneasCriamApenasUmaVez() throws Exception {
        AtomicInteger criacoes = new AtomicInteger();
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<IdempotencyService.Resultado> chamada = () -> {
                largada.await();
                return service.executar("heroi", "10", "mesma-chave", "Flash", () -> {
                    criacoes.incrementAndGet();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return 99L;
                });
            };
            Future<IdempotencyService.Resultado> a = executor.submit(chamada);
            Future<IdempotencyService.Resultado> b = executor.submit(chamada);
            largada.countDown();
            var r1 = a.get(5, TimeUnit.SECONDS);
            var r2 = b.get(5, TimeUnit.SECONDS);
            assertEquals(1, criacoes.get());
            assertEquals(Long.valueOf(99L), r1.idCriado());
            assertEquals(Long.valueOf(99L), r2.idCriado());
            assertNotEquals(r1.criadoAgora(), r2.criadoAgora());
        } finally {
            executor.shutdownNow();
        }
    }
}
