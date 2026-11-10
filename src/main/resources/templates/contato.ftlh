<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <title>Contato | EstanteWeb</title>
</head>
<body>
    <#include "cabecalho.ftlh">
    <h2>Contato</h2>
    <#if erro??>
        <p role="alert">${erro}</p>
    </#if>
    <form method="post" action="/contato">
        <p><label for="nome">Nome:</label>
            <input type="text" id="nome" name="nome" value="${nome!}"></p>
        <p><label for="mensagem">Mensagem:</label>
            <textarea id="mensagem" name="mensagem">${mensagem!}</textarea></p>
        <button type="submit">Enviar mensagem</button>
    </form>
</body>
</html>
