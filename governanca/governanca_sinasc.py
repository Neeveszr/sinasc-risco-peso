#!/usr/bin/env python3
"""
Governança de Dados | Contexto 4A - Risco de baixo peso ao nascer | SINASC São Paulo
Trabalho Final Integrado - UNIFSA

Como rodar (raiz do projeto):
    python governanca/governanca_sinasc.py

Entrada : data/raw/DNRE2014.csv   (dado bruto: NÃO versionar)
Saídas  : governanca/saida/       (relatórios agregados e listas de revisão)
          data/processed/         (dado tratado: NÃO versionar)

Política de tratamento (versão 2):
- Nada é excluído por suspeita. Valores fora de faixa e inconsistências são SINALIZADOS
  em listas de revisão (07 e 08) até a confirmação com o dicionário oficial.
- Excluídos: apenas duplicatas exatas e registros sem PESO (alvo indefinido).
- Supressão: registros em grupos de quase-identificadores com menos de 5 pessoas são
  removidos do arquivo tratado (risco de reidentificação), com contagem no log.
"""
import json
import os
from pathlib import Path

import numpy as np
import pandas as pd

RAIZ = Path(__file__).resolve().parent.parent
RAW = Path(os.environ.get("SINASC_RAW", RAIZ / "data" / "raw" / "DNRE2014.csv"))
PROC_DIR = RAIZ / "data" / "processed"
SAIDA = RAIZ / "governanca" / "saida"

ANO_REF = 2014
FAIXA_PESO_REVISAO = (300, 6500)   # gramas: sinaliza para revisão, NÃO exclui
LIMIAR_BAIXO_PESO = 2500           # alvo: PESO < 2500 g (variante A)
K_MIN = 5                          # tamanho mínimo de grupo de quase-identificadores
LIMITE_FALTANTE = 0.05

IDADE_VALIDAS = ["10–14", "15–19", "20–24", "25–29", "30–34", "35–39",
                 "40–44", "45–49", ">=50"]

# Variáveis do modelo: nome tratado -> origem
FEATURES = {
    "idade_mae_faixa": "FxEtPartur",
    "escolaridade_mae": "ESCMAE2010",
    "estado_civil_mae": "ESTCIVMAE",
    "tipo_gravidez": "GRAVIDEZ",
    "trimestre_inicio_prenatal": "MESPRENAT",
    "gestacoes_anteriores": "QTDGESTANT",
    "partos_anteriores": "QTDPARTNOR + QTDPARTCES",
    "filhos_vivos": "QTDFILVIVO",
    "filhos_mortos": "QTDFILMORT",
}

# Categorias finais (já generalizadas). É o contrato de entrada da API.
CATEGORIAS = {
    "idade_mae_faixa": ["10–19", "20–34", "35+"],
    "escolaridade_mae": ["baixa", "media", "alta"],
    "estado_civil_mae": ["com_companheiro", "sem_companheiro"],
    "tipo_gravidez": ["unica", "dupla", "tripla_ou_mais"],
    "trimestre_inicio_prenatal": ["1", "2", "3"],
    "gestacoes_anteriores": ["0", "1", "2+"],
    "partos_anteriores": ["0", "1", "2+"],
    "filhos_vivos": ["0", "1", "2+"],
    "filhos_mortos": ["0", "1+"],
}
DOMINIO = {k: set(v) for k, v in CATEGORIAS.items()}

# Como cada campo de entrada da API é tratado quando o usuário informa "ignorado"
TRATAMENTO_AUSENTE = ("Valor 'ignorado' vira categoria própria no pipeline "
                      "(SimpleImputer strategy='constant', fill_value='ignorado'). "
                      "A definição final é da equipe de IA.")

CATALOGO = [
    ("LOCNASC", "Local de ocorrência do nascimento", "categórica", "Não pessoal", "Interno", "Removida", "Ocorre no parto; indisponível no momento da decisão (pré-natal)."),
    ("CODMUNNASC", "Município de nascimento (código IBGE)", "categórica", "Não pessoal", "Público", "Removida", "Administrativa; a base já é do município de São Paulo."),
    ("FxEtPartur", "Faixa etária da mãe", "categórica (faixas de 5 anos)", "Dado pessoal", "Interno", "Mantida: idade_mae_faixa", "Já agrupada em faixas; idade é fator de risco gestacional."),
    ("RACACORMAE", "Raça/cor da mãe", "categórica", "Dado pessoal sensível (origem racial ou étnica)", "Restrito", "Removida", "Necessidade não demonstrada para a finalidade; risco de discriminação (LGPD, art. 5º II e art. 11). Equidade só em análise agregada."),
    ("CODUFNATU", "UF de naturalidade da mãe", "categórica", "Dado pessoal", "Interno", "Removida", "Quase-identificador com baixo ganho preditivo (princípio da necessidade)."),
    ("ESTCIVMAE", "Estado civil da mãe", "categórica (1 a 5, 9)", "Dado pessoal", "Interno", "Mantida: estado_civil_mae", "Indicador de suporte durante a gestação; não identifica diretamente."),
    ("ESCMAE", "Escolaridade da mãe (anos de estudo, versão antiga)", "categórica", "Dado pessoal", "Interno", "Removida", "Redundante com ESCMAE2010 (versão atual)."),
    ("ESCMAE2010", "Escolaridade da mãe (classificação 2010)", "categórica (0 a 5, 9)", "Dado pessoal", "Interno", "Mantida: escolaridade_mae", "Indicador socioeconômico de risco; disponível no pré-natal."),
    ("SERIESCMAE", "Série escolar da mãe", "categórica", "Dado pessoal", "Interno", "Removida", "Cerca de 35% ausente e redundante com ESCMAE2010."),
    ("CODOCUPMAE", "Ocupação da mãe (CBO)", "categórica (alta cardinalidade)", "Dado pessoal", "Interno", "Removida", "Alta cardinalidade e potencial quase-identificador; ganho preditivo baixo."),
    ("CODMUNRES", "Município de residência da mãe", "categórica", "Não pessoal", "Público", "Removida", "Valor constante (todos em São Paulo): sem variabilidade."),
    ("CODPAISRES", "País de residência da mãe", "categórica", "Não pessoal", "Público", "Removida", "Valor constante: sem variabilidade."),
    ("QTDGESTANT", "Número de gestações anteriores", "numérica", "Dado pessoal sensível (saúde)", "Confidencial", "Mantida: gestacoes_anteriores", "Antecedente obstétrico; agrupado em 0, 1, 2 e 3+ (generalização)."),
    ("QTDPARTNOR", "Partos normais anteriores", "numérica", "Dado pessoal sensível (saúde)", "Confidencial", "Mantida: partos_anteriores", "Somado a QTDPARTCES e agrupado em 0, 1 e 2+."),
    ("QTDPARTCES", "Partos cesáreos anteriores", "numérica", "Dado pessoal sensível (saúde)", "Confidencial", "Mantida: partos_anteriores", "Ver QTDPARTNOR."),
    ("QTDFILVIVO", "Número de filhos vivos", "numérica", "Dado pessoal sensível (saúde)", "Confidencial", "Mantida: filhos_vivos", "Agrupado em 0, 1 e 2+."),
    ("QTDFILMORT", "Número de filhos mortos", "numérica", "Dado pessoal sensível (saúde)", "Confidencial", "Mantida: filhos_mortos", "Agrupado em 0 e 1+ (mínimo necessário)."),
    ("SEMAGESTAC", "Semanas de gestação ao parto", "numérica", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Só conhecida ao nascimento: vazamento temporal (preditor forte do próprio fenômeno)."),
    ("TPMETESTIM", "Método de estimativa da idade gestacional", "categórica", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Registrado ao nascimento; cerca de 45% ausente."),
    ("CONSPRENAT", "Número de consultas de pré-natal", "numérica", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Consolidada só ao fim da gestação (vazamento parcial). Usada apenas na regra de consistência K3."),
    ("MESPRENAT", "Mês de gestação em que iniciou o pré-natal", "categórica (01 a 09, 99)", "Dado pessoal sensível (saúde)", "Confidencial", "Mantida: trimestre_inicio_prenatal", "Convertida em trimestre (1º, 2º, 3º). Códigos 10 e 99 tratados como ausentes."),
    ("GRAVIDEZ", "Tipo de gravidez (única, dupla, tripla ou mais)", "categórica (1 a 3, 9)", "Dado pessoal sensível (saúde)", "Confidencial", "Mantida: tipo_gravidez", "Gestação múltipla é fator de risco de baixo peso."),
    ("TPAPRESENT", "Apresentação do bebê no parto", "categórica", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Ocorre no parto."),
    ("STTRABPART", "Indução do trabalho de parto", "categórica", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Ocorre no parto."),
    ("PARTO", "Tipo de parto (vaginal ou cesáreo)", "categórica", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Ocorre no parto."),
    ("STCESPARTO", "Cesárea indicada na gestação", "categórica", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Cerca de 46% ausente; informação do parto."),
    ("TPNASCASSI", "Tipo de assistência ao parto", "categórica", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Ocorre no parto."),
    ("DTNASC2", "Data de nascimento", "data", "Dado pessoal", "Restrito", "Removida", "Quase-identificador (data + idade + ocupação). Usada só para checar o ano de referência."),
    ("SEXO", "Sexo do nascido vivo", "categórica (M, F, I)", "Dado pessoal", "Restrito", "Removida", "Não necessário para a finalidade; refere-se ao recém-nascido."),
    ("APGAR1", "Apgar no 1º minuto", "numérica (0 a 10)", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Avaliado após o nascimento: vazamento temporal."),
    ("APGAR5", "Apgar no 5º minuto", "numérica (0 a 10)", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Avaliado após o nascimento: vazamento temporal."),
    ("PESO", "Peso ao nascer (g)", "numérica", "Dado pessoal sensível (saúde)", "Confidencial", "Removida após gerar o alvo", "Origem do alvo baixo_peso (< 2500 g). Mantê-la seria vazamento direto."),
    ("IDANOMAL", "Anomalia congênita identificada", "categórica", "Dado pessoal sensível (saúde)", "Confidencial", "Removida", "Diagnosticada ao nascimento."),
    ("FaixaEtPai", "Faixa etária do pai", "categórica", "Dado pessoal", "Interno", "Removida", "Cerca de 59% ausente; não necessária para a finalidade."),
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
    """Regras de qualidade. Retorna lista de dicionários (uma linha por regra)."""
    r = []

    def add(dim, rid, desc, mask):
        if mask is None:
            return
        r.append({"fase": fase, "dimensao": dim, "regra": rid, "descricao": desc,
                  "violacoes": int(mask.sum())})

    def num(c):
        return _num(df[c]) if c in df.columns else None

    def cat(c, validos):
        if c not in df.columns:
            return None
        return df[c].notna() & ~df[c].isin(validos)

    if fase == "antes":
        p = num("PESO")
        lo, hi = FAIXA_PESO_REVISAO
        add("Acurácia", "A1", f"PESO fora de {lo} a {hi} g (sinalizado para revisão, não excluído)",
            (p < lo) | (p > hi))
        add("Completude", "C1", "PESO ausente (alvo sem valor; registro excluído)", p.isna())
        s = num("SEMAGESTAC")
        add("Acurácia", "A2", "Semanas de gestação fora de 22 a 44", s.notna() & ((s < 22) | (s > 44)))
        a1, a5 = num("APGAR1"), num("APGAR5")
        add("Acurácia", "A3", "APGAR 1º ou 5º minuto fora de 0 a 10", (a1 < 0) | (a1 > 10) | (a5 < 0) | (a5 > 10))
        add("Acurácia", "A4", "SEXO fora de M, F, I", cat("SEXO", {"M", "F", "I"}))
        add("Acurácia", "A5", "ESCMAE2010 fora de 0 a 5 ou 9", cat("ESCMAE2010", {"0", "1", "2", "3", "4", "5", "9"}))
        add("Acurácia", "A6", "MESPRENAT fora de 01 a 09 ou 99", cat("MESPRENAT", {"01", "02", "03", "04", "05", "06", "07", "08", "09", "99"}))
        add("Acurácia", "A7", "ESTCIVMAE fora de 1 a 5 ou 9", cat("ESTCIVMAE", {"1", "2", "3", "4", "5", "9"}))
        ano = pd.to_datetime(df["DTNASC2"], errors="coerce").dt.year
        add("Tempestividade", "T1", f"Data de nascimento fora do ano de referência {ANO_REF}", ano.notna() & (ano != ANO_REF))
        partos = num("QTDPARTNOR") + num("QTDPARTCES")
        add("Consistência", "K1", "Partos anteriores maiores que gestações anteriores (sinalizado)", partos > num("QTDGESTANT"))
        filhos = num("QTDFILVIVO") + num("QTDFILMORT")
        add("Consistência", "K2", "Número de filhos menor que partos anteriores (sinalizado)", filhos < partos)
        mes = num("MESPRENAT")
        add("Consistência", "K3", "Zero consultas de pré-natal com mês de início informado", (num("CONSPRENAT") == 0) & mes.between(1, 9))
        add("Consistência", "K4", "Registros duplicados exatos (excluídos)", pd.Series(df.duplicated(), index=df.index))
    else:
        for nome, dom in DOMINIO.items():
            v = df[nome]
            add("Acurácia", "A8", f"Valores fora das categorias: {nome}", v.notna() & ~v.isin(dom))
        add("Completude", "C1", "Alvo baixo_peso ausente", df["baixo_peso"].isna())
    return r


def transformar(df):
    """Generalização e seleção de atributos para o modelo. Saída em categorias textuais."""
    out = pd.DataFrame(index=df.index)
    f = df["FxEtPartur"]
    out["idade_mae_faixa"] = pd.Series(np.select(
        [f.isin(["10–14", "15–19"]), f.isin(["20–24", "25–29", "30–34"]), f.isin(["35–39", "40–44", "45–49", ">=50"])],
        ["10–19", "20–34", "35+"], default=None), index=df.index)
    out["escolaridade_mae"] = _num(df["ESCMAE2010"]).map({0: "baixa", 1: "baixa", 2: "baixa",
                                                         3: "media", 4: "alta", 5: "alta"})
    out["estado_civil_mae"] = _num(df["ESTCIVMAE"]).map({1: "sem_companheiro", 2: "com_companheiro",
                                                         3: "sem_companheiro", 4: "sem_companheiro",
                                                         5: "com_companheiro"})
    out["tipo_gravidez"] = _num(df["GRAVIDEZ"]).map({1: "unica", 2: "dupla", 3: "tripla_ou_mais"})
    out["trimestre_inicio_prenatal"] = _num(df["MESPRENAT"]).map(
        {m: str((m - 1) // 3 + 1) for m in range(1, 10)})
    out["gestacoes_anteriores"] = _num(df["QTDGESTANT"]).clip(upper=2).map({0: "0", 1: "1", 2: "2+"})
    nor, ces = _num(df["QTDPARTNOR"]), _num(df["QTDPARTCES"])
    out["partos_anteriores"] = (nor + ces).clip(upper=2).map({0: "0", 1: "1", 2: "2+"})
    out["filhos_vivos"] = _num(df["QTDFILVIVO"]).clip(upper=2).map({0: "0", 1: "1", 2: "2+"})
    out["filhos_mortos"] = _num(df["QTDFILMORT"]).clip(upper=1).map({0: "0", 1: "1+"})
    return out


def tratar(raw, log, saida):
    df = raw.copy()
    log["linhas_entrada"] = len(df)

    dup = df.duplicated()
    log["duplicatas_exatas_excluidas"] = int(dup.sum())
    df = df[~dup]

    peso = _num(df["PESO"])
    sem_peso = peso.isna()
    log["sem_peso_excluidos"] = int(sem_peso.sum())
    df = df[~sem_peso]
    peso = _num(df["PESO"])

    # Sinalizar (sem excluir): peso fora da faixa de plausibilidade
    lo, hi = FAIXA_PESO_REVISAO
    fora = (peso < lo) | (peso > hi)
    log["peso_fora_faixa_sinalizado_mantido"] = int(fora.sum())
    saida["07_peso_para_revisao.csv"] = pd.DataFrame({
        "linha_csv": df.index[fora] + 2, "PESO_g": peso[fora].values,
        "motivo": f"fora de {lo} a {hi} g"})

    # Sinalizar (sem excluir): inconsistências K1 e K2
    nor, ces, ges = _num(df["QTDPARTNOR"]), _num(df["QTDPARTCES"]), _num(df["QTDGESTANT"])
    k1 = (nor + ces) > ges
    filhos = _num(df["QTDFILVIVO"]) + _num(df["QTDFILMORT"])
    k2 = filhos < (nor + ces)
    suspeito = k1 | k2
    log["registros_suspeitos_K1"] = int(k1.sum())
    log["registros_suspeitos_K2"] = int(k2.sum())
    log["registros_suspeitos_unicos_antes_da_supressao"] = int(suspeito.sum())
    motivo = np.where(k1 & k2, "K1 e K2", np.where(k1, "K1", np.where(k2, "K2", "")))
    saida["08_registros_suspeitos.csv"] = pd.DataFrame({
        "linha_csv": df.index[suspeito] + 2,
        "regra": motivo[suspeito],
        "PARTOS_ANT": (nor + ces)[suspeito].values,
        "GESTACOES_ANT": ges[suspeito].values,
        "FILHOS_VIVOS_MAIS_MORTOS": filhos[suspeito].values,
    })

    feat = transformar(df)
    alvo = (_num(df["PESO"]) < LIMIAR_BAIXO_PESO).astype(int)
    proc = feat.assign(baixo_peso=alvo, registro_suspeito=suspeito.astype(int))

    # Supressão de grupos pequenos (k < 5) para reduzir reidentificação
    tam = proc.groupby(list(FEATURES.keys()), dropna=False)["baixo_peso"].transform("size")
    suprimir = tam < K_MIN
    log["registros_suprimidos_grupo_menor_que_5"] = int(suprimir.sum())
    log["suprimidos_com_ausencia_em_algum_campo"] = int((suprimir & proc[list(FEATURES.keys())].isna().any(axis=1)).sum())
    proc = proc[~suprimir]
    log["registros_suspeitos_restantes_no_arquivo_tratado"] = int(proc["registro_suspeito"].sum())

    log["linhas_saida"] = len(proc)
    log["prevalencia_baixo_peso_pct"] = round(float(proc["baixo_peso"].mean()) * 100, 2)
    return proc.reset_index(drop=True)


def k_anonimato(proc):
    tam = proc.groupby(list(FEATURES.keys()), dropna=False)["baixo_peso"].transform("size")
    return {
        "quase_identificadores": list(FEATURES.keys()),
        "k_minimo_no_arquivo_tratado": int(tam.min()),
        "grupos_distintos": int(proc.groupby(list(FEATURES.keys()), dropna=False).ngroups),
        "regra_aplicada": "Registros em grupos com menos de 5 pessoas foram suprimidos do arquivo tratado.",
        "regra_de_divulgacao": "Tabelas agregadas publicadas devem omitir células com menos de 5 registros.",
    }


def contrato_api():
    descr = {
        "idade_mae_faixa": "Faixa etária da gestante",
        "escolaridade_mae": "Escolaridade da gestante",
        "estado_civil_mae": "Situação conjugal da gestante",
        "tipo_gravidez": "Tipo de gravidez",
        "trimestre_inicio_prenatal": "Trimestre em que o pré-natal começou",
        "gestacoes_anteriores": "Gestações anteriores",
        "partos_anteriores": "Partos anteriores (normais e cesáreos somados)",
        "filhos_vivos": "Filhos vivos",
        "filhos_mortos": "Filhos mortos",
    }
    campos = []
    for nome, valores in CATEGORIAS.items():
        campos.append({
            "campo": nome,
            "descricao": descr[nome],
            "tipo": "string",
            "valores_validos": valores,
            "aceita_ignorado": True,
            "obrigatorio": True,
            "tratamento_quando_ignorado": TRATAMENTO_AUSENTE,
        })
    return {"versao_dados": f"SINASC-SP-{ANO_REF}", "alvo": "baixo_peso (1 = PESO < 2500 g)",
            "observacao": "Campo 'obrigatorio' significa que o formulário sempre envia o campo; "
                          "'ignorado' é um valor válido. Alinhar com a equipe de IA antes da entrega.",
            "campos": campos}


def catalogo_df(raw):
    linhas = []
    for col, desc, tipo, lgpd, conf, decisao, just in CATALOGO:
        pct = round(float(raw[col].isna().mean()) * 100, 2) if col in raw.columns else None
        linhas.append({"coluna": col, "descricao": desc, "tipo": tipo, "origem": "SINASC (DataSUS/Prefeitura SP)",
                       "percentual_faltante": pct, "classificacao_lgpd": lgpd, "confidencialidade": conf,
                       "decisao": decisao, "justificativa": just, "responsavel": "Data Steward (equipe de dados)",
                       "status_revisao": "Pendente: conferir descrição no dicionário oficial"})
    return pd.DataFrame(linhas)


def completude(raw, proc):
    linhas = []
    for nome, origem in FEATURES.items():
        if nome == "partos_anteriores":
            pct_raw = round(float((raw["QTDPARTNOR"].isna() | raw["QTDPARTCES"].isna()).mean()) * 100, 2)
        else:
            pct_raw = round(float(raw[origem].isna().mean()) * 100, 2)
        pct_proc = round(float(proc[nome].isna().mean()) * 100, 2)
        linhas.append({"variavel": nome, "origem": origem, "faltante_antes_pct": pct_raw,
                       "faltante_depois_pct": pct_proc, "alerta": pct_proc > LIMITE_FALTANTE * 100})
    return pd.DataFrame(linhas)


def main():
    SAIDA.mkdir(parents=True, exist_ok=True)
    PROC_DIR.mkdir(parents=True, exist_ok=True)

    raw = carregar(RAW)
    print(f"Lidas {len(raw)} linhas e {raw.shape[1]} colunas de {RAW.name}")

    antes = checar(raw, "antes")
    log, extras = {}, {}
    proc = tratar(raw, log, extras)
    depois = checar(proc, "depois")
    q = pd.DataFrame(antes + depois)
    comp = completude(raw, proc)
    kanon = k_anonimato(proc)
    cat = catalogo_df(raw)

    cat.to_csv(SAIDA / "01_catalogo_dados.csv", index=False, encoding="utf-8-sig")
    q.to_csv(SAIDA / "02_qualidade_antes_depois.csv", index=False, encoding="utf-8-sig")
    comp.to_csv(SAIDA / "03_completude_variaveis.csv", index=False, encoding="utf-8-sig")
    (SAIDA / "04_log_tratamento.json").write_text(json.dumps(log, ensure_ascii=False, indent=2), encoding="utf-8")
    (SAIDA / "05_k_anonimato.json").write_text(json.dumps(kanon, ensure_ascii=False, indent=2), encoding="utf-8")
    (SAIDA / "06_contrato_entrada_api.json").write_text(json.dumps(contrato_api(), ensure_ascii=False, indent=2), encoding="utf-8")
    for nome, df_extra in extras.items():
        df_extra.to_csv(SAIDA / nome, index=False, encoding="utf-8-sig")
    proc.to_csv(PROC_DIR / "sinasc_governado.csv", index=False, encoding="utf-8")

    print("\nTratamento:")
    for k, v in log.items():
        print(f"  {k}: {v}")
    print(f"\nk mínimo após supressão: {kanon['k_minimo_no_arquivo_tratado']}")
    print(f"Saídas em: {SAIDA}")


if __name__ == "__main__":
    main()
