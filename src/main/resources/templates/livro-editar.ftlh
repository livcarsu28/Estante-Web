
<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <title>Editar livro - EstanteWeb</title>
</head>
<body>
   <#include "cabecalho.ftlh">

<h1>Editar livro</h1>

<#if erro??>
    <p>${erro}</p>
</#if>

<form method="post" action="/livros/${livro.id?c}">

    <label for="titulo">Título:</label>
    <input type="text" id="titulo" name="titulo" value="${titulo!}">
    <br><br>

    <label for="autor">Autor:</label>
    <input type="text" id="autor" name="autor" value="${autor!}">
    <br><br>

    <label for="ano">Ano:</label>
    <input type="text" id="ano" name="ano" value="${ano!}">
    <br><br>

    <label for="preco">Preço:</label>
    <input type="text" id="preco" name="preco" value="${preco!}">
    <br><br>

    <button type="submit">Salvar alterações</button>

</form>
<p><a href="/livros/${livro.id?c}">Cancelar e voltar para a ficha</a></p>
</body>
</html>