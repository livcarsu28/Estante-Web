package br.com.estanteweb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LivroGerenciamentoTests {
    private LivroRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        repository = mock(LivroRepository.class);
        LivroController controller = new LivroController();
        ReflectionTestUtils.setField(controller, "repository", repository);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        Livro livro = new Livro();
        livro.setId(1998L); livro.setTitulo("Dom Casmurro");
        livro.setAutor("Machado de Assis"); livro.setAno(1899); livro.setPreco(480);
        when(repository.buscarPorId(1998L)).thenReturn(livro);
        when(repository.atualizar(any(Livro.class))).thenReturn(1);
    }

    @Test
    void abreEdicaoPreenchidaSemGravar() throws Exception {
        mvc.perform(get("/livros/1998/editar")).andExpect(status().isOk())
            .andExpect(view().name("livro-editar"))
            .andExpect(model().attribute("titulo", "Dom Casmurro"))
            .andExpect(model().attribute("preco", "480.0"));
        verify(repository, never()).atualizar(any());
    }

    @Test
    void corrigePrecoPreservandoDemaisCamposERedireciona() throws Exception {
        mvc.perform(post("/livros/1998").param("titulo", "Dom Casmurro")
            .param("autor", "Machado de Assis").param("ano", "1899").param("preco", "48,00"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/livros/1998"));
        verify(repository).atualizar(argThat(l -> l.getId() == 1998L && l.getPreco() == 48
            && l.getAno() == 1899 && l.getTitulo().equals("Dom Casmurro")
            && l.getAutor().equals("Machado de Assis")));
    }

    @Test
    void entradaInvalidaPreservadaSemGravar() throws Exception {
        String[][] casos = {{" ", "Autor", "2000", "48"}, {"Título", " ", "2000", "48"},
            {"Título", "Autor", "abc", "48"}, {"L'Étranger", "Camus", "1942", "abc"},
            {"Título", "Autor", "2000", "0"}, {"Título", "Autor", "2000", "NaN"}};
        for (String[] c : casos) {
            mvc.perform(post("/livros/1998").param("titulo", c[0]).param("autor", c[1])
                .param("ano", c[2]).param("preco", c[3]))
                .andExpect(status().isOk()).andExpect(view().name("livro-editar"))
                .andExpect(model().attributeExists("erro"))
                .andExpect(model().attribute("titulo", c[0])).andExpect(model().attribute("preco", c[3]));
        }
        verify(repository, never()).atualizar(any());
    }

    @Test
    void getExclusaoSoConsultaEPostRemove() throws Exception {
        mvc.perform(get("/livros/1998/excluir")).andExpect(status().isOk())
            .andExpect(view().name("livro-excluir")).andExpect(model().attributeExists("livro"));
        verify(repository, never()).excluir(anyLong());
        when(repository.excluir(1998L)).thenReturn(1);
        mvc.perform(post("/livros/1998/excluir")).andExpect(redirectedUrl("/livros/html"));
        verify(repository).excluir(1998L);
    }

    @Test
    void inexistente404EIdInvalido400() throws Exception {
        for (String sufixo : new String[]{"", "/editar", "/excluir"}) {
            mvc.perform(get("/livros/999" + sufixo)).andExpect(status().isNotFound());
        }
        mvc.perform(post("/livros/999").param("titulo", "Título").param("autor", "Autor")
            .param("ano", "2000").param("preco", "48")).andExpect(status().isNotFound());
        mvc.perform(post("/livros/999/excluir")).andExpect(status().isNotFound());
        mvc.perform(get("/livros/abc/editar")).andExpect(status().isBadRequest());
        verify(repository, never()).atualizar(any());
    }

    @Test
    void sqlUsaWhereEParametrosParaIsolarRegistro() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        LivroRepository real = new LivroRepository();
        ReflectionTestUtils.setField(real, "jdbcTemplate", jdbc);
        Livro livro = new Livro(); livro.setId(1998L); livro.setTitulo("Dom Casmurro");
        livro.setAutor("Machado de Assis"); livro.setAno(1899); livro.setPreco(48);
        real.atualizar(livro); real.excluir(1998L);
        verify(jdbc).update("UPDATE livro SET titulo = ?, autor = ?, ano = ?, preco = ? WHERE id = ?",
            "Dom Casmurro", "Machado de Assis", 1899, 48.0, 1998L);
        verify(jdbc).update("DELETE FROM livro WHERE id = ?", 1998L);
        verifyNoMoreInteractions(jdbc);
    }
}
