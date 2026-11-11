package br.com.estanteweb;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EstantewebApplicationTests {
    @Autowired
    private LivroRepository repository;

    @Value("${local.server.port}")
    private int port;

    private HttpResponse<String> get(String caminho) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + caminho)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void paginasRespondemERecursoInexistenteRetorna404() throws Exception {
        HttpResponse<String> inicio = get("/");
        assertEquals(200, inicio.statusCode());
        assertTrue(inicio.body().contains("livros usados com história"));
        assertEquals(200, get("/sobre").statusCode());
        assertEquals(404, get("/rota-inexistente").statusCode());
    }

    @Test
    void totalCorrespondeAoAcervoIncluindoLivroDaMissao43() throws Exception {
        assertEquals(6, repository.listarTodos().size());
        assertEquals(repository.listarTodos().size(), repository.contarLivros());
        HttpResponse<String> acervo = get("/livros");
        assertEquals(200, acervo.statusCode());
        assertTrue(acervo.body().startsWith("Acervo EstanteWeb: "));
        for (Livro livro : repository.listarTodos()) {
            assertTrue(acervo.body().contains(livro.getTitulo() + ", de " + livro.getAutor()
                    + " (R$ " + livro.getPreco() + ")"));
        }
        assertEquals("O acervo tem " + repository.contarLivros() + " livros.", get("/livros/total").body());
    }

    @Test
    void contentTypeDistingueTextoPuroDeHtml() throws Exception {
        HttpResponse<String> texto = get("/livros");
        HttpResponse<String> html = get("/livros/html");
        assertEquals(200, html.statusCode());
        assertTrue(texto.headers().firstValue("Content-Type").orElseThrow().startsWith("text/plain"));
        assertTrue(html.headers().firstValue("Content-Type").orElseThrow().startsWith("text/html"));
        assertTrue(texto.body().contains("O <b>Grande</b> Gatsby"));
        assertTrue(html.body().contains("<ul>"));
        assertTrue(html.body().contains("O &lt;b&gt;Grande&lt;/b&gt; Gatsby</a>, de F. Scott Fitzgerald"));
        assertFalse(html.body().contains("<b>Grande</b>"));
        assertEquals(6, html.body().split("<li>", -1).length - 1);
    }
    @Test
    void olaRenderizaVariaveisEAnoSemSeparador() throws Exception {
        HttpResponse<String> resposta = get("/ola");
        assertEquals(200, resposta.statusCode());
        assertTrue(resposta.body().contains("Bem-vindo à EstanteWeb!"));
        assertTrue(resposta.body().contains("desde 1998"));
        assertFalse(resposta.body().contains("1,998"));
        assertFalse(resposta.body().contains("${nomeLoja}"));
    }

    @Test
    void listagensIncluemCabecalhoCompartilhado() throws Exception {
        for (String rota : new String[]{"/livros/html", "/sugestoes"}) {
            HttpResponse<String> resposta = get(rota);
            assertEquals(200, resposta.statusCode());
            assertTrue(resposta.body().contains("<header>"));
            assertTrue(resposta.body().contains("<h1>EstanteWeb</h1>"));
            assertTrue(resposta.body().contains("href=\"/\""));
        }
    }
}
