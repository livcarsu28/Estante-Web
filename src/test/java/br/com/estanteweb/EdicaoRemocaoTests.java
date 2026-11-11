package br.com.estanteweb;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.file.Path;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class EdicaoRemocaoTests {
    private SingleConnectionDataSource dataSource;
    private JdbcTemplate jdbc;
    private LivroRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void preparar() throws Exception {
        dataSource = new SingleConnectionDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL", "sa", "", true);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE livro (id BIGINT AUTO_INCREMENT PRIMARY KEY, titulo VARCHAR(255), "
                + "autor VARCHAR(255), ano INT, preco DECIMAL(10,2))");
        jdbc.update("INSERT INTO livro VALUES (?, ?, ?, ?, ?)", 1998L, "Dom Casmurro", "Machado de Assis", 1899, 480);
        jdbc.update("INSERT INTO livro VALUES (?, ?, ?, ?, ?)", 20L, "Livro de controle", "Outra autora", 2000, 12);
        repository = new LivroRepository();
        injetar(repository, "jdbcTemplate", jdbc);
        LivroController controller = new LivroController();
        injetar(controller, "repository", repository);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private void injetar(Object alvo, String nome, Object valor) throws Exception {
        Field campo = alvo.getClass().getDeclaredField(nome);
        campo.setAccessible(true);
        campo.set(alvo, valor);
    }

    @AfterEach
    void fechar() {
        if (dataSource != null) dataSource.destroy();
    }

    private String renderizar(String nome, Map<String, Object> dados) throws Exception {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_35);
        configuration.setDirectoryForTemplateLoading(Path.of("src/main/resources/templates").toFile());
        configuration.setDefaultEncoding("UTF-8");
        configuration.setRecognizeStandardFileExtensions(true);
        StringWriter saida = new StringWriter();
        configuration.getTemplate(nome + ".ftlh").process(dados, saida);
        return saida.toString();
    }

    private MvcResult editar(long id, String titulo, String autor, String ano, String preco) throws Exception {
        return mvc.perform(post("/livros/" + id).param("titulo", titulo).param("autor", autor)
                .param("ano", ano).param("preco", preco)).andReturn();
    }

    @Test
    void fichaLigaEdicaoERemocaoEFormularioAbrePreenchido() throws Exception {
        var ficha = mvc.perform(get("/livros/1998")).andReturn();
        String htmlFicha = renderizar("livro", ficha.getModelAndView().getModel());
        assertTrue(htmlFicha.contains("href=\"/livros/1998/editar\""));
        assertTrue(htmlFicha.contains("href=\"/livros/1998/excluir\""));
        var resultado = mvc.perform(get("/livros/1998/editar")).andReturn();
        assertEquals(200, resultado.getResponse().getStatus());
        assertEquals("livro-editar", resultado.getModelAndView().getViewName());
        String html = renderizar("livro-editar", resultado.getModelAndView().getModel());
        for (String valor : new String[]{"Dom Casmurro", "Machado de Assis", "1899", "480.0"}) {
            assertTrue(html.contains("value=\"" + valor + "\""));
        }
        assertTrue(html.contains("method=\"post\" action=\"/livros/1998\""));
        assertEquals(480, repository.buscarPorId(1998L).getPreco());
        assertEquals(2, repository.contarLivros());
    }

    @Test
    void corrigeApenasPrecoDoSelecionadoEPreservaDemaisCampos() throws Exception {
        var resultado = editar(1998L, "Dom Casmurro", "Machado de Assis", "1899", "48,00");
        assertEquals(302, resultado.getResponse().getStatus());
        assertEquals("/livros/1998", resultado.getResponse().getHeader("Location"));
        Livro livro = repository.buscarPorId(1998L);
        assertEquals(48, livro.getPreco());
        assertEquals("Dom Casmurro", livro.getTitulo());
        assertEquals("Machado de Assis", livro.getAutor());
        assertEquals(1899, livro.getAno());
        assertEquals(12, repository.buscarPorId(20L).getPreco());
        for (int i = 0; i < 2; i++) {
            var ficha = mvc.perform(get("/livros/1998")).andReturn();
            assertEquals(48, ((Livro) ficha.getModelAndView().getModel().get("livro")).getPreco());
        }
        assertEquals(2, repository.contarLivros());
    }

    @Test
    void reenviarMesmosValoresContinuaValido() throws Exception {
        assertEquals(302, editar(1998L, "Dom Casmurro", "Machado de Assis", "1899", "480").getResponse().getStatus());
        assertEquals(2, repository.contarLivros());
    }

    @Test
    void invalidosVoltamComCamposSemAlterarBanco() throws Exception {
        String[][] casos = {{" ", "Autora", "2000", "48"}, {"Livro", " ", "2000", "48"},
                {"Livro", "Autora", "abc", "48"}, {"Livro", "Autora", "2000", "abc"},
                {"Livro", "Autora", "2000", "0"}, {"Livro", "Autora", "2000", "-1"},
                {"Livro", "Autora", "2000", "NaN"}, {"Livro", "Autora", "2000", "Infinity"}};
        for (String[] caso : casos) {
            var resultado = editar(1998L, caso[0], caso[1], caso[2], caso[3]);
            assertEquals(200, resultado.getResponse().getStatus());
            assertEquals("livro-editar", resultado.getModelAndView().getViewName());
            var dados = resultado.getModelAndView().getModel();
            assertTrue(dados.containsKey("erro"));
            String[] campos = {"titulo", "autor", "ano", "preco"};
            for (int i = 0; i < campos.length; i++) assertEquals(caso[i], dados.get(campos[i]));
            assertTrue(renderizar("livro-editar", dados).contains("<form"));
            assertEquals(480, repository.buscarPorId(1998L).getPreco());
            assertEquals(12, repository.buscarPorId(20L).getPreco());
        }
    }

    @Test
    void cadastroTambemPreservaEntradaInvalidaESucessoNaoVazaCamposNaUrl() throws Exception {
        var invalido = mvc.perform(post("/livros").param("titulo", "Livro").param("autor", "Autora")
                .param("ano", "abc").param("preco", "12,50")).andReturn();
        assertEquals("livro-novo", invalido.getModelAndView().getViewName());
        String html = renderizar("livro-novo", invalido.getModelAndView().getModel());
        assertTrue(html.contains("value=\"abc\""));
        assertTrue(html.contains("value=\"12,50\""));
        assertEquals(2, repository.contarLivros());
        var valido = mvc.perform(post("/livros").param("titulo", "Livro").param("autor", "Autora")
                .param("ano", "2001").param("preco", "12,50")).andReturn();
        assertEquals(302, valido.getResponse().getStatus());
        assertEquals("/livros/html", valido.getResponse().getHeader("Location"));
        assertEquals(3, repository.contarLivros());
    }

    @Test
    void tituloComApostrofoETagsPermaneceDadoSemAtingirOutroLivro() throws Exception {
        String titulo = "L'Étranger <b>teste</b>";
        assertEquals(302, editar(1998L, titulo, "Albert Camus", "1942", "48").getResponse().getStatus());
        assertEquals(titulo, repository.buscarPorId(1998L).getTitulo());
        assertEquals("Livro de controle", repository.buscarPorId(20L).getTitulo());
        var ficha = mvc.perform(get("/livros/1998")).andReturn();
        String html = renderizar("livro", ficha.getModelAndView().getModel());
        assertTrue(html.contains("&lt;b&gt;teste&lt;/b&gt;"));
        assertFalse(html.contains("<b>teste</b>"));
    }

    @Test
    void getExclusaoSoMostraConfirmacaoECancelamento() throws Exception {
        for (int i = 0; i < 2; i++) {
            var resultado = mvc.perform(get("/livros/1998/excluir")).andReturn();
            assertEquals(200, resultado.getResponse().getStatus());
            assertEquals("livro-excluir", resultado.getModelAndView().getViewName());
            String html = renderizar("livro-excluir", resultado.getModelAndView().getModel());
            assertTrue(html.contains("Dom Casmurro"));
            assertTrue(html.contains("method=\"post\" action=\"/livros/1998/excluir\""));
            assertTrue(html.contains("href=\"/livros/1998\""));
        }
        assertEquals(2, repository.contarLivros());
        assertNotNull(repository.buscarPorId(1998L));
    }

    @Test
    void postExclusaoRemoveSomenteSelecionadoERedireciona() throws Exception {
        var resultado = mvc.perform(post("/livros/1998/excluir")).andReturn();
        assertEquals(302, resultado.getResponse().getStatus());
        assertEquals("/livros/html", resultado.getResponse().getHeader("Location"));
        assertNull(repository.buscarPorId(1998L));
        assertNotNull(repository.buscarPorId(20L));
        assertEquals(1, repository.contarLivros());
        assertEquals(404, mvc.perform(get("/livros/1998")).andReturn().getResponse().getStatus());
        assertEquals(404, mvc.perform(post("/livros/1998/excluir")).andReturn().getResponse().getStatus());
    }

    @Test
    void enderecosInexistentes404EIdentificadoresInvalidos400() throws Exception {
        for (String sufixo : new String[]{"", "/editar", "/excluir"}) {
            assertEquals(404, mvc.perform(get("/livros/9999" + sufixo)).andReturn().getResponse().getStatus());
            assertEquals(400, mvc.perform(get("/livros/abc" + sufixo)).andReturn().getResponse().getStatus());
        }
        assertEquals(404, editar(9999L, "Livro", "Autora", "2000", "48").getResponse().getStatus());
        assertEquals(404, mvc.perform(post("/livros/9999/excluir")).andReturn().getResponse().getStatus());
        assertEquals(400, mvc.perform(post("/livros/abc/excluir")).andReturn().getResponse().getStatus());
        assertEquals(2, repository.contarLivros());
    }
}
