# OP Sukuna Benchmark

Este benchmark existe para testar a IA do OP Sukuna em lutas repetidas e controladas, sem depender de observacao manual dentro do jogo. A meta principal atual e medir se o `SukunaPerfect` consegue vencer o `ItadoriModulo` de forma consistente, enquanto a telemetria separa duas perguntas diferentes:

- o laboratorio rodou corretamente?
- quando o laboratorio rodou corretamente, a IA tomou boas decisoes?

Se o laboratorio falha, o resultado geral do benchmark deve ser tratado como invalido, mesmo quando algumas lutas individuais ainda tenham dados uteis.

## Como Rodar

Comando comum:

```bash
python3 scripts/op_sukuna_benchmark.py --scenario itadori_modulo --fights 40 --duration 2400 --parallel auto
```

Opcoes importantes:

- `--scenario itadori_modulo`: cenario suportado hoje.
- `--fights`: quantidade total de lutas.
- `--duration`: limite de ticks por luta.
- `--parallel auto`: deixa o script escolher a quantidade de arenas paralelas.
- `--max-parallel`: teto usado pelo modo automatico.
- `--arena-spacing`: distancia entre arenas.
- `--arena-starts-per-tick`: quantas arenas podem iniciar/preparar por tick.
- `--telemetry-profile full|balanced|minimal`: controla volume de coleta.
- `--sample-interval`: intervalo de amostras em ticks; `0` usa o padrao do perfil.
- `--stop-server` / `--no-stop-server`: fecha ou mantem o servidor apos o benchmark.
- `--single-phase`: roda apenas uma fase.
- `--two-phase`: roda uma fase curta de gate e, se passar, uma fase maior.
- `--analyze-only <dir>`: reprocessa um benchmark ja exportado, sem abrir servidor.

O script inicia `./gradlew runServer` com variaveis de ambiente `JJKU_OP_SUKUNA_BENCH_*`. O runner Forge le essas variaveis, cria arenas, executa lutas e exporta os arquivos.

## Fluxo Interno

1. O script Python configura o ambiente, inicia o servidor e acompanha o log.
2. O runner Forge detecta `JJKU_OP_SUKUNA_BENCH=true`.
3. O benchmark ativa as gamerules necessarias da IA e telemetria, e desativa spawn natural de mobs.
4. Cada arena passa por preparacao de chunks, construcao do piso, limpeza pre-spawn e spawn do par Sukuna/alvo.
5. O runner marca as entidades com tags persistentes de benchmark, registra o `fight_id` e faz validacao de warmup.
6. A luta roda ate morte do Sukuna, morte do alvo, timeout ou falha de laboratorio.
7. Ao terminar, o runner registra `benchmark_end` e `JJKU_BENCH_FINISHED`, exporta a telemetria e gera resumos.
8. O script Python encontra o export, reanalisa os JSONL e imprime o caminho do relatorio.

## Arenas e Laboratorio

As arenas sao espacadas para evitar que uma luta interfira em outra. Cada arena tem um centro, uma variante de distancia inicial e um conjunto proprio de entidades marcadas.

Antes de uma luta comecar, o runner:

- limpa entidades antigas da arena;
- constroi/limpa o espaco de combate;
- spawna Sukuna e ItadoriModulo;
- registra UUIDs, `fight_id`, `run_index` e `arena_index`;
- valida que as entidades continuam vivas e rastreaveis;
- faz warmup curto antes de liberar a luta.

Se algo falhar nessa etapa, a luta vira `lab_invalid`. Exemplos:

- `entity_spawn_failed`
- `entity_spawn_unstable`
- `entity_missing_during_warmup`
- `entity_died_during_warmup`
- `chunk_unloaded_during_warmup`
- `arena_contamination`

`lab_invalid` invalida o benchmark como evidencia final. Isso e intencional: se o laboratorio falhou, nao da para usar a taxa de vitoria como verdade.

## Arquivos Gerados

Os exports ficam em:

```text
run/jjkur-op-ai-metrics/benchmarks/<benchmark_id>/
```

Arquivos principais:

- `benchmark_summary.json`: resumo agregado, win rate, validade, outcomes, top actions e motivos de bloqueio.
- `benchmark_report.md`: versao legivel do resumo.
- `fight_summaries.jsonl`: uma linha por luta, com resultado, dano resolvido, decisoes, sintomas e integridade.
- `bad_windows.jsonl`: lutas suspeitas ou ruins, usadas para diagnostico rapido.
- `op_sukuna_brain-*.jsonl`: telemetria bruta de eventos e decisoes.
- `summary.json`, `fights.json`, `regressions.md`: exports gerais da telemetria OP Sukuna.

## Qualidade da Telemetria

O campo mais importante e `telemetry_quality` em `benchmark_summary.json`.

Campos principais:

- `valid`: verdadeiro somente se nao houve falha de laboratorio, contaminacao, erro de parse, lutas sem decisoes ou falta de decisoes reais.
- `decision_data_valid`: verdadeiro quando as decisoes reais foram capturadas corretamente, mesmo que o benchmark geral esteja invalido por `invalid_lab`.
- `invalid_lab_fights`: quantidade de lutas invalidadas pelo laboratorio.
- `contamination_fights`: lutas com sinais de interferencia entre arenas ou entidades externas.
- `real_decision_fights`: lutas com decisoes reais da IA.
- `synthetic_decision_fights`: lutas onde o analisador precisou sintetizar decisoes a partir de samples.
- `missing_decision_fights`: lutas sem janela de decisao suficiente.
- `parse_errors`: erros lendo JSONL.
- `invalid_reason`: resumo textual dos bloqueadores.

Regra pratica:

- `telemetry_quality.valid=true`: pode usar o benchmark como evidencia final.
- `telemetry_quality.valid=false` e `decision_data_valid=true`: pode analisar padroes de IA nas lutas validas, mas nao pode aceitar win rate como resultado definitivo.
- `decision_data_valid=false`: nao culpe a IA ainda; corrija coleta/export/parser primeiro.

## Como Interpretar Resultados

O benchmark contra `itadori_modulo` usa gate de `70%` de vitoria.

Outcomes:

- `target_dead`: Sukuna venceu.
- `sukuna_dead`: Sukuna perdeu.
- `both_dead`: ambos morreram.
- `timeout`: luta terminou por tempo, geralmente indicando baixa conversao ofensiva.
- `lab_invalid`: laboratorio falhou; invalida o benchmark.
- `runner_error` / `errors`: erro do runner ou processamento.

Sintomas comuns:

- `no_impact_loop`: muitas decisoes sem dano real resolvido.
- `blocked_actions_high`: muitas acoes bloqueadas ou fallback.
- `zero_resolved_damage`: luta sem dano verdadeiro causado.
- `outcome=timeout`: Sukuna nao conseguiu finalizar no tempo.
- `lab_invalid:<reason>`: falha de laboratorio, nao conclusao de IA.

## Utilidade Final

O benchmark serve para transformar ajustes da IA em um ciclo objetivo:

1. Fazer uma mudanca pequena no scoring, movimento, dominio, RCT ou selecao de alvo.
2. Rodar dezenas ou centenas de lutas.
3. Confirmar que o laboratorio foi valido.
4. Ver se win rate, dano resolvido, bloqueios e loops melhoraram.
5. Abrir apenas as janelas ruins quando o agregado indicar regressao.

Isso evita depender de uma luta manual isolada. Uma luta manual ainda e util para observar animacao, pathfinding e comportamento visual, mas o benchmark mede consistencia.

O objetivo final nao e apenas aumentar dano. O benchmark deve mostrar que Sukuna:

- inicia ofensiva cedo;
- causa dano resolvido, nao dano teorico;
- evita loops de movimento sem impacto;
- nao fica preso em skill ativa sem retorno;
- nao usa fallback defensivo sem necessidade;
- vence o alvo antes do timeout;
- mantem o laboratorio limpo e reprodutivel.

## Validacao Recomendada Apos Mudancas

Rodada curta:

```bash
python3 scripts/op_sukuna_benchmark.py --scenario itadori_modulo --fights 40 --duration 1800 --parallel auto
```

Rodada principal:

```bash
python3 scripts/op_sukuna_benchmark.py --scenario itadori_modulo --fights 100 --duration 2400 --parallel auto
```

Analise de um export existente:

```bash
python3 scripts/op_sukuna_benchmark.py --analyze-only run/jjkur-op-ai-metrics/benchmarks/<benchmark_id>
```

Antes de aceitar uma melhora, cheque:

- `invalid_fights=0`;
- `telemetry_quality.valid=true`;
- `real_decision_fights` igual ao numero de lutas;
- `synthetic_decision_fights=0`;
- `missing_decision_fights=0`;
- `contamination_fights=0`;
- queda em `no_impact_loop`;
- queda em `executing_active_skill`;
- aumento de `win_rate_valid`.
