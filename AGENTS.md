# Regras para alterações

- Leia o README e os cenários antes de alterar a aplicação ou os testes.
- Cruze a regra de negócio com o diff da aplicação e a expectativa do teste. Não ajuste o resultado esperado só para deixar a execução verde.
- Use dados fictícios. Não copie código, requisitos ou credenciais de clientes.
- Preserve a organização atual. Extraia uma keyword ou helper quando houver repetição real.
- Sem sleeps para esconder falhas. Espere por estado observável e investigue a causa antes de adicionar retry.
- Execute o comando documentado. Informe o que rodou e o que continua sem verificação.
