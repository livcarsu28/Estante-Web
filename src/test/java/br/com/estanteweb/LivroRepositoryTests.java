package br.com.estanteweb;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static org.junit.jupiter.api.Assertions.*;

// SQL executado de verdade em H2; a aplicação continua configurada para MySQL.
class LivroRepositoryTests {
    private SingleConnectionDataSource dataSource;
    private JdbcTemplate jdbc;
    private LivroRepository repository;

    @BeforeEach
    void preparar() throws Exception {
        dataSource = new SingleConnectionDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "", true);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE livro (id BIGINT AUTO_INCREMENT PRIMARY KEY, titulo VARCHAR(255), "
                + "autor VARCHAR(255), ano INT, preco DECIMAL(10,2))");
        jdbc.update("INSERT INTO livro (titulo, autor, ano, preco) VALUES (?, ?, ?, ?)",
                "O <b>Grande</b> Gatsby", "F. Scott Fitzgerald", 1925, 28.0);
        jdbc.update("INSERT INTO livro (titulo, autor, ano, preco) VALUES (?, ?, ?, ?)",
                "Livro no limite", "Autor Dois", 2000, 25.0);
        jdbc.update("INSERT INTO livro (titulo, autor, ano, preco) VALUES (?, ?, ?, ?)",
                "Livro econômico", "Autor Três", 2010, 10.0);
        repository = new LivroRepository();
        injetar(repository, "jdbcTemplate", jdbc);
    }

    private void injetar(Object alvo, String nome, Object valor) throws Exception {
        Field campo = alvo.getClass().getDeclaredField(nome);
        campo.setAccessible(true);
        campo.set(alvo, valor);
    }

    @AfterEach
    void fechar() {
        if (dataSource != null) {
            dataSource.destroy();
        }
    }

    @Test
    void converteMapComTiposNumericosDiferentes() {
        Livro livro = Livro.deMap(Map.of("id", new BigDecimal("3000000000"),
                "titulo", "Título", "autor", "Autor", "ano", 1998L, "preco", new BigDecimal("24.90")));
        assertEquals(3000000000L, livro.getId());
        assertEquals(1998, livro.getAno());
        assertEquals(24.9, livro.getPreco(), 0.00001);
        assertEquals("Título", livro.getTitulo());
        assertEquals("Autor", livro.getAutor());
    }

    @Test
    void consultaBancoEConverteTodasAsColunas() {
        assertEquals(3, repository.contarLivros());
        var livros = repository.listarTodos();
        assertEquals(3, livros.size());
        Livro livro = livros.stream().filter(l -> l.getTitulo().contains("Gatsby")).findFirst().orElseThrow();
        assertTrue(livro.getId() > 0);
        assertEquals("O <b>Grande</b> Gatsby", livro.getTitulo());
        assertEquals("F. Scott Fitzgerald", livro.getAutor());
        assertEquals(1925, livro.getAno());
        assertEquals(28.0, livro.getPreco(), 0.00001);
    }

    @Test
    void insercaoEAlteracaoAparecemSemRecriarRepositorio() {
        jdbc.update("INSERT INTO livro (titulo, autor, ano, preco) VALUES (?, ?, ?, ?)",
                "Livro novo", "Autora Nova", 2020, 12.5);
        assertEquals(4, repository.contarLivros());
        long id = repository.listarTodos().stream().filter(l -> l.getTitulo().equals("Livro novo"))
                .findFirst().orElseThrow().getId();
        jdbc.update("UPDATE livro SET titulo = ? WHERE id = ?", "Título atualizado", id);
        assertTrue(repository.listarTodos().stream().anyMatch(l -> l.getId() == id
                && l.getTitulo().equals("Título atualizado")));
    }

    @Test
    void filtroUsaLimiteInclusivoENaoTrazLivrosMaisCaros() {
        var encontrados = repository.listarAtePreco(25);
        assertEquals(2, encontrados.size());
        assertTrue(encontrados.stream().anyMatch(l -> l.getPreco() == 25));
        assertTrue(encontrados.stream().allMatch(l -> l.getPreco() <= 25));
        assertEquals(3, repository.listarAtePreco(999).size());
        assertTrue(repository.listarAtePreco(0).isEmpty());
    }

    @Test
    void somaDoBancoEVazioSemNull() {
        assertEquals(63.0, repository.somarValorAcervo(), 0.00001);
        jdbc.update("DELETE FROM livro");
        assertEquals(0, repository.contarLivros());
        assertTrue(repository.listarTodos().isEmpty());
        assertEquals(0.0, repository.somarValorAcervo(), 0.00001);
    }

    @Test
    void controllerRecebeRepositorioEUsaDadosConsultados() throws Exception {
        LivroController controller = new LivroController();
        injetar(controller, "repository", repository);
        assertEquals("O acervo tem 3 livros.", controller.total());
        var resposta = controller.buscar(25);
        assertEquals(200, resposta.getStatusCode().value());
        assertEquals("2", resposta.getHeaders().getFirst("X-Total-Encontrados"));
        assertEquals(404, controller.buscar(0).getStatusCode().value());
        assertEquals("O valor total do acervo é R$ 63.0", controller.valorTotal());
    }
}
