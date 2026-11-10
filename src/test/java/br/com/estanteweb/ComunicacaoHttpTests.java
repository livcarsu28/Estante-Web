package br.com.estanteweb;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ComunicacaoHttpTests {
    @Value("${local.server.port}")
    private int port;

    private HttpResponse<String> enviar(HttpRequest.Builder pedido) throws Exception {
        return HttpClient.newHttpClient().send(pedido.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder pedido(String caminho) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + caminho));
    }

    private String codificar(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }

    @Test
    void filtraIncluindoPrecoLimiteEInformaTotal() throws Exception {
        var r = enviar(pedido("/livros/busca?precoMaximo=24.9").GET());
        assertEquals(200, r.statusCode());
        assertEquals("4", r.headers().firstValue("X-Total-Encontrados").orElseThrow());
        assertTrue(r.body().contains("O Mapa da Rua Antiga"));
        assertFalse(r.body().contains("Viagem ao Último Capítulo"));
        assertFalse(r.body().contains("Gatsby"));
    }

    @Test
    void buscaSemParametroRetornaAcervoCompleto() throws Exception {
        var r = enviar(pedido("/livros/busca").GET());
        assertEquals(200, r.statusCode());
        assertEquals("6", r.headers().firstValue("X-Total-Encontrados").orElseThrow());
        assertEquals(enviar(pedido("/livros").GET()).body(), r.body());
    }

    @Test
    void buscaVazia404EParametroNaoNumerico400() throws Exception {
        var vazio = enviar(pedido("/livros/busca?precoMaximo=0").GET());
        assertEquals(404, vazio.statusCode());
        assertTrue(vazio.body().contains("Nenhum livro encontrado"));
        assertEquals(400, enviar(pedido("/livros/busca?precoMaximo=abc").GET()).statusCode());
    }

    @Test
    void leUserAgentDaRequisicao() throws Exception {
        var r = enviar(pedido("/livros/quem-acessa").header("User-Agent", "ClienteDeTeste/1.0").GET());
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("ClienteDeTeste/1.0"));
    }

    @Test
    void contatoAceitaPostERejeitaMensagemEmBranco() throws Exception {
        var formulario = enviar(pedido("/contato").GET());
        assertEquals(200, formulario.statusCode());
        assertTrue(formulario.headers().firstValue("Content-Type").orElseThrow().startsWith("text/html"));
        assertTrue(formulario.body().contains("method=\"post\" action=\"/contato\""));
        var ok = enviar(pedido("/contato").header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("nome=Ana&mensagem=" + codificar("Olá, EstanteWeb!"))));
        assertEquals(200, ok.statusCode());
        assertTrue(ok.body().contains("Obrigado, Ana!"));
        var vazio = enviar(pedido("/contato").header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("nome=Ana&mensagem=+++")));
        assertEquals(400, vazio.statusCode());
        assertEquals(400, enviar(pedido("/contato").header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("nome=Ana"))).statusCode());
    }

    @Test
    void sugestoesAcumulamCom201ECabecalhoESaoEscapadasNoHtml() throws Exception {
        var formulario = enviar(pedido("/sugestoes/nova").GET());
        assertTrue(formulario.body().contains("method=\"post\" action=\"/sugestoes\""));
        int anterior = 0;
        for (String titulo : new String[]{"Livro Um", "Livro Dois", "<script>alert(1)</script>"}) {
            var r = enviar(pedido("/sugestoes").header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("nomeCliente=Ana&livroSugerido=" + codificar(titulo))));
            assertEquals(201, r.statusCode());
            int total = Integer.parseInt(r.headers().firstValue("X-Total-Sugestoes").orElseThrow());
            assertTrue(total > anterior);
            anterior = total;
        }
        var lista = enviar(pedido("/sugestoes").GET());
        assertEquals(200, lista.statusCode());
        assertTrue(lista.body().contains("Livro Um"));
        assertTrue(lista.body().contains("Livro Dois"));
        assertTrue(lista.body().contains("&lt;script&gt;"));
        assertFalse(lista.body().contains("<script>"));
        assertEquals(0, new SugestaoRepository().contarSugestoes());
    }
}
