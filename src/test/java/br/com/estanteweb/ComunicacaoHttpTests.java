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
    void contatoInvalidoPreservaNomeESucessoRedireciona() throws Exception {
        var formulario = enviar(pedido("/contato").GET());
        assertEquals(200, formulario.statusCode());
        assertTrue(formulario.body().contains("method=\"post\" action=\"/contato\""));
        assertTrue(formulario.body().contains("value=\"\""));
        var vazio = enviar(pedido("/contato").header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("nome=Ana&mensagem=+++")));
        assertEquals(200, vazio.statusCode());
        assertTrue(vazio.body().contains("A mensagem não pode estar em branco."));
        assertTrue(vazio.body().contains("value=\"Ana\""));
        var ok = enviar(pedido("/contato").header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("nome=Ana&mensagem=" + codificar("Olá!"))));
        assertEquals(302, ok.statusCode());
        assertEquals("/contato/sucesso", ok.headers().firstValue("Location").orElseThrow());
        var sucesso = enviar(pedido("/contato/sucesso").GET());
        assertEquals(200, sucesso.statusCode());
        assertTrue(sucesso.body().contains("Sua mensagem foi recebida."));
        assertEquals(200, enviar(pedido("/contato/sucesso").GET()).statusCode());
    }

    @Test
    void sugestoesInvalidasPreservamCamposENaoSaoSalvas() throws Exception {
        String antes = enviar(pedido("/sugestoes").GET()).body();
        String[][] casos = {{"   ", "Livro a corrigir"}, {"Nome preservado", "   "}};
        for (String[] caso : casos) {
            var r = enviar(pedido("/sugestoes").header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("nomeCliente=" + codificar(caso[0])
                    + "&livroSugerido=" + codificar(caso[1]))));
            assertEquals(200, r.statusCode());
            assertTrue(r.body().contains("Preencha seu nome e o livro sugerido."));
            assertTrue(r.body().contains("value=\"" + caso[0] + "\""));
            assertTrue(r.body().contains("value=\"" + caso[1] + "\""));
        }
        assertEquals(antes, enviar(pedido("/sugestoes").GET()).body());
    }

    @Test
    void sugestaoValidaRedirecionaComFlagEGetNaoDuplica() throws Exception {
        String livro = "Livro de teste " + java.util.UUID.randomUUID();
        var r = enviar(pedido("/sugestoes").header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("nomeCliente=Ana&livroSugerido=" + codificar(livro))));
        assertEquals(302, r.statusCode());
        assertEquals("/sugestoes?enviada=true", r.headers().firstValue("Location").orElseThrow());
        var lista = enviar(pedido("/sugestoes?enviada=true").GET());
        assertEquals(200, lista.statusCode());
        assertTrue(lista.body().contains("Sua sugestão de leitura foi enviada com sucesso!"));
        assertTrue(lista.body().contains(livro));
        assertEquals(lista.body(), enviar(pedido("/sugestoes?enviada=true").GET()).body());
        assertFalse(enviar(pedido("/sugestoes").GET()).body().contains("enviada com sucesso!"));
        assertFalse(enviar(pedido("/sugestoes?enviada=false").GET()).body().contains("enviada com sucesso!"));
    }
}
