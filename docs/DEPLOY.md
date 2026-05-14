# Deployment Guide - Search Microservice
## 1. Prerequisites
Para desplegar este microservicio, es necesario contar con:

Java 17 y Maven (para la construcción del artefacto).

Docker y Docker Compose (para el despliegue de contenedores).

## 2. Build project
Ejecuta el siguiente comando en la raíz del proyecto para generar el archivo JAR:

Bash
mvn clean package -DskipTests
## 3. Run production environment
Una vez generado el artefacto, levanta los servicios definidos en el archivo de producción:

Bash
docker-compose -f deploy/docker-compose.prod.yml up -d
## 4. API Endpoints
Una vez que los contenedores estén en ejecución, puedes probar el microservicio con los siguientes endpoints:

Búsqueda: GET /api/search?q=laptop
Sugerencias: GET /api/search/suggest?q=lap