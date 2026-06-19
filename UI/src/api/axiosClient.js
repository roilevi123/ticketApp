import axios from 'axios';

const axiosClient = axios.create({
    baseURL: 'http://localhost:8080/api',
});

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
        return Promise.reject(error);
    }
);

axiosClient.interceptors.response.use(
    (response) => response,
    (error) => {
        const isNetworkError =
            error.code === 'ERR_NETWORK' && !error.response;

        if (!navigator.onLine) {
            alert("Connection lost. Please check your internet connection.");
            return Promise.reject(error);
        }

        if (isNetworkError) {
            alert("Server is unavailable. Please try again later.");
            return Promise.reject(error);
        }

        if (
            error.response?.status === 401 &&
            error.response?.data?.error === 'ACCOUNT_REMOVED'
        ) {
            localStorage.removeItem('token');
            localStorage.removeItem('role');
            localStorage.removeItem('userID');
            window.location.replace('/account-removed');
        }

        return Promise.reject(error);
    }
);
export default axiosClient;
