"""Configuração do front-end, lida das variáveis de ambiente."""

import os

# Endereço da API Spring Boot (sem barra no final).
API_URL = os.getenv("API_URL", "http://localhost:8080").rstrip("/")

# Tempo máximo de espera por resposta da API, em segundos.
API_TIMEOUT = float(os.getenv("API_TIMEOUT", "15"))

# Modo demonstração: "1" usa respostas fictícias e não chama a API.
# Serve para desenvolver as telas antes de as rotas do back-end ficarem prontas.
MODO_DEMO = os.getenv("API_MOCK", "0") == "1"

NOME_SISTEMA = "Risco de Baixo Peso ao Nascer"
