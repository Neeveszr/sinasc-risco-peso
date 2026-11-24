# Como contribuir

O histórico de commits de **cada integrante** entra na nota da entrevista. Todos devem commitar na sua própria frente.

## Fluxo

1. `git checkout main` e `git pull origin main` antes de começar.
2. Criar uma branch por tarefa a partir da `main`: `feature/<frente>-<assunto>`, por exemplo `feature/backend-jwt` ou `feature/ml-pipeline`.
3. Commits pequenos, com mensagem no imperativo e prefixo da frente:
   - `ml: adiciona remoção de colunas com vazamento`
   - `backend: implementa login com JWT`
   - `governanca: classifica atributos pela LGPD`
   - `front: cria tela de resultado`
   - `docs: preenche model card`
4. Dar push da branch e abrir Pull Request para `main`, com título no mesmo padrão dos commits e uma frase dizendo o que mudou.
5. O Antonio revisa e aprova. Só depois o PR é mesclado (merge) na `main`. **Ninguém dá push direto na `main`.**
6. Depois do merge, apagar a branch e voltar ao passo 1.

Cada PR deve tratar de **uma tarefa só** e mexer na pasta da própria frente. Se precisar alterar a pasta de outra pessoa, avise antes.

## Regras

- **Nunca** commitar dados brutos (CSV, XLSX, DBF). O `.gitignore` bloqueia, mas confira com `git status`.
- **Nunca** commitar senhas, chaves ou `.env`.
- Toda biblioteca, base ou código de terceiros entra nas referências do README.
