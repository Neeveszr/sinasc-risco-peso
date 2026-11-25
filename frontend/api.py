"""Cliente da API Spring Boot.

Todas as chamadas do front passam por aqui. Em modo demonstração (API_MOCK=1),
as funções devolvem respostas fictícias de mock.py com o mesmo formato.

Rotas usadas (as de predição são a proposta de contrato descrita no README):
- POST /api/auth/login                 -> pronta (Rodolfo)
- GET  /api/usuarios                   -> pronta (Rodolfo), filtro da Auditoria
- POST /api/predicoes                  -> a implementar (Carlos)
- POST /api/predicoes/{id}/revisao     -> a implementar (Carlos)
- GET  /api/predicoes                  -> a implementar (Carlos), filtros e paginação
"""

import requests

import config
import mock


class ApiErro(Exception):
    """Erro devolvido pela API (formato RFC 7807) ou falha de conexão."""

    def __init__(self, status, mensagem, erros=None):
        super().__init__(mensagem)
        self.status = status
        self.mensagem = mensagem
        self.erros = erros or []


def _requisicao(metodo, caminho, token=None, json=None, params=None):
    cabecalhos = {"Authorization": f"Bearer {token}"} if token else {}
    try:
        resposta = requests.request(
            metodo,
            f"{config.API_URL}{caminho}",
            headers=cabecalhos,
            json=json,
            params=params,
            timeout=config.API_TIMEOUT,
        )
    except requests.RequestException:
        raise ApiErro(0, "Não foi possível conectar à API. Tente novamente em instantes.")

    if resposta.ok:
        return resposta.json() if resposta.content else None

    try:
        corpo = resposta.json()
    except ValueError:
        corpo = {}
    mensagem = corpo.get("detail") or corpo.get("title") or f"Erro {resposta.status_code} na API."
    erros = [f"{e.get('campo')}: {e.get('mensagem')}" for e in corpo.get("erros", [])]
    raise ApiErro(resposta.status_code, mensagem, erros)


def login(email, senha):
    """Devolve {token, tipo, expiraEmMinutos, usuario: {id, nome, email, perfil, ...}}."""
    if config.MODO_DEMO:
        return mock.login(email, senha)
    return _requisicao("POST", "/api/auth/login", json={"email": email, "senha": senha})


def criar_predicao(token, dados):
    """Envia os 10 atributos e devolve a predição registrada."""
    if config.MODO_DEMO:
        return mock.criar_predicao(dados, mock.usuario_do_token(token))
    return _requisicao("POST", "/api/predicoes", token, json=dados)


def solicitar_revisao(token, predicao_id, motivo):
    if config.MODO_DEMO:
        return mock.solicitar_revisao(predicao_id, motivo)
    return _requisicao("POST", f"/api/predicoes/{predicao_id}/revisao", token, json={"motivo": motivo})


def listar_usuarios(token):
    """Usuários para o filtro da Auditoria (GET /api/usuarios, só gestor/auditor). Devolve a lista."""
    if config.MODO_DEMO:
        return mock.listar_usuarios()
    pagina = _requisicao("GET", "/api/usuarios", token, params={"size": 100, "sort": "nome,asc"})
    return pagina.get("content", [])


def listar_predicoes(token, inicio=None, fim=None, usuario_id=None, faixa=None, pagina=0, tamanho=50):
    """Histórico paginado. Devolve uma página do Spring: {content: [...], totalElements, ...}."""
    params = {
        "inicio": inicio.isoformat() if inicio else None,
        "fim": fim.isoformat() if fim else None,
        "usuarioId": usuario_id,
        "faixa": faixa,
        "page": pagina,
        "size": tamanho,
        "sort": "criadoEm,desc",
    }
    params = {k: v for k, v in params.items() if v is not None}
    if config.MODO_DEMO:
        return mock.listar_predicoes(params)
    return _requisicao("GET", "/api/predicoes", token, params=params)
