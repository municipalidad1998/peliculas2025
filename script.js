// Integrating DoramaExpress.js module

import DoramaExpress from 'DoramaExpress';

// Existing functionality
function searchSeries(query) {
    // Existing search functionality
}

function loadSeriesInfo(seriesId) {
    // Existing load info functionality
}

function playVideo(videoId) {
    // Existing video playback functionality
}

// New functionality to support DoramaExpress
function searchDorama(query) {
    // Searching for series using DoramaExpress
    return DoramaExpress.search(query);
}

function loadDoramaInfo(seriesId) {
    // Load series information with DoramaExpress
    return DoramaExpress.loadSeriesInfo(seriesId);
}

function playDoramaVideo(videoId) {
    // Play video using DoramaExpress functionality
    DoramaExpress.playVideo(videoId);
}