// DoramaExpress.js

class DoramaExpress {
    constructor() {
        this.apiUrl = 'https://api.example.com/dorama';
    }

    async fetchContent() {
        try {
            const response = await fetch(this.apiUrl);
            const data = await response.json();
            this.renderContent(data);
        } catch (error) {
            console.error('Error fetching content:', error);
        }
    }

    renderContent(data) {
        const contentContainer = document.getElementById('content');
        contentContainer.innerHTML = '';
        data.forEach(item => {
            const element = document.createElement('div');
            element.innerHTML = `<h3>${item.title}</h3>'; 
            contentContainer.appendChild(element);
        });
    }

    async search(query) {
        try {
            const response = await fetch(`${this.apiUrl}/search?q=${query}`);
            const data = await response.json();
            this.renderContent(data);
        } catch (error) {
            console.error('Error during search:', error);
        }
    }

    async loadSeriesInfo(seriesId) {
        try {
            const response = await fetch(`${this.apiUrl}/series/${seriesId}`);
            return await response.json();
        } catch (error) {
            console.error('Error loading series info:', error);
        }
    }

    async getVideoLinks(seriesId) {
        try {
            const response = await fetch(`${this.apiUrl}/series/${seriesId}/videos`);
            return await response.json();
        } catch (error) {
            console.error('Error getting video links:', error);
        }
    }
}

export default DoramaExpress;