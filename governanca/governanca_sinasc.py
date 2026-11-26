#!/usr/bin/env python3
"""
Governança de Dados | Contexto 4A - Risco de baixo peso ao nascer | SINASC São Paulo (2024)
Trabalho Final Integrado - UNIFSA

Como rodar (raiz do projeto):
    python governanca/governanca_sinasc.py

Entrada : data/raw/DNRE2024.csv     (dado bruto: NÃO versionar)
Saídas  : data/processed/           (arquivo para análise de qualidade, NÃO é o treino: NÃO versionar)
          data/interim/             (listas por registro para revisão: NÃO versionar)
          governanca/saida/         (apenas contagens e tabelas agregadas: versionar)

Política:
- Nada é excluído por suspeita. Valores fora de faixa e inconsistências são sinalizados.
- Excluídos: apenas duplicatas exatas e registros sem PESO (sem alvo).
- Valores fora do domínio do SINASC viram ausentes (registrado no log).
- O treino usa a base bruta com o pipeline de IA (separar_atributos_alvo). O arquivo de
  data/processed/ serve só para análise de qualidade e não entra no treino.
- Não há supressão. O k<5 é aplicado só às tabelas publicadas (arquivo 09).
"""
import json
import os
from pathlib import Path

import numpy as np
import pandas as pd

RAIZ = Path(__file__).resolve().parent.parent
RAW = Path(os.environ.get("SINASC_RAW", RAIZ / "data" / "raw" / "DNRE2024.csv"))
PROC_DIR = RAIZ / "data" / "processed"
INTERIM_DIR = RAIZ / "data" / "interim"
SAIDA = RAIZ / "governanca" / "saida"

ANO_REF = 2024
FAIXA_PESO_REVISAO = (300, 6500)   # gramas: sinaliza, NÃO exclui (suposição a confirmar)
LIMIAR_BAIXO_PESO = 2500           # alvo: PESO < 2500 g (variante A)
K_MIN = 5                          # tamanho mínimo de célula em tabelas publicadas
MAX_CONTAGEM = 40                  # sinalização de qualidade para contagens obstétricas (suposição a confirmar)
MAX_CONTAGEM_API = 15              # validação da API para contagens obstétricas (acordo do grupo)

# Nome do campo na API (front -> back, camelCase). Conferir com frontend/README.md.
NOME_API = {
    "idade_mae_faixa": "faixaEtaria",
    "escolaridade_mae": "escolaridade",
    "estado_civil_mae": "estadoCivil",
    "tipo_gravidez": "tipoGravidez",
    "mes_inicio_prenatal": "mesInicioPrenatal",
    "gestacoes_anteriores": "gestacoesAnteriores",
    "partos_normais_anteriores": "partosNormais",
    "partos_cesareos_anteriores": "partosCesareos",
    "filhos_vivos": "filhosVivos",
    "filhos_mortos": "perdasFetais",
}

# Os 10 atributos do modelo: nome usado pelo pipeline -> coluna original do SINASC
FEATURES = {
    "idade_mae_faixa": "FxEtPartur",
    "escolaridade_mae": "ESCMAE2010",
    "estado_civil_mae": "ESTCIVMAE",
    "tipo_gravidez": "GRAVIDEZ",
    "mes_inicio_prenatal": "MESPRENAT",
    "gestacoes_anteriores": "QTDGESTANT",
    "partos_normais_anteriores": "QTDPARTNOR",
    "partos_cesareos_anteriores": "QTDPARTCES",
    "filhos_vivos": "QTDFILVIVO",
    "filhos_mortos": "QTDFILMORT",
}

IDADE_VALIDAS = ["10–14", "15–19", "20–24", "25–29", "30–34", "35–39",
                 "40–44", "45–49", ">=50", "Ign"]   # formato da base (travessão e "Ign")
IDADE_API = ["10-14", "15-19", "20-24", "25-29", "30-34", "35-39",
             "40-44", "45-49", ">=50"]              # formato da API (hífen, sem "Ign")
# Códigos do SINASC. 9 (ou 99 no mês) = ignorado, que é valor válido.
DOMINIO_CODIGO = {
    "escolaridade_mae": [0, 1, 2, 3, 4, 5, 9],
    "estado_civil_mae": [1, 2, 3, 4, 5, 9],
    "tipo_gravidez": [1, 2, 3, 9],
    "mes_inicio_prenatal": [1, 2, 3, 4, 5, 6, 7, 8, 9],
}
CONTAGENS = ["gestacoes_anteriores", "partos_normais_anteriores",
             "partos_cesareos_anteriores", "filhos_vivos", "filhos_mortos"]

DESCRICAO_FEATURE = {
    "idade_mae_faixa": "Faixa etária da gestante (5 anos)",
    "escolaridade_mae": "Escolaridade da gestante (códigos SINASC)",
    "estado_civil_mae": "Situação conjugal da gestante (códigos SINASC)",
    "tipo_gravidez": "Tipo de gravidez (1 única, 2 dupla, 3 tripla ou mais, 9 ignorado)",
    "mes_inicio_prenatal": "Mês de gestação em que começou o pré-natal (1 a 9; vazio = não iniciado ou não informado)",
    "gestacoes_anteriores": "Número de gestações anteriores",
    "partos_normais_anteriores": "Número de partos normais anteriores",
    "partos_cesareos_anteriores": "Número de partos cesáreos anteriores",
    "filhos_vivos": "Número de filhos vivos",
    "filhos_mortos": "Número de perdas fetais anteriores",
}

