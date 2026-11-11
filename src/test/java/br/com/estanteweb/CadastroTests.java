package br.com.estanteweb;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import freemarker.template.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.*;

// Testes isolados: usam apenas H2, nunca alteram o MySQL da aplicação.
class CadastroTests {
    private SingleConnectionDataSource dataSource;
    private JdbcTemplate jdbc;
    private LivroRepository livros;
    private LivroController livroController;
    private SugestaoRepository sugestoes;
    private SugestaoController sugestaoController;

    private void injetar(Object alvo, String nome, Object valor) throws Exception {
        Field campo = alvo.getClass().getDeclaredField(nome);
        campo.setAccessible(true);
        campo.set(alvo, valor);
    }

    @BeforeEach
    void preparar() throws Exception {
        dataSource = new SingleConnectionDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL", "sa", "", true);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE livro (id BIGINT AUTO_INCREMENT PRIMARY KEY, titulo VARCHAR(255), "
                + "autor VARCHAR(255), ano INT, preco DECIMAL(10,2))");
        jdbc.execute("CREATE TABLE sugestao (id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "nome_cliente VARCHAR(255), livro_sugerido VARCHAR(255))");
        livros = new LivroRepository();
        injetar(livros, "jdbcTemplate", jdbc);
        livroController = new LivroController();
        injetar(livroController, "repository", livros);
        sugestoes = new SugestaoRepository();
        injetar(sugestoes, "jdbcTemplate", jdbc);
        sugestaoController = new SugestaoController();
        injetar(sugestaoController, "repository", sugestoes);
    }

    @AfterEach
    void fechar() {
        if (dataSource != null) dataSource.destroy();
    }

    private String renderizar(String nome, Map<String, Object> dados, Locale locale) throws Exception {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_35);
        configuration.setDirectoryForTemplateLoading(Path.of("src/main/resources/templates").toFile());
        configuration.setDefaultEncoding("UTF-8");
        configuration.setLocale(locale);
        configuration.setRecognizeStandardFileExtensions(true);
        StringWriter saida = new StringWriter();
        configuration.getTemplate(nome + ".ftlh").process(dados, saida);
        return saida.toString();
    }

    @Test
    void formularioNovoAbreSemDados() throws Exception {
        assertEquals("livro-novo", livroController.novoLivro());
        String html = renderizar("livro-novo", Map.of(), Locale.US);
        assertTrue(html.contains("method=\"post\" action=\"/livros\""));
        assertTrue(html.contains("Novo livro - EstanteWeb"));
        for (String campo : new String[]{"titulo", "autor", "ano", "preco"}) {
            assertTrue(html.contains("name=\"" + campo + "\" value=\"\""));
        }
    }

    @Test
    void cadastroValidoGravaCamposEIdGeradoERedireciona() throws Exception {
        String retorno = livroController.salvarLivro("L'Étranger", "Albert Camus", "1942", "24.9",
                new ExtendedModelMap());
        assertEquals("redirect:/livros/html", retorno);
        assertEquals(1, livros.contarLivros());
        Livro salvo = livros.listarTodos().get(0);
        assertTrue(salvo.getId() > 0);
        assertEquals("L'Étranger", salvo.getTitulo());
        assertEquals("Albert Camus", salvo.getAutor());
        assertEquals(1942, salvo.getAno());
        assertEquals(24.9, salvo.getPreco(), 0.00001);
        assertEquals(24.9, livros.somarValorAcervo(), 0.00001);
    }

    @Test
    void validacaoAntesDoInsertPreservaTodosOsCampos() {
        Object[][] casos = {{"   ", "Autora", 24.9}, {"Título", "   ", 24.9},
                {"Título", "Autora", 0.0}, {"Título", "Autora", -2.0},
                {"Título", "Autora", Double.NaN}, {"Título", "Autora", Double.POSITIVE_INFINITY}};
        for (Object[] caso : casos) {
            ExtendedModelMap model = new ExtendedModelMap();
            assertEquals("livro-novo", livroController.salvarLivro((String) caso[0], (String) caso[1],
                    "1998", caso[2].toString(), model));
            assertTrue(model.containsAttribute("erro"));
            assertEquals(caso[0], model.getAttribute("titulo"));
            assertEquals(caso[1], model.getAttribute("autor"));
            assertEquals("1998", model.getAttribute("ano"));
            assertEquals(caso[2].toString(), model.getAttribute("preco"));
        }
        assertEquals(0, livros.contarLivros());
    }

    @Test
    void formularioComErroMantemNumerosValidosIndependentementeDoLocale() throws Exception {
        ExtendedModelMap model = new ExtendedModelMap();
        livroController.salvarLivro("", "Autora <teste>", "1998", "24.9", model);
        for (Locale locale : new Locale[]{Locale.US, Locale.forLanguageTag("pt-BR")}) {
            String html = renderizar("livro-novo", model.asMap(), locale);
            assertTrue(html.contains("value=\"1998\""));
            assertTrue(html.contains("value=\"24.9\""));
            assertTrue(html.contains("Autora &lt;teste&gt;"));
            assertFalse(html.contains("value=\"1,998\""));
            assertFalse(html.contains("value=\"1.998\""));
        }
    }

    @Test
    void sugestaoValidaPersisteENovoRepositorioLeDoMesmoBanco() throws Exception {
        assertEquals("redirect:/sugestoes?enviada=true", sugestaoController.adicionar(
                "Ana D'Ávila", "Livro <b>favorito</b>", new ExtendedModelMap()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM sugestao", Integer.class));
        SugestaoRepository novoRepositorio = new SugestaoRepository();
        injetar(novoRepositorio, "jdbcTemplate", jdbc);
        Sugestao salva = novoRepositorio.listarTodas().get(0);
        assertEquals("Ana D'Ávila", salva.getNomeCliente());
        assertEquals("Livro <b>favorito</b>", salva.getLivroSugerido());
        ExtendedModelMap model = new ExtendedModelMap();
        sugestaoController.listar(true, model);
        String html = renderizar("sugestoes", model.asMap(), Locale.US);
        assertTrue(html.contains("Livro &lt;b&gt;favorito&lt;/b&gt;"));
        assertTrue(html.contains("enviada com sucesso!"));
    }

    @Test
    void sugestoesInvalidasNaoSaoGravadas() {
        for (String[] campos : new String[][]{{" ", "Livro"}, {"Ana", " "}}) {
            ExtendedModelMap model = new ExtendedModelMap();
            assertEquals("sugestao-nova", sugestaoController.adicionar(campos[0], campos[1], model));
            assertTrue(model.containsAttribute("erro"));
            assertEquals(campos[0], model.getAttribute("nomeCliente"));
            assertEquals(campos[1], model.getAttribute("livroSugerido"));
        }
        assertTrue(sugestoes.listarTodas().isEmpty());
    }
}
