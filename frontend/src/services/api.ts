import axios from "axios";

/**
 * Instancia de Axios configurada para la API del backend.
 * Base URL: /api/v1 (proxied a localhost:8080 en desarrollo)
 */
export const api = axios.create({
  baseURL: "/api/v1",
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 10000,
});

/**
 * Interceptor de respuesta para manejo centralizado de errores.
 */
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      // El servidor respondió con un código de estado fuera del rango 2xx
      console.error("API Error:", error.response.status, error.response.data);
    } else if (error.request) {
      // La petición se envió pero no hubo respuesta
      console.error("Network Error:", error.message);
    } else {
      // Algo pasó al configurar la petición
      console.error("Request Error:", error.message);
    }
    return Promise.reject(error);
  },
);

export default api;
