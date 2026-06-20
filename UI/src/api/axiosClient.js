import axios from 'axios';

const axiosClient = axios.create({
    baseURL: 'http://localhost:8080/api',
});

const showGlobalError = (message) => {
    window.dispatchEvent(
        new CustomEvent('global-error', {
            detail: { message },
        })
    );
};

axiosClient.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token') || 'guest-temporary-token';
        config.headers['Authorization'] = `Bearer ${token}`;
        return config;
    },
    (error) => Promise.reject(error)
);

axiosClient.interceptors.response.use(
    (response) => response,
    (error) => {
        const isNetworkError = !error.response;

        if (!navigator.onLine) {
            error.isGlobalHandled = true;
            showGlobalError('Connection lost. Please check your internet connection.');
            return Promise.reject(error);
        }

        if (isNetworkError) {
            error.isGlobalHandled = true;
            showGlobalError('Server is unavailable. Please try again later.');
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