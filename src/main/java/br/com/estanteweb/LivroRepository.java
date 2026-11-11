package br.com.estanteweb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LivroRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Livro> listarTodos() {
        String sql = "SELECT * FROM livro";
        List<Map<String, Object>> linhas = jdbcTemplate.queryForList(sql);
        List<Livro> livros = new ArrayList<>();
        for (Map<String, Object> linha : linhas) {
            livros.add(Livro.deMap(linha));
        }
        return livros;
    }

    public int contarLivros() {
        String sql = "SELECT COUNT(*) FROM livro";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    public List<Livro> listarAtePreco(double precoMaximo) {
        String sql = "SELECT * FROM livro WHERE preco <= ?";
        List<Map<String, Object>> linhas = jdbcTemplate.queryForList(sql, precoMaximo);
        List<Livro> livros = new ArrayList<>();
        for (Map<String, Object> linha : linhas) {
            livros.add(Livro.deMap(linha));
        }
        return livros;
    }

    public double somarValorAcervo() {
        String sql = "SELECT SUM(preco) FROM livro";
        Double total = jdbcTemplate.queryForObject(sql, Double.class);
        return total != null ? total : 0.0;
    }
}
