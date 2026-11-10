package br.com.estanteweb;

import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import freemarker.core.InvalidReferenceException;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FreeMarkerTemplatesTests {
    private Configuration configuration;

    @BeforeEach
    void preparar() throws Exception {
        configuration = new Configuration(Configuration.VERSION_2_3_35);
        configuration.setDirectoryForTemplateLoading(Path.of("src/main/resources/templates").toFile());
        configuration.setDefaultEncoding("UTF-8");
        configuration.setLocale(Locale.US);
        configuration.setRecognizeStandardFileExtensions(true);
        configuration.setLogTemplateExceptions(false);
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    private String renderizar(String nome, Map<String, Object> dados) throws Exception {
        StringWriter saida = new StringWriter();
        configuration.getTemplate(nome).process(dados, saida);
        return saida.toString();
    }

    @Test
    void anoSemAgrupamentoEVariavelObrigatoria() throws Exception {
        String html = renderizar("ola.ftlh", Map.of("nomeLoja", "EstanteWeb", "anoFundacao", 1998));
        assertTrue(html.contains("Bem-vindo à EstanteWeb!"));
        assertTrue(html.contains("desde 1998"));
        assertFalse(html.contains("1,998"));
        assertThrows(InvalidReferenceException.class,
                () -> renderizar("ola.ftlh", Map.of("anoFundacao", 1998)));
    }

    @Test
    void acervoVazioEAcervoComTags() throws Exception {
        String vazio = renderizar("livros.ftlh", Map.of("livros", List.of()));
        assertTrue(vazio.contains("Nenhum livro no acervo no momento."));
        assertFalse(vazio.contains("<li>"));
        String html = renderizar("livros.ftlh", Map.of("livros", new LivroRepository().listarTodos()));
        assertEquals(6, html.split("<li>", -1).length - 1);
        assertTrue(html.contains("O &lt;b&gt;Grande&lt;/b&gt; Gatsby"));
        assertFalse(html.contains("<b>Grande</b>"));
        assertTrue(html.contains("<header>"));
        assertTrue(html.contains("href=\"/\""));
    }

    @Test
    void sugestoesVaziasEDadosDoVisitanteEscapados() throws Exception {
        assertTrue(renderizar("sugestoes.ftlh", Map.of("sugestoes", List.of()))
                .contains("Nenhuma sugestão recebida"));
        String html = renderizar("sugestoes.ftlh", Map.of("sugestoes",
                List.of(new Sugestao("Ana <teste>", "<script>livro</script>"))));
        assertTrue(html.contains("&lt;script&gt;livro&lt;/script&gt;"));
        assertTrue(html.contains("Ana &lt;teste&gt;"));
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("<header>"));
    }
}
