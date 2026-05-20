import axios from 'axios';

const axiosClient = axios.create({
    baseURL: 'http://localhost:8080/api',
});

// Request interceptor to attach authentication token
axiosClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            // Attach Bearer token for authenticated endpoints
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        else {
            config.headers['Authorization'] = 'Bearer guest-temporary-token';
        }
        return config;
    },
    (error) => {
        // Forward any request setup errors to be handled by callers
        return Promise.reject(error);
    }
);

export default axiosClient;
