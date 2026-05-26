import axios from 'axios';

const axiosClient = axios.create({
    baseURL: 'http://localhost:8080/api',
});

// Request interceptor to attach authentication token
axiosClient.interceptors.request.use(
    (config) => {
        // Fall back to the hardcoded guest token if no token is in localStorage yet
        // (prevents a race condition on first load where AuthContext hasn't fetched
        // the guest JWT before EventCatalog fires its first request).
        const token = localStorage.getItem('token') || 'guest-temporary-token';
        config.headers['Authorization'] = `Bearer ${token}`;
        return config;
    },
    (error) => {
        // Forward any request setup errors to be handled by callers
        return Promise.reject(error);
    }
);

export default axiosClient;
