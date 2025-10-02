import React, { createContext, useReducer, useEffect, useMemo, useCallback } from 'react';
import { authApi } from '../services/apiService';

// 1. 초기 상태 정의
const initialState = {
    isAuthenticated: false,
    user: null,
    loading: true,
};

// 2. Reducer 함수 정의
const authReducer = (state, action) => {
    switch (action.type) {
        case 'LOGIN_SUCCESS':
            return {
                ...state,
                isAuthenticated: true,
                user: action.payload, // payload는 { username, role, nickname } 형태
                loading: false,
            };
        case 'LOGOUT':
            return {
                ...state,
                isAuthenticated: false,
                user: null,
                loading: false,
            };
        case 'SET_LOADING':
            return {
                ...state,
                loading: action.payload,
            };
        default:
            return state;
    }
};

// 3. Context 생성
export const AuthContext = createContext(initialState);

// 4. AuthProvider 컴포넌트
export const AuthProvider = ({ children }) => {
    const [state, dispatch] = useReducer(authReducer, initialState);

    useEffect(() => {
        const checkSession = async () => {
            try {
                const response = await authApi.checkSessionStatus();
                // 백엔드에서 받은 사용자 정보와 역할을 그대로 사용
                if (response.status === 200 && response.data && response.data.username) {
                    dispatch({ type: 'LOGIN_SUCCESS', payload: response.data });
                } else {
                    dispatch({ type: 'LOGOUT' });
                }
            } catch (error) {
                console.log("Session check failed (expected for non-logged in users or on logout):", error.response ? error.response.status : error.message);
                dispatch({ type: 'LOGOUT' });
            } finally {
                dispatch({ type: 'SET_LOADING', payload: false });
            }
        };

        checkSession();
    }, []);

    // 로그인 함수를 useCallback으로 감싸서 메모이제이션
    const login = useCallback(async (username, password) => {
        try {
            // FIX: 로그인 성공 후, 백엔드로부터 받은 응답 데이터를 직접 사용
            const response = await authApi.login(username, password);
            if (response.status === 200 && response.data && response.data.username) {
                dispatch({ type: 'LOGIN_SUCCESS', payload: response.data });
            } else {
                // 로그인 성공했으나 응답 데이터가 비정상적인 경우
                dispatch({ type: 'LOGOUT' });
                throw new Error('로그인 성공 후 사용자 정보 확인 실패');
            }
            return true;
        } catch (error) {
            console.error('Login failed:', error);
            // 로그인 실패 시 상태를 로그아웃으로 변경
            dispatch({ type: 'LOGOUT' });
            throw error;
        }
    }, []);

    // 로그아웃 함수를 useCallback으로 감싸서 메모이제이션
    const logout = useCallback(async () => {
        try {
            await authApi.logout();
            dispatch({ type: 'LOGOUT' });
            return true;
        } catch (error) {
            console.error('Logout failed:', error);
            throw error;
        }
    }, []);

    // Context value를 useMemo로 감싸 불필요한 리렌더링 방지
    const authContextValue = useMemo(() => ({
        ...state,
        login,
        logout,
    }), [state, login, logout]);

    return (
        <AuthContext.Provider value={authContextValue}>
            {children}
        </AuthContext.Provider>
    );
};
