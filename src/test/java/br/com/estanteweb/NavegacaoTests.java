package br.com.estanteweb;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import freemarker.template.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class NavegacaoTests {
    private SingleConnectionDataSource dataSource;
    private LivroRepository repository;
    private MockMvc mvc;

    private void injetar(Object alvo, String nome, Object valor) throws Exception {
        Field campo = alvo.getClass().getDeclaredField(nome);
        campo.setAccessible(true);
        campo.set(alvo, valor);
    }

    @BeforeEach
    void preparar() throws Exception {
        dataSource = new SingleConnectionDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL", "sa", "", true);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE livro (id BIGINT AUTO_INCREMENT PRIMARY KEY, titulo VARCHAR(255), "
                + "autor VARCHAR(255), ano INT, preco DECIMAL(10,2))");
        jdbc.update("INSERT INTO livro VALUES (?, ?, ?, ?, ?)", 1998L, "Z <b>Livro</b>", "Autor A", 1998, 30);
        jdbc.update("INSERT INTO livro VALUES (?, ?, ?, ?, ?)", 3L, "A Livro", "Autor A", 2001, 10);
        jdbc.update("INSERT INTO livro VALUES (?, ?, ?, ?, ?)", 4L, "M Livro", "Autor B", 1990, 20);
        repository = new LivroRepository();
        injetar(repository, "jdbcTemplate", jdbc);
        LivroController controller = new LivroController();
        injetar(controller, "repository", repository);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    void fechar() {
        if (dataSource != null) dataSource.destroy();
    }

    private String renderizar(String nome, Map<String, Object> dados, Locale locale) throws Exception {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_35);
        configuration.setDirectoryForTemplateLoading(Path.of("src/main/resources/templates").toFile());
        configuration.setDefaultEncoding("UTF-8");
        configuration.setRecognizeStandardFileExtensions(true);
        configuration.setLocale(locale);
        StringWriter saida = new StringWriter();
        configuration.getTemplate(nome + ".ftlh").process(dados, saida);
        return saida.toString();
    }

    @Test
    void fichaRecebeLivroEOutrosTitulosDoMesmoAutor() throws Exception {
        MvcResult resultado = mvc.perform(get("/livros/1998")).andReturn();
        assertEquals(200, resultado.getResponse().getStatus());
        assertEquals("livro", resultado.getModelAndView().getViewName());
        Livro livro = (Livro) resultado.getModelAndView().getModel().get("livro");
        assertEquals(1998L, livro.getId());
        assertEquals("Z <b>Livro</b>", livro.getTitulo());
        String html = renderizar("livro", resultado.getModelAndView().getModel(), Locale.US);
        assertTrue(html.contains("Z &lt;b&gt;Livro&lt;/b&gt;"));
        assertTrue(html.contains("href=\"/livros/3\""));
        assertTrue(html.contains("Voltar para o acervo"));
        assertTrue(html.contains("Outros títulos do mesmo autor"));
    }

    @Test
    void inexistenteRetorna404EIdNaoNumerico400() throws Exception {
        assertNull(repository.buscarPorId(9999L));
        assertEquals(404, mvc.perform(get("/livros/9999")).andReturn().getResponse().getStatus());
        assertEquals(400, mvc.perform(get("/livros/abc")).andReturn().getResponse().getStatus());
    }

    @Test
    void rotasEstaticasNaoSaoConfundidasComIdentificador() throws Exception {
        var novo = mvc.perform(get("/livros/novo")).andReturn();
        assertEquals(200, novo.getResponse().getStatus());
        assertEquals("livro-novo", novo.getModelAndView().getViewName());
        assertEquals(200, mvc.perform(get("/livros/html")).andReturn().getResponse().getStatus());
        assertEquals(200, mvc.perform(get("/livros/total")).andReturn().getResponse().getStatus());
    }

    @Test
    void ordenacaoAceitaQuatroColunasEEntradaDesconhecidaNaoAlteraSql() {
        List<Comparator<Livro>> comparadores = List.of(Comparator.comparing(Livro::getTitulo),
                Comparator.comparing(Livro::getAutor), Comparator.comparingInt(Livro::getAno),
                Comparator.comparingDouble(Livro::getPreco));
        String[] colunas = {"titulo", "autor", "ano", "preco"};
        for (int i = 0; i < colunas.length; i++) {
            List<Livro> resultado = repository.listarOrdenadoPor(colunas[i]);
            for (int j = 1; j < resultado.size(); j++) {
                assertTrue(comparadores.get(i).compare(resultado.get(j - 1), resultado.get(j)) <= 0);
            }
        }
        List<Long> padrao = repository.listarTodos().stream().map(Livro::getId).toList();
        for (String invalida : new String[]{null, "invalida", "preco; DROP TABLE livro"}) {
            assertEquals(padrao, repository.listarOrdenadoPor(invalida).stream().map(Livro::getId).toList());
        }
        assertEquals(3, repository.contarLivros());
    }

    @Test
    void listaOrdenadaMantemLinksSemSeparadorDeMilhar() throws Exception {
        MvcResult resultado = mvc.perform(get("/livros/html").param("ordem", "preco")).andReturn();
        assertEquals(200, resultado.getResponse().getStatus());
        assertEquals("livros", resultado.getModelAndView().getViewName());
        for (Locale locale : new Locale[]{Locale.US, Locale.forLanguageTag("pt-BR")}) {
            String html = renderizar("livros", resultado.getModelAndView().getModel(), locale);
            assertTrue(html.contains("href=\"/livros/1998\""));
            assertFalse(html.contains("/livros/1,998"));
            assertFalse(html.contains("/livros/1.998"));
            assertTrue(html.indexOf("A Livro") < html.indexOf("M Livro"));
            assertTrue(html.indexOf("M Livro") < html.indexOf("Z &lt;b&gt;Livro"));
            assertTrue(html.contains("href=\"/livros/novo\""));
        }
    }

    @Test
    void autorSemOutrosLivrosNaoExibeSecao() throws Exception {
        assertTrue(repository.listarDoMesmoAutor("Autor B", 4L).isEmpty());
        assertEquals(1, repository.listarDoMesmoAutor("Autor A", 1998L).size());
        MvcResult resultado = mvc.perform(get("/livros/4")).andReturn();
        String html = renderizar("livro", resultado.getModelAndView().getModel(), Locale.US);
        assertFalse(html.contains("Outros títulos do mesmo autor"));
    }
}
