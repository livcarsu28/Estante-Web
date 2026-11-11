<!DOCTYPE html>
<html lang="pt-BR">
<head><meta charset="UTF-8"><title>Remover livro | EstanteWeb</title></head>
<body>
    <#include "cabecalho.ftlh">
    <h1>Remover livro</h1>
    <p>Deseja remover este livro do acervo? Esta ação não pode ser desfeita.</p>
    <dl>
        <dt>Título</dt><dd>${livro.titulo}</dd>
        <dt>Autor</dt><dd>${livro.autor}</dd>
        <dt>Ano</dt><dd>${livro.ano?c}</dd>
        <dt>Preço</dt><dd>R$ ${livro.preco?string["0.00"]}</dd>
    </dl>
    <form method="post" action="/livros/${livro.id?c}/excluir">
        <button type="submit">Confirmar remoção</button>
    </form>
    <p><a href="/livros/${livro.id?c}">Cancelar e voltar para a ficha</a></p>
</body>
</html>
