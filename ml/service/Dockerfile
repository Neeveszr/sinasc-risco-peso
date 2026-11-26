# Serviço de predição. Construir a partir da pasta ml/:
#   cd ml && docker build -f service/Dockerfile -t sinasc-ml .
#   docker run -p 8000:8000 sinasc-ml
FROM python:3.11-slim

WORKDIR /app

COPY service/requirements.txt service/requirements.txt
RUN pip install --no-cache-dir -r service/requirements.txt

COPY src/ src/
COPY artifacts/ artifacts/
COPY service/ service/

WORKDIR /app/service
EXPOSE 8000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
