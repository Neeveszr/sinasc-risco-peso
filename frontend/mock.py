"""Respostas fictícias para o modo demonstração (API_MOCK=1).

Nada aqui é o modelo real: as regras abaixo só imitam o formato da resposta
para desenvolver e testar as telas. A tela mostra um aviso de demonstração.
"""

import random
from datetime import datetime, timedelta, timezone
from zoneinfo import ZoneInfo

VERSAO_DEMO = "demo"

_USUARIOS = {
    "operador": {"id": 2, "nome": "Operador Demo", "perfil": "OPERADOR"},
    "gestor": {"id": 1, "nome": "Gestor Demo", "perfil": "GESTOR_AUDITOR"},
}

_FUSO = ZoneInfo("America/Sao_Paulo")

_historico = []
_proximo_id = 1
_populado = False


def _erro(status, mensagem):
    from api import ApiErro  # import aqui dentro: api.py importa este módulo

    return ApiErro(status, mensagem)


def usuario_do_token(token):
    """No modo demo o token é só 'demo-operador' ou 'demo-gestor'."""
    return _USUARIOS["gestor"] if token == "demo-gestor" else _USUARIOS["operador"]


def login(email, senha):
    if not email or not senha:
        raise _erro(401, "E-mail ou senha inválidos")
    chave = "gestor" if email.strip().lower().startswith("gestor") else "operador"
    usuario = dict(_USUARIOS[chave], email=email.strip().lower(), ativo=True)
    return {"token": f"demo-{chave}", "tipo": "Bearer", "expiraEmMinutos": 120, "usuario": usuario}


def listar_usuarios():
    return sorted(_USUARIOS.values(), key=lambda u: u["nome"])


def _faixa(probabilidade):
    # Limiares provisórios de demonstração; os reais serão definidos pelo JP.
    if probabilidade < 0.15:
        return "BAIXO"
    if probabilidade < 0.30:
        return "MEDIO"
    return "ALTO"


def _calcular(dados):
    contribuicoes = []

    gravidez = dados.get("tipoGravidez")
    if gravidez == 2:
        contribuicoes.append(("tipoGravidez", "Gravidez dupla (gêmeos)", 0.50))
    elif gravidez == 3:
        contribuicoes.append(("tipoGravidez", "Gravidez tripla ou mais", 0.85))

    mes = dados.get("mesInicioPrenatal")
    if mes is None:
        contribuicoes.append(("mesInicioPrenatal", "Pré-natal ainda não iniciado", 0.17))
    elif mes >= 4:
        contribuicoes.append(("mesInicioPrenatal", f"Pré-natal iniciado no {mes}º mês", 0.05))
    else:
        contribuicoes.append(("mesInicioPrenatal", f"Pré-natal iniciado cedo ({mes}º mês)", -0.02))

    faixa = dados.get("faixaEtaria")
    if faixa in ("10-14", "15-19"):
        contribuicoes.append(("faixaEtaria", f"Gestante com {faixa.replace('-', ' a ')} anos", 0.04))
    elif faixa in ("40-44", "45-49", ">=50"):
        contribuicoes.append(("faixaEtaria", "Gestante com 40 anos ou mais", 0.05))

    if (dados.get("perdasFetais") or 0) >= 1:
        contribuicoes.append(("perdasFetais", "Perda fetal anterior", 0.03))

    probabilidade = min(0.99, max(0.01, 0.08 + sum(c[2] for c in contribuicoes)))
    contribuicoes.sort(key=lambda c: abs(c[2]), reverse=True)
    fatores = [
        {"atributo": a, "descricao": d, "efeito": "AUMENTA" if v > 0 else "REDUZ", "impacto": round(abs(v), 3)}
        for a, d, v in contribuicoes[:3]
    ]
    return round(probabilidade, 3), fatores


def criar_predicao(dados, usuario=None):
    global _proximo_id
    probabilidade, fatores = _calcular(dados)
    usuario = usuario or _USUARIOS["operador"]
    predicao = {
        "id": _proximo_id,
        "probabilidade": probabilidade,
        "faixaRisco": _faixa(probabilidade),
        "fatores": fatores,
        "versaoModelo": VERSAO_DEMO,
        "criadoEm": datetime.now(timezone.utc).isoformat(),
        "usuarioId": usuario["id"],
        "usuarioNome": usuario["nome"],
        "revisaoSolicitada": False,
    }
    _proximo_id += 1
    _historico.append(predicao)
    return predicao


def solicitar_revisao(predicao_id, motivo):
    for predicao in _historico:
        if predicao["id"] == predicao_id:
            predicao["revisaoSolicitada"] = True
            return {"predicaoId": predicao_id, "status": "PENDENTE", "motivo": motivo}
    raise _erro(404, f"Predição {predicao_id} não encontrada")


def _popular():
    """Histórico inicial fictício, sempre igual (semente fixa)."""
    gerador = random.Random(42)
    agora = datetime.now(timezone.utc)
    for _ in range(40):
        dados = {
            "tipoGravidez": gerador.choices([1, 2, 3], weights=[90, 9, 1])[0],
            "mesInicioPrenatal": gerador.choice([1, 2, 2, 3, 4, 6, None]),
            "faixaEtaria": gerador.choice(["15-19", "20-24", "25-29", "30-34", "35-39", "40-44"]),
            "perdasFetais": gerador.choice([0, 0, 0, 1]),
        }
        usuario = gerador.choice(list(_USUARIOS.values()))
        predicao = criar_predicao(dados, usuario)
        predicao["criadoEm"] = (agora - timedelta(days=gerador.randint(0, 30), minutes=gerador.randint(0, 1440))).isoformat()
        predicao["revisaoSolicitada"] = gerador.random() < 0.15


def _dia_local(instante_iso):
    """Data no horário de Brasília: é assim que o usuário escolhe o período."""
    return datetime.fromisoformat(instante_iso).astimezone(_FUSO).date().isoformat()


def listar_predicoes(params):
    global _populado
    if not _populado:
        _popular()
        _populado = True

    def manter(p):
        dia = _dia_local(p["criadoEm"])
        return (
            ("inicio" not in params or dia >= params["inicio"])
            and ("fim" not in params or dia <= params["fim"])
            and ("usuarioId" not in params or p["usuarioId"] == params["usuarioId"])
            and ("faixa" not in params or p["faixaRisco"] == params["faixa"])
        )

    itens = sorted(filter(manter, _historico), key=lambda p: p["criadoEm"], reverse=True)
    pagina, tamanho = params.get("page", 0), params.get("size", 50)
    conteudo = itens[pagina * tamanho:(pagina + 1) * tamanho]
    return {"content": conteudo, "totalElements": len(itens), "number": pagina, "size": tamanho}