# Catálogo: coluna, descrição, tipo, classificação LGPD, confidencialidade, decisão, justificativa.
# As descrições seguem o layout padrão do SINASC: CONFERIR com o dicionário oficial.
CATALOGO = [
    ("LOCNASC", "Local de ocorrência do nascimento", "categórica", "Não pessoal", "Interno", "Removida", "Ocorre no parto; indisponível no momento da decisão (pré-natal)."),
    ("CODMUNNASC", "Município de nascimento (código IBGE)", "categórica", "Não pessoal", "Público", "Removida", "Administrativa; a base já é do município de São Paulo."),
    ("FxEtPartur", "Faixa etária da mãe", "categórica (faixas de 5 anos)", "Dado pessoal", "Interno", "Mantida: idade_mae_faixa", "Mantida no formato original; 'Ign' é código de ignorado."),
    ("RACACORMAE", "Raça/cor da mãe", "categórica", "Dado pessoal sensível (origem racial ou étnica)", "Restrito", "Removida", "Necessidade não demonstrada para a finalidade; risco de discriminação (LGPD, art. 5º II e art. 11). Equidade só em análise agregada, fora do modelo."),
    ("CODUFNATU", "UF de naturalidade da mãe", "categórica", "Dado pessoal", "Interno", "Removida", "Quase-identificador sem ganho preditivo demonstrado (princípio da necessidade)."),
    ("ESTCIVMAE", "Estado civil da mãe", "categórica (1 a 5, 9)", "Dado pessoal", "Interno", "Mantida: estado_civil_mae", "Códigos do SINASC; 9 = ignorado."),
    ("ESCMAE", "Escolaridade da mãe (anos de estudo, versão antiga)", "categórica", "Dado pessoal", "Interno", "Removida", "Redundante com ESCMAE2010 (versão atual)."),
    ("ESCMAE2010", "Escolaridade da mãe (classificação 2010)", "categórica (0 a 5, 9)", "Dado pessoal", "Interno", "Mantida: escolaridade_mae", "Códigos do SINASC; disponível no pré-natal."),
    ("SERIESCMAE", "Série escolar da mãe", "categórica", "Dado pessoal", "Interno", "Removida", "Alta proporção de ausentes e redundante com ESCMAE2010."),
    ("CODOCUPMAE", "Ocupação da mãe (CBO)", "categórica (alta cardinalidade)", "Dado pessoal", "Interno", "Removida", "Alta cardinalidade e potencial quase-identificador; ganho preditivo não demonstrado."),
    ("CODMUNRES", "Município de residência da mãe", "categórica", "Não pessoal", "Público", "Removida", "Valor constante (todos em São Paulo): sem variabilidade."),
    ("CODPAISRES", "País de residência da mãe", "categórica", "Não pessoal", "Público", "Removida", "Valor constante: sem variabilidade."),
    ("QTDGESTANT", "Número de gestações anteriores", "numérica", "Dado pessoal sensível (saúde)", "Confidencial", "Mantida: gestacoes_anteriores", "Mantida como contagem, sem agrupamento."),
    ("QTDPARTNOR", "Partos normais anteriores", "numérica", "Dado pessoal sensível (saúde)", "Confidencial", "Mantida: partos_normais_anteriores", "Mantida separada de QTDPARTCES, como o pipeline espera."),
    ("QTDPARTCES", "Partos cesáreos anteriores", "numérica", "Dado pessoal sensível (saúde)", "Confidencial", "Mantida: partos_cesareos_anteriores", "Mantida separada de QTDPARTNOR."),
    ("QTDFILVIVO", "Número de filhos vivos", "numérica", "Dado pessoal sensível (saúde)", "Confidencial", "Mantida: filhos_vivos", "Mantida como contagem."),
    ("QTDFILMORT", "Número de filhos mortos", "numérica", "Dado pessoal sensível (saúde)", "Confidencial", "Mantida: filhos_mortos", "Mantida como contagem; atributo sensível, usado só no mínimo necessário."),
    ("SEMAGESTAC", "Semanas de gestação ao parto", "numérica", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Só conhecida ao nascimento: vazamento temporal (preditor do próprio fenômeno)."),
    ("TPMETESTIM", "Método de estimativa da idade gestacional", "categórica", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Registrado ao nascimento."),
    ("CONSPRENAT", "Número de consultas de pré-natal", "numérica", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Consolidada só ao fim da gestação (vazamento parcial). Usada apenas na regra de consistência K3."),
    ("MESPRENAT", "Mês de gestação em que iniciou o pré-natal", "categórica (01 a 09, 99)", "Dado pessoal sensível (saúde)", "Confidencial", "Mantida: mes_inicio_prenatal", "Códigos do SINASC; 99 = ignorado; 10 e valores fora do domínio são tratados como ausentes."),
    ("GRAVIDEZ", "Tipo de gravidez (única, dupla, tripla ou mais)", "categórica (1 a 3, 9)", "Dado pessoal sensível (saúde)", "Confidencial", "Mantida: tipo_gravidez", "Gestação múltipla é fator de risco de baixo peso."),
    ("TPAPRESENT", "Apresentação do bebê no parto", "categórica", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Ocorre no parto."),
    ("STTRABPART", "Indução do trabalho de parto", "categórica", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Ocorre no parto."),
    ("PARTO", "Tipo de parto (vaginal ou cesáreo)", "categórica", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Ocorre no parto."),
    ("STCESPARTO", "Cesárea indicada na gestação", "categórica", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Informação do parto."),
    ("TPNASCASSI", "Tipo de assistência ao parto", "categórica", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Ocorre no parto."),
    ("DTNASC2", "Data de nascimento", "data", "Dado pessoal", "Restrito", "Removida", "Quase-identificador (data + idade + ocupação). Usada só para checar o ano de referência."),
    ("SEXO", "Sexo do nascido vivo", "categórica (M, F, I)", "Dado pessoal", "Restrito", "Removida", "Não necessário para a finalidade; refere-se ao recém-nascido."),
    ("APGAR1", "Apgar no 1º minuto", "numérica (0 a 10)", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Avaliado após o nascimento: vazamento temporal."),
    ("APGAR5", "Apgar no 5º minuto", "numérica (0 a 10)", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Avaliado após o nascimento: vazamento temporal."),
    ("PESO", "Peso ao nascer (g)", "numérica", "Dado pessoal sensível (saúde)", "Confidencial", "Removida após gerar o alvo", "Origem do alvo baixo_peso (< 2500 g). Mantê-la seria vazamento direto."),
    ("IDANOMAL", "Anomalia congênita identificada", "categórica", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Diagnosticada ao nascimento."),
    ("FaixaEtPai", "Faixa etária do pai", "categórica", "Dado pessoal", "Interno", "Removida", "Não necessária para a finalidade; alta proporção de ausentes."),
    ("RETROALIM", "Campo de retroalimentação do sistema (conferir no dicionário)", "categórica", "Não pessoal", "Interno", "Removida", "Campo administrativo sem relação com a decisão."),
    ("TPROBSON", "Classificação de Robson", "categórica", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Derivada de variáveis do parto (vazamento)."),
    ("STDNNOVA", "Indicador de declaração nova (conferir no dicionário)", "categórica", "Não pessoal", "Interno", "Removida", "Campo administrativo."),
]


def carregar(caminho):
    for enc in ("utf-8", "latin-1"):
        try:
            df = pd.read_csv(caminho, dtype=str, keep_default_na=False, encoding=enc)
            break
        except UnicodeDecodeError:
            continue
    df.columns = [c.strip() for c in df.columns]
    for c in df.columns:
        df[c] = df[c].str.strip().replace("", np.nan)
    return df


def _num(s):
    return pd.to_numeric(s, errors="coerce")


def checar(df, fase):
    """Regras de qualidade. Retorna uma linha por regra, com a contagem de violações."""
    r = []

    def add(dim, rid, desc, mask):
        if mask is None:
            return
        r.append({"fase": fase, "dimensao": dim, "regra": rid, "descricao": desc,
                  "violacoes": int(mask.sum())})

    def num(c):
        return _num(df[c])

    def fora(c, validos):
        v = num(c)
        return v.notna() & ~v.isin(validos)

    if fase == "antes":
        p = num("PESO")
        lo, hi = FAIXA_PESO_REVISAO
        add("Acurácia", "A1", f"PESO fora de {lo} a {hi} g (sinalizado, não excluído)", (p < lo) | (p > hi))
        add("Completude", "C1", "PESO ausente (sem alvo; excluído)", p.isna())
        s = num("SEMAGESTAC")
        add("Acurácia", "A2", "Semanas de gestação fora de 22 a 44", s.notna() & ((s < 22) | (s > 44)))
        a1, a5 = num("APGAR1"), num("APGAR5")
        add("Acurácia", "A3", "APGAR 1º ou 5º minuto fora de 0 a 10", (a1 < 0) | (a1 > 10) | (a5 < 0) | (a5 > 10))
        add("Acurácia", "A4", "SEXO fora de M, F, I", df["SEXO"].notna() & ~df["SEXO"].isin(["M", "F", "I"]))
        add("Acurácia", "A5", "ESCMAE2010 fora do domínio SINASC", fora("ESCMAE2010", DOMINIO_CODIGO["escolaridade_mae"]))
        add("Acurácia", "A6", "ESTCIVMAE fora do domínio SINASC", fora("ESTCIVMAE", DOMINIO_CODIGO["estado_civil_mae"]))
        add("Acurácia", "A7", "GRAVIDEZ fora do domínio SINASC", fora("GRAVIDEZ", DOMINIO_CODIGO["tipo_gravidez"]))
        add("Acurácia", "A8", "MESPRENAT fora de 1 a 9 da API (inclui 99 e 10)", fora("MESPRENAT", DOMINIO_CODIGO["mes_inicio_prenatal"]))
        for c in CONTAGENS:
            col = FEATURES_ORIGEM[c]
            v = num(col)
            add("Acurácia", "A9", f"{col} acima de {MAX_CONTAGEM} (implausível)", v > MAX_CONTAGEM)
        ano = pd.to_datetime(df["DTNASC2"], errors="coerce").dt.year
        add("Tempestividade", "T1", f"Data de nascimento fora do ano de referência {ANO_REF}", ano.notna() & (ano != ANO_REF))
        partos = num("QTDPARTNOR") + num("QTDPARTCES")
        add("Consistência", "K1", "Partos anteriores maiores que gestações anteriores (sinalizado)", partos > num("QTDGESTANT"))
        filhos = num("QTDFILVIVO") + num("QTDFILMORT")
        add("Consistência", "K2", "Número de filhos menor que partos anteriores (sinalizado)", filhos < partos)
        add("Consistência", "K3", "Zero consultas de pré-natal com mês de início informado",
            (num("CONSPRENAT") == 0) & num("MESPRENAT").between(1, 9))
        add("Consistência", "K4", "Registros duplicados exatos (excluídos)", pd.Series(df.duplicated(), index=df.index))
    else:
        for nome, validos in DOMINIO_CODIGO.items():
            v = df[nome]
            add("Acurácia", "A10", f"Valores fora do domínio após tratamento: {nome}", v.notna() & ~v.isin(validos))
        add("Completude", "C2", "Alvo baixo_peso ausente", df["baixo_peso"].isna())
    return r


FEATURES_ORIGEM = {k: v for k, v in FEATURES.items()}


def transformar(df, log):
    """Seleciona os 10 atributos do modelo, no formato original do SINASC."""
    out = pd.DataFrame(index=df.index)
    idade = df["FxEtPartur"].str.replace("–", "-", regex=False)
    ign = idade.eq("Ign")
    log["idade_ignorada_tratada_como_ausente"] = int(ign.sum())
    out["idade_mae_faixa"] = idade.where(idade.isin(IDADE_API))
    for nome, col in FEATURES.items():
        if nome != "idade_mae_faixa":
            out[nome] = _num(df[col])
    fora = 0
    for nome, validos in DOMINIO_CODIGO.items():
        m = out[nome].notna() & ~out[nome].isin(validos)
        fora += int(m.sum())
        out.loc[m, nome] = np.nan
    for nome in CONTAGENS:
        m = out[nome].notna() & ((out[nome] < 0) | (out[nome] > MAX_CONTAGEM))
        fora += int(m.sum())
        out.loc[m, nome] = np.nan
    log["valores_fora_do_dominio_tratados_como_ausentes"] = fora
    log["mes_99_tratado_como_ausente"] = int((_num(df["MESPRENAT"]) == 99).sum())
    for c in out.columns:
        if c != "idade_mae_faixa":
            out[c] = out[c].astype("Int64")
    return out


def tratar(raw, log, interim):
    log["linhas_entrada"] = len(raw)

    dup = raw.duplicated()
    log["duplicatas_exatas_excluidas"] = int(dup.sum())
    df = raw[~dup]

    sem_peso = _num(df["PESO"]).isna()
    log["sem_peso_excluidos"] = int(sem_peso.sum())
    df = df[~sem_peso]
    peso = _num(df["PESO"])

    lo, hi = FAIXA_PESO_REVISAO
    fora = (peso < lo) | (peso > hi)
    log["peso_fora_faixa_sinalizado_mantido"] = int(fora.sum())
    interim["07_peso_para_revisao.csv"] = pd.DataFrame({
        "linha_csv": df.index[fora] + 2, "PESO_g": peso[fora].values,
        "motivo": f"fora de {lo} a {hi} g"})

    nor, ces, ges = _num(df["QTDPARTNOR"]), _num(df["QTDPARTCES"]), _num(df["QTDGESTANT"])
    k1 = (nor + ces) > ges
    filhos = _num(df["QTDFILVIVO"]) + _num(df["QTDFILMORT"])
    k2 = filhos < (nor + ces)
    sus = k1 | k2
    log["registros_suspeitos_K1"] = int(k1.sum())
    log["registros_suspeitos_K2"] = int(k2.sum())
    log["registros_suspeitos_total_mantidos_e_marcados"] = int(sus.sum())
    motivo = np.where(k1 & k2, "K1 e K2", np.where(k1, "K1", np.where(k2, "K2", "")))
    interim["08_registros_suspeitos.csv"] = pd.DataFrame({
        "linha_csv": df.index[sus] + 2,
        "regra": motivo[sus],
        "PARTOS_NORMAIS_ANT": nor[sus].values,
        "PARTOS_CESAREOS_ANT": ces[sus].values,
        "GESTACOES_ANT": ges[sus].values,
        "FILHOS_VIVOS_MAIS_MORTOS": filhos[sus].values,
    })

    feat = transformar(df, log)
    alvo = (_num(df["PESO"]) < LIMIAR_BAIXO_PESO).astype(int)
    proc = feat.assign(baixo_peso=alvo.values, registro_suspeito=sus.astype(int).values)
    log["linhas_saida_treino"] = len(proc)
    log["prevalencia_baixo_peso_pct"] = round(float(proc["baixo_peso"].mean()) * 100, 2)
    return proc.reset_index(drop=True)


def k_anonimato(proc):
    cols = list(FEATURES.keys())
    tam = proc.groupby(cols, dropna=False)["baixo_peso"].transform("size")
    return {
        "quase_identificadores": cols,
        "k_minimo_no_arquivo_de_treino": int(tam.min()),
        "registros_em_grupos_com_k_menor_que_5": int((tam < K_MIN).sum()),
        "percentual_registros_em_grupos_com_k_menor_que_5": round(float((tam < K_MIN).mean()) * 100, 2),
        "uso": "O arquivo de treino não é publicado nem versionado. Apenas as tabelas do arquivo 09 são publicadas, com células de menos de 5 registros omitidas.",
    }


def tabela_publicavel(proc):
    t = (proc.groupby(["idade_mae_faixa", "tipo_gravidez"], dropna=False)
         .agg(n=("baixo_peso", "size"), casos_baixo_peso=("baixo_peso", "sum"))
         .reset_index())
    t["percentual_baixo_peso"] = (t["casos_baixo_peso"] / t["n"] * 100).round(2)
    pequena = t["n"] < K_MIN
    for c in ["n", "casos_baixo_peso", "percentual_baixo_peso"]:
        t[c] = t[c].astype(object)
        t.loc[pequena, c] = "<5"
    return t


def contrato_api():
    """Contrato entre front, back-end e serviço de IA, conforme acordado com o grupo."""
    categorias = {"escolaridade_mae", "estado_civil_mae", "tipo_gravidez"}
    campos = []
    for nome, col in FEATURES.items():
        if nome == "idade_mae_faixa":
            tipo, valores = "string", IDADE_API
            nulo = "Aceito; tratado como Ignorado"
            obrigatorio = False
        elif nome == "mes_inicio_prenatal":
            tipo, valores = "integer", DOMINIO_CODIGO[nome]
            nulo = "Aceito; significa não iniciado ou não informado"
            obrigatorio = False
        elif nome in categorias:
            tipo, valores = "integer", DOMINIO_CODIGO[nome]
            nulo = "Aceito; tratado como Ignorado"
            obrigatorio = False
        else:
            tipo, valores = "integer", list(range(0, MAX_CONTAGEM_API + 1))
            nulo = "Rejeitado com erro 422"
            obrigatorio = True
        campos.append({
            "nomeFrontBackend": NOME_API[nome],
            "nomeServicoIA": col,
            "descricao": DESCRICAO_FEATURE[nome],
            "tipo": tipo,
            "valoresValidos": valores,
            "obrigatorio": obrigatorio,
            "nulo": nulo,
        })
    return {
        "versao_dados": f"SINASC-SP-{ANO_REF}",
        "alvo": "baixo_peso (1 = PESO < 2500 g), calculado pelo pipeline de IA a partir de PESO",
        "regras_gerais": [
            "Front -> back-end: usar nomeFrontBackend (camelCase).",
            "Back-end -> serviço de IA: usar nomeServicoIA (nomes originais do SINASC).",
            "Código 9 é valor válido só em escolaridade, estadoCivil e tipoGravidez (Ignorado).",
            "Faixa etária é validada com hífen (40-44). O valor desconhecido vai como null.",
            "As 5 contagens são obrigatórias, de 0 a 15. null gera erro 422.",
            "Regra entre campos: partosNormais + partosCesareos <= gestacoesAnteriores.",
            f"Sinalização de qualidade (fora da API) usa o limite de {MAX_CONTAGEM}.",
        ],
        "campos": campos,
    }


def catalogo_df(raw):
    linhas = []
    for col, desc, tipo, lgpd, conf, decisao, just in CATALOGO:
        pct = round(float(raw[col].isna().mean()) * 100, 2) if col in raw.columns else None
        linhas.append({"coluna": col, "descricao": desc, "tipo": tipo,
                       "origem": "SINASC (DataSUS/Prefeitura SP)",
                       "percentual_faltante": pct, "classificacao_lgpd": lgpd,
                       "confidencialidade": conf, "decisao": decisao, "justificativa": just,
                       "responsavel": "Data Steward (equipe de dados)",
                       "status_revisao": "Pendente: conferir descrição no dicionário oficial"})
    return pd.DataFrame(linhas)


def completude(raw, proc):
    linhas = []
    for nome, origem in FEATURES.items():
        linhas.append({"variavel": nome, "origem": origem,
                       "faltante_antes_pct": round(float(raw[origem].isna().mean()) * 100, 2),
                       "faltante_depois_pct": round(float(proc[nome].isna().mean()) * 100, 2)})
    return pd.DataFrame(linhas)


def main():
    for d in (SAIDA, PROC_DIR, INTERIM_DIR):
        d.mkdir(parents=True, exist_ok=True)

    raw = carregar(RAW)
    print(f"Lidas {len(raw)} linhas e {raw.shape[1]} colunas de {RAW.name}")

    antes = checar(raw, "antes")
    log, interim = {}, {}
    proc = tratar(raw, log, interim)
    depois = checar(proc, "depois")
    q = pd.DataFrame(antes + depois)
    comp = completude(raw, proc)
    kanon = k_anonimato(proc)
    pub = tabela_publicavel(proc)

    cat = catalogo_df(raw)
    cat.to_csv(SAIDA / "01_catalogo_dados.csv", index=False, encoding="utf-8-sig")
    q.to_csv(SAIDA / "02_qualidade_antes_depois.csv", index=False, encoding="utf-8-sig")
    comp.to_csv(SAIDA / "03_completude_variaveis.csv", index=False, encoding="utf-8-sig")
    (SAIDA / "04_log_tratamento.json").write_text(json.dumps(log, ensure_ascii=False, indent=2), encoding="utf-8")
    (SAIDA / "05_k_anonimato.json").write_text(json.dumps(kanon, ensure_ascii=False, indent=2), encoding="utf-8")
    (SAIDA / "06_contrato_entrada_api.json").write_text(json.dumps(contrato_api(), ensure_ascii=False, indent=2), encoding="utf-8")
    pub.to_csv(SAIDA / "09_tabela_publicavel_k5.csv", index=False, encoding="utf-8-sig")

    for nome, tabela in interim.items():
        tabela.to_csv(INTERIM_DIR / nome, index=False, encoding="utf-8-sig")
    proc.to_csv(PROC_DIR / "analise_qualidade_2024.csv", index=False, encoding="utf-8")

    print("\nTratamento:")
    for k, v in log.items():
        print(f"  {k}: {v}")
    print(f"\nk mínimo no arquivo de treino: {kanon['k_minimo_no_arquivo_de_treino']}")
    print(f"Saídas agregadas: {SAIDA}")
    print(f"Listas por registro (não versionar): {INTERIM_DIR}")
    print(f"Arquivo de análise de qualidade (não versionar): {PROC_DIR}")


if __name__ == "__main__":
    main()
