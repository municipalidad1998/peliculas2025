function cargarDoramaExpress() {
  const script = document.createElement('script');
  script.src = 'https://raw.githubusercontent.com/municipalidad1998/peliculas2025/main/DoramaExpress.js';
  script.async = true;
  document.head.appendChild(script);
}

cargarDoramaExpress();
