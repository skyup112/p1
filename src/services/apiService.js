import axios from 'axios';
import qs from 'qs'; // For x-www-form-urlencoded login

// Base URL for the backend API
const API_BASE_URL = 'http://localhost:8080/api'; // Or your deployed backend URL

const api = axios.create({
    baseURL: API_BASE_URL,
    withCredentials: true, // Important for session-based authentication (JSESSIONID cookie continues to be used)
});

// Request Interceptor: CSRF token logic removed (no longer need to add X-XSRF-TOKEN header)
api.interceptors.request.use(config => {
    // JWT 토큰이 있다면 Authorization 헤더에 추가 (이전 답변에서 누락되었을 수 있어 추가)
    const token = localStorage.getItem('jwtToken');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
}, error => {
    return Promise.reject(error);
});

// Axios Interceptor: Intercept API requests and responses (alert() removed)
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response) {
            console.error('API Response Error:', error.response.status, error.response.data);
            // You can trigger modal messages for the user here
            // Example: dispatch(setModalMessage('Server error occurred.'));
        } else if (error.request) {
            console.error('Network Error: No response received.', error.request);
            // Example: dispatch(setModalMessage('Please check your network connection.'));
        } else {
            console.error('Error setting up request:', error.message);
            // Example: dispatch(setModalMessage('An unknown error occurred while processing the request.'));
        }
        return Promise.reject(error);
    }
);

// --- Authentication related API ---
export const authApi = {
    // Modified login function to return the entire backend response
    login: (username, password) => api.post('/auth/login',
        qs.stringify({ username, password }),
        { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } }
    ),
    logout: () => api.post('/auth/logout'),
    register: (registerDTO) => api.post('/auth/register', registerDTO),
    checkSessionStatus: () => api.get('/auth/status'),
    checkEmailExists: (email) => api.get(`/auth/check-email?email=${encodeURIComponent(email)}`),
    checkUsernameExists: (username) => api.get(`/auth/check-username?username=${encodeURIComponent(username)}`),
};

// --- 경기 일정 관련 API ---
export const gameScheduleApi = {
    getAllGames: () => api.get('/games'),
    getGameById: (id) => api.get(`/games/${id}`),
    createGame: (gameDTO) => api.post('/games', gameDTO),
    updateGame: (id, gameDTO) => api.put(`/games/${id}`, gameDTO),
    deleteGame: (id) => api.delete(`/games/${id}`),
    // ⭐⭐⭐ 이 부분이 추가되어야 합니다! ⭐⭐⭐
    crawlGameSchedules: (seasonYear, month) => api.post(`/games/crawl-and-update`, null, { params: { seasonYear, month } }),
    // ⭐⭐⭐ 위 코드와 정확히 일치하는지 확인해주세요. ⭐⭐⭐
};

// --- Comment related API ---
export const commentApi = {
    addComment: (gameId, commentDTO) => api.post(`/games/${gameId}/comments`, commentDTO),
    // Paging parameters added: page, size, and sorting parameters (sortBy, sortDirection)
    getCommentsByGameId: (gameId, page = 0, size = 10, sortBy = 'createdAt', sortDirection = 'desc') => 
        api.get(`/games/${gameId}/comments`, { 
            params: { 
                page, 
                size, 
                sortBy, 
                sortDirection 
            } 
        }),
    // Updated endpoint for prediction counts
    getPredictionCommentCounts: (gameId) => api.get(`/games/${gameId}/comments/prediction-counts`),
    updateComment: (gameId, commentId, commentDTO) => api.put(`/games/${gameId}/comments/${commentId}`, commentDTO),
    deleteComment: (gameId, commentId) => api.delete(`/games/${gameId}/comments/${commentId}`),
};

// --- Lineup related API ---
export const lineupApi = {
    getLineupByGameId: (gameId) => api.get(`/games/${gameId}/lineup`),
    createOrUpdateLineup: (gameId, lineupDTO) => api.put(`/games/${gameId}/lineup`, lineupDTO),
    deleteLineupByGameId: (gameId) => api.delete(`/games/${gameId}/lineup`),
    // ⭐ 추가된 라인업 크롤링 API 엔드포인트 ⭐
    crawlLineups: (gameId) => api.post(`/lineups/${gameId}/crawl`), // LineupController의 엔드포인트와 일치
};

// --- Admin Member Management API ---
export const adminMemberApi = {
    getAllMembers: () => api.get('/admin/members'),
    getMemberByUsername: (username) => api.get(`/admin/members/${username}`),
    updateMember: (id, memberDTO) => api.put(`/admin/members/${id}`, memberDTO),
    banMemberPermanently: (username) => api.post(`/admin/members/${username}/ban/permanent`),
    banMemberTemporarily: (username, days) => {
        const untilDate = new Date();
        untilDate.setDate(untilDate.getDate() + days);
        untilDate.setHours(23, 59, 59, 999);
        return api.post(`/admin/members/ban/temp`, { username, bannedUntil: untilDate.toISOString() });
    },
    unbanMember: (username) => api.post(`/admin/members/unban`, { username }),
    deleteMember: (id) => api.delete(`/admin/members/${id}`),
};

// --- Team Management API (getTeamByName added) ---
export const teamApi = {
    getAllTeams: () => api.get('/teams'),
    getTeamById: (id) => api.get(`/teams/${id}`),
    // NEW: Endpoint to get team by name
    getTeamByName: (name) => api.get(`/teams/by-name/${encodeURIComponent(name)}`),
    createTeam: (teamDTO) => api.post('/teams', teamDTO),
    updateTeam: (id, teamDTO) => api.put(`/teams/${id}`, teamDTO),
    deleteTeam: (id) => api.delete(`/teams/${id}`),
};

// --- User Profile Management API (added) ---
export const userApi = {
    getUserProfile: () => api.get('/members/me'),
    updateUserProfile: (updateRequestDTO) => api.put('/members/me', updateRequestDTO),
    changePassword: (passwordChangeRequestDTO) => api.put('/members/me/password', passwordChangeRequestDTO),
    deleteUserAccount: (deleteRequestDTO) => api.delete('/members/me', { data: deleteRequestDTO }),
};

// --- NEW: Team Ranking related API ---
export const teamRankingApi = {
    // For GET requests, passing query parameters in a `params` object is cleaner.
    getAllTeamRankings: (seasonYear) => api.get(`/rankings`, { params: { seasonYear } }),
    getTeamRankingById: (id) => api.get(`/rankings/${id}`),
    // For POST requests, passing query parameters in a `params` object is also cleaner.
    // Pass null as the second argument if no request body is needed.
    calculateAndSaveRankings: (seasonYear) => api.post(`/rankings/calculate`, null, { params: { seasonYear } }),
    createTeamRanking: (teamRankingDTO) => api.post('/rankings', teamRankingDTO),
    updateTeamRanking: (id, teamRankingDTO) => api.put(`/rankings/${id}`, teamRankingDTO),
    deleteTeamRanking: (id) => api.delete(`/rankings/${id}`),
    // ⭐⭐⭐ This part was added! ⭐⭐⭐
    crawlAndUpdateRankings: (seasonYear) => api.post(`/rankings/crawl-and-update`, null, { params: { seasonYear } }),
};

export default api;