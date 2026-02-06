import type { AxiosInstance } from 'axios';
import { createHttpClient } from './httpClient';

let httpClient: AxiosInstance | null = null;

export const initApiClient = (baseUrl: string): AxiosInstance => {
    httpClient = createHttpClient({ baseUrl });
    return httpClient;
};

export const getApiClient = (): AxiosInstance => {
    if (!httpClient) {
        throw new Error('API client not initialized. Call initApiClient(apiUrl) first.');
    }
    return httpClient;
};
