# Java e WireMock — cotação de frete

[English version](README.en.md)

Um cliente HTTP em Java 21 e testes JUnit 5 contra uma API virtualizada com WireMock. O objetivo é conferir o que o cliente envia, como interpreta uma cotação e como responde a falhas do serviço.

## Executar

Java 21 e Maven 3.9 ou superior:

```bash
cp .env.example .env
mvn -B -ntp verify
```

No PowerShell, use `Copy-Item .env.example .env`. O arquivo aceita `CHAVE=valor`, sem aspas ou expansão de shell; é lido com `java.util.Properties`. Variáveis do processo têm prioridade. O token do exemplo é fictício. A URL e porta são fornecidas pelo servidor WireMock de cada execução, sem endereço fixo no cliente.

## Cenários

- Requisição POST com caminho, cabeçalhos e corpo esperados; valor monetário em BigDecimal.
- HTTP 401, 404 e 503: erro explícito, sem expor o corpo recebido e sem retry automático.
- Resposta lenta: timeout do cliente e uma única requisição.
- Serviço indisponível seguido de recuperação em uma nova chamada do consumidor.
- JSON inválido, campos ausentes, preço negativo, moeda incorreta e prazo inválido.
- CEPs fora do contrato são recusados antes de enviar HTTP.

O WireMock reinicia stubs e histórico entre testes. Os cenários não chamam transportadora real. Aqui a API local é intencional: **virtualização de serviço**, não uma simulação apresentada como teste de produção. Um contrato definido apenas neste repositório não comprova compatibilidade com uma API externa.

## Estrutura

`src/main/java/portfolio/ShippingClient.java` contém o cliente. `src/test/java/portfolio/ShippingClientTest.java` define stubs, falhas controladas e verificações de requisições. O CI executa `mvn verify` e guarda JUnit em artifacts.

Em **Actions**, abra a execução: **Summary** apresenta contagens e status da etapa; **Artifacts** permite baixar `junit-results` com XML JUnit, relatórios de texto do Surefire e uma cópia do resumo. Os arquivos ficam disponíveis por sete dias, inclusive em falhas. Ausência de relatório é indicada como execução não confirmada e falha a etapa de publicação do resumo.

[Estratégia e comunicação de resultados](docs/test-strategy.md) · [Execuções e artifacts no Actions](https://github.com/brunobaccari/java-wiremock-shipping/actions).

Referências: [WireMock com JUnit Jupiter](https://wiremock.org/docs/junit-jupiter/), [simulação de falhas](https://wiremock.org/docs/simulating-faults/), [cenários com estado](https://wiremock.org/docs/stateful-behaviour/).

Datas de commits deste portfólio foram reorganizadas retroativamente; as execuções do Actions mantêm suas datas reais.
