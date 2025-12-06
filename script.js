document.addEventListener('DOMContentLoaded', function() {
    // URL de tu Apps Script (¡REEMPLAZA CON LA TUYA!)
    const API_URL = 'https://script.google.com/macros/s/YOUR_APPS_SCRIPT_DEPLOYMENT_ID/exec';
    
    // Elementos del DOM
    const moviesContainer = document.getElementById('moviesContainer');
    const playerModal = document.getElementById('playerModal');
    const moviePlayer = document.getElementById('moviePlayer');
    const movieTitle = document.getElementById('movieTitle');
    const closeModal = document.querySelector('.close');
    const instructionsModal = document.getElementById('instructionsModal');
    const instructionsBtn = document.getElementById('howToBtn');
    const closeInstructions = document.querySelector('.close-instructions');

    // Cargar películas
    async function loadMovies() {
        try {
            moviesContainer.innerHTML = '<div class="loading">Cargando películas...</div>';
            
            const response = await fetch(API_URL);
            if (!response.ok) throw new Error('Error en la respuesta de la API');
            
            const movies = await response.json();
            
            if (movies.length === 0) {
                moviesContainer.innerHTML = '<div class="loading">No hay películas en tu carpeta. ¡Sube algunas!</div>';
                return;
            }
            
            // Generar tarjetas de películas
            moviesContainer.innerHTML = movies.map(movie => `
                <div class="movie-card" data-url="${movie.url}" data-title="${movie.title}">
                    <img src="${movie.thumbnail}" alt="${movie.title}" 
                         onerror="this.src='https://via.placeholder.com/250x141?text=Sin+Portada'">
                    <div class="info">
                        <div class="title">${movie.title}</div>
                        <div class="meta">
                            <span>${formatFileSize(movie.duration)}</span>
                        </div>
                    </div>
                </div>
            `).join('');
            
            // Agregar evento de clic a cada tarjeta
            document.querySelectorAll('.movie-card').forEach(card => {
                card.addEventListener('click', () => {
                    const url = card.getAttribute('data-url');
                    const title = card.getAttribute('data-title');
                    playMovie(url, title);
                });
            });
            
        } catch (error) {
            console.error('Error:', error);
            moviesContainer.innerHTML = `<div class="loading">Error cargando películas: ${error.message}</div>`;
        }
    }

    // Reproducir película
    function playMovie(url, title) {
        moviePlayer.src = url;
        movieTitle.textContent = title;
        playerModal.style.display = 'block';
        moviePlayer.play();
    }

    // Cerrar modal del reproductor
    closeModal.addEventListener('click', () => {
        playerModal.style.display = 'none';
        moviePlayer.pause();
        moviePlayer.src = '';
    });

    // Cerrar modal al hacer clic fuera
    window.addEventListener('click', (event) => {
        if (event.target === playerModal) {
            playerModal.style.display = 'none';
            moviePlayer.pause();
            moviePlayer.src = '';
        }
    });

    // Mostrar instrucciones
    instructionsBtn.addEventListener('click', () => {
        instructionsModal.style.display = 'block';
    });

    // Cerrar instrucciones
    closeInstructions.addEventListener('click', () => {
        instructionsModal.style.display = 'none';
    });

    // Cerrar instrucciones al hacer clic fuera
    window.addEventListener('click', (event) => {
        if (event.target === instructionsModal) {
            instructionsModal.style.display = 'none';
        }
    });

    // Funciones auxiliares
    function formatFileSize(bytes) {
        if (bytes === 0) return "0 Bytes";
        const k = 1024;
        const sizes = ["Bytes", "KB", "MB", "GB"];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
    }

    // Cargar películas al iniciar
    loadMovies();
});