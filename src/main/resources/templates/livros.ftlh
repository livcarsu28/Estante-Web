<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <title>Acervo EstanteWeb</title>
</head>
<body>
    <#include "cabecalho.ftlh">
    <h2>Acervo EstanteWeb</h2>
    <#if livros?size == 0>
        <p>Nenhum livro no acervo no momento.</p>
    <#else>
        <ul>
            <#list livros as livro>
                <li>${livro.titulo}, de ${livro.autor} (R$ ${livro.preco?c})</li>
            </#list>
        </ul>
    </#if>
</body>
</html>
