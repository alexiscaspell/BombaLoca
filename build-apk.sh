#!/bin/bash

# Colores para mensajes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

echo -e "${PURPLE}🇦🇷 ====================================== 🇦🇷${NC}"
echo -e "${BLUE}         BOMBA LOCA - Geografía Argentina${NC}"
echo -e "${PURPLE}🇦🇷 ====================================== 🇦🇷${NC}"
echo -e "${YELLOW}💣 Iniciando construcción del APK...${NC}"

# Verificar si Docker está instalado
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: Docker no está instalado. Por favor, instala Docker primero.${NC}"
    exit 1
fi

# Verificar si Gradle está instalado
if ! command -v gradle &> /dev/null; then
    echo -e "${YELLOW}Gradle no está instalado. Instalando...${NC}"
    sudo apt-get update && sudo apt-get install -y gradle || {
        echo -e "${RED}Error al instalar Gradle${NC}"
        exit 1
    }
fi

# Crear directorio gradle/wrapper si no existe
mkdir -p gradle/wrapper

# Construir la imagen Docker
echo -e "${YELLOW}🏗️  Construyendo imagen Docker para Bomba Loca...${NC}"
docker build -t bomba-loca-builder . || {
    echo -e "${RED}💥 Error al construir la imagen Docker${NC}"
    exit 1
}

# Crear un contenedor temporal
echo -e "${YELLOW}📦 Creando contenedor temporal...${NC}"
docker create --name bomba-loca-container bomba-loca-builder || {
    echo -e "${RED}💥 Error al crear el contenedor temporal${NC}"
    exit 1
}

# Extraer el APK
echo -e "${YELLOW}📲 Extrayendo el APK de Bomba Loca...${NC}"
docker cp bomba-loca-container:/app/app/build/outputs/apk/debug/app-debug.apk ./bomba-loca.apk || {
    echo -e "${RED}💥 Error al extraer el APK${NC}"
    exit 1
}

# Limpiar el contenedor temporal
echo -e "${YELLOW}🧹 Limpiando contenedor temporal...${NC}"
docker rm bomba-loca-container || {
    echo -e "${RED}💥 Error al limpiar el contenedor temporal${NC}"
    exit 1
}

# Verificar si el APK se generó correctamente
if [ -f "./bomba-loca.apk" ]; then
    echo -e "${GREEN}🎉 ¡APK de Bomba Loca generado exitosamente! 🎉${NC}"
    echo -e "${GREEN}📱 El APK se encuentra en: $(pwd)/bomba-loca.apk${NC}"
    echo -e "${BLUE}🇦🇷 ¡Listo para conquistar Argentina! 🗺️${NC}"
    echo -e "${PURPLE}💣 Tienes 3 vidas para completar el mapa 💣${NC}"
else
    echo -e "${RED}💥 Error: No se pudo generar el APK de Bomba Loca${NC}"
    exit 1
fi 