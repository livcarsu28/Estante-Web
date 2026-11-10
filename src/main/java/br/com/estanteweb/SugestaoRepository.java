package br.com.estanteweb;

import java.util.ArrayList;
import java.util.List;

// Dados somente em memória: reiniciar a aplicação esvazia as sugestões.
public class SugestaoRepository {
    private final List<Sugestao> sugestoes = new ArrayList<>();

    public synchronized void adicionar(Sugestao sugestao) {
        sugestoes.add(sugestao);
    }

    public synchronized int contarSugestoes() {
        return sugestoes.size();
    }

    public synchronized List<Sugestao> listarTodas() {
        return List.copyOf(sugestoes);
    }
}
