# Utiliser l'image Nginx officielle comme base
FROM nginx:1.25-alpine

# Copier les fichiers statiques de l'interface (HTML, CSS, JS)
COPY index.html /usr/share/nginx/html
COPY style.css /usr/share/nginx/html
COPY app.js /usr/share/nginx/html

# Copier la configuration Nginx personnalisée
COPY nginx.conf /etc/nginx/conf.d/default.conf
