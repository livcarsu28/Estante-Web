package br.com.estanteweb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SugestaoRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Sugestao> listarTodas() {
        String sql = "SELECT * FROM sugestao";

        return jdbcTemplate.queryForList(sql)
                .stream()
                .map(Sugestao::deMap)
                .toList();
    }

    public void salvar(Sugestao sugestao) {
        String sql = "INSERT INTO sugestao (nome_cliente, livro_sugerido) VALUES (?, ?)";

        jdbcTemplate.update(
                sql,
                sugestao.getNomeCliente(),
                sugestao.getLivroSugerido()
        );
    }
}