import React, { useReducer, useState, useMemo, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authApi } from '../services/apiService';
import Modal from './Modal';

// 1. Initial State Definition
const initialState = {
    username: '',
    password: '',
    confirmPassword: '',
    name: '',
    nickname: '',
    email: '',
    phoneNumber: '',
    modalMessage: '',
    // --- 유효성 검사 관련 상태 추가 ---
    usernameError: '',
    usernameExistsError: '', // 아이디 중복 에러 메시지
    isUsernameChecking: false, // 아이디 중복 확인 중인지 여부
    passwordMatchError: '',
    emailError: '',
    isEmailChecking: false, // 이메일 중복 확인 중인지 여부
};

// 2. Reducer Function Definition
function registerFormReducer(state, action) {
    switch (action.type) {
        case 'SET_FIELD':
            return { ...state, [action.field]: action.value };
        case 'SET_MODAL_MESSAGE':
            return { ...state, modalMessage: action.payload };
        case 'CLEAR_MODAL_MESSAGE':
            return { ...state, modalMessage: '' };
        case 'RESET_FORM':
            return { ...initialState, modalMessage: state.modalMessage };
        // --- 유효성 검사 관련 액션 추가 ---
        case 'SET_USERNAME_ERROR':
            return { ...state, usernameError: action.payload };
        case 'SET_USERNAME_EXISTS_ERROR': // 아이디 중복 에러 액션
            return { ...state, usernameExistsError: action.payload };
        case 'SET_USERNAME_CHECKING': // 아이디 중복 확인 중 액션
            return { ...state, isUsernameChecking: action.payload };
        case 'SET_PASSWORD_MATCH_ERROR':
            return { ...state, passwordMatchError: action.payload };
        case 'SET_EMAIL_ERROR':
            return { ...state, emailError: action.payload };
        case 'SET_EMAIL_CHECKING':
            return { ...state, isEmailChecking: action.payload };
        default:
            return state;
    }
}

function RegisterForm() {
    const [state, dispatch] = useReducer(registerFormReducer, initialState);
    const navigate = useNavigate();

    // Destructure state for easier access
    const {
        username,
        password,
        confirmPassword,
        name,
        nickname,
        email,
        phoneNumber,
        modalMessage,
        // --- 유효성 검사 관련 상태 ---
        usernameError,
        usernameExistsError, // 추가
        isUsernameChecking, // 추가
        passwordMatchError,
        emailError,
        isEmailChecking,
    } = state;

    // 중복 확인을 위한 디바운스 타이머
    const [emailDebounceTimer, setEmailDebounceTimer] = useState(null);
    const [usernameDebounceTimer, setUsernameDebounceTimer] = useState(null); // 아이디 디바운스 타이머

    // --- 유효성 검사 로직 ---

    // 아이디 유효성 검사 (영어 알파벳과 숫자만) 및 중복 확인
    const validateUsername = useCallback(async (value) => {
        // 1. 형식 검사
        if (!/^[a-zA-Z0-9]*$/.test(value)) { // 영어 알파벳과 숫자만 허용
            dispatch({ type: 'SET_USERNAME_ERROR', payload: '아이디는 영어 알파벳과 숫자만 입력 가능합니다.' });
            dispatch({ type: 'SET_USERNAME_EXISTS_ERROR', payload: '' }); // 형식 오류 시 중복 오류 초기화
            return false;
        }
        dispatch({ type: 'SET_USERNAME_ERROR', payload: '' });

        // 2. 중복 확인 (디바운싱 적용)
        if (usernameDebounceTimer) {
            clearTimeout(usernameDebounceTimer);
        }

        if (value.length >= 4) { // 최소 길이 (예시: 4글자 이상일 때만 중복 확인)
            dispatch({ type: 'SET_USERNAME_CHECKING', payload: true });
            setUsernameDebounceTimer(setTimeout(async () => {
                try {
                    // FIX: 실제 authApi.checkUsernameExists 사용
                    const response = await authApi.checkUsernameExists(value);
                    // 백엔드가 200 OK와 함께 boolean 값을 반환한다고 가정
                    const isDuplicate = response.data;

                    if (isDuplicate) {
                        dispatch({ type: 'SET_USERNAME_EXISTS_ERROR', payload: '이미 사용 중인 아이디입니다.' });
                    } else {
                        dispatch({ type: 'SET_USERNAME_EXISTS_ERROR', payload: '' });
                    }
                } catch (error) {
                    console.error('아이디 중복 확인 실패:', error);
                    // 백엔드에서 404가 아닌 200 OK와 { exists: false }를 반환하는 것이 일반적이지만,
                    // 만약 404를 반환한다면 중복이 아닌 것으로 처리 (서버 설정에 따라 다름)
                    if (error.response && error.response.status === 404) {
                        dispatch({ type: 'SET_USERNAME_EXISTS_ERROR', payload: '' }); // 아이디가 없으면 중복 아님
                    } else {
                        dispatch({ type: 'SET_USERNAME_EXISTS_ERROR', payload: '아이디 중복 확인 중 오류가 발생했습니다.' });
                    }
                } finally {
                    dispatch({ type: 'SET_USERNAME_CHECKING', payload: false });
                }
            }, 500)); // 500ms 디바운스
        } else {
            dispatch({ type: 'SET_USERNAME_CHECKING', payload: false });
            dispatch({ type: 'SET_USERNAME_EXISTS_ERROR', payload: '' }); // 길이가 짧으면 중복 오류 초기화
        }
        return true;
    }, [usernameDebounceTimer]);

    // 비밀번호 일치 여부 검사
    const validatePasswordMatch = useCallback((pwd, confirmPwd) => {
        if (pwd && confirmPwd && pwd !== confirmPwd) {
            dispatch({ type: 'SET_PASSWORD_MATCH_ERROR', payload: '비밀번호가 일치하지 않습니다.' });
            return false;
        }
        dispatch({ type: 'SET_PASSWORD_MATCH_ERROR', payload: '' });
        return true;
    }, []);

    // 이메일 유효성 및 중복 확인
    const validateEmail = useCallback(async (value) => {
        // 1. 형식 검사
        if (!/^\S+@\S+\.\S+$/.test(value)) {
            dispatch({ type: 'SET_EMAIL_ERROR', payload: '유효한 이메일 형식이 아닙니다.' });
            return false;
        }
        dispatch({ type: 'SET_EMAIL_ERROR', payload: '' });

        // 2. 중복 확인 (디바운싱 적용)
        if (emailDebounceTimer) {
            clearTimeout(emailDebounceTimer);
        }

        if (value) {
            dispatch({ type: 'SET_EMAIL_CHECKING', payload: true });
            setEmailDebounceTimer(setTimeout(async () => {
                try {
                    // FIX: 실제 authApi.checkEmailExists 사용
                    const response = await authApi.checkEmailExists(value);
                    // 백엔드가 200 OK와 함께 boolean 값을 반환한다고 가정
                    const isDuplicate = response.data;

                    if (isDuplicate) {
                        dispatch({ type: 'SET_EMAIL_ERROR', payload: '이미 사용 중인 이메일입니다.' });
                    } else {
                        dispatch({ type: 'SET_EMAIL_ERROR', payload: '' });
                    }
                } catch (error) {
                    console.error('이메일 중복 확인 실패:', error);
                    // 백엔드에서 404가 아닌 200 OK와 { exists: false }를 반환하는 것이 일반적이지만,
                    // 만약 404를 반환한다면 중복이 아닌 것으로 처리 (서버 설정에 따라 다름)
                    if (error.response && error.response.status === 404) {
                        dispatch({ type: 'SET_EMAIL_ERROR', payload: '' }); // 이메일이 없으면 중복 아님
                    } else {
                        dispatch({ type: 'SET_EMAIL_ERROR', payload: '이메일 중복 확인 중 오류가 발생했습니다.' });
                    }
                } finally {
                    dispatch({ type: 'SET_EMAIL_CHECKING', payload: false });
                }
            }, 500)); // 500ms 디바운스
        } else {
            dispatch({ type: 'SET_EMAIL_CHECKING', payload: false });
        }
        return true;
    }, [emailDebounceTimer]);


    // --- 필드 변경 핸들러 ---
    const handleChange = (e) => {
        const { id, value } = e.target;
        dispatch({ type: 'SET_FIELD', field: id, value });

        // 실시간 유효성 검사 호출
        if (id === 'username') {
            validateUsername(value);
        } else if (id === 'password' || id === 'confirmPassword') {
            const currentPassword = id === 'password' ? value : password;
            const currentConfirmPassword = id === 'confirmPassword' ? value : confirmPassword;
            validatePasswordMatch(currentPassword, currentConfirmPassword);
        } else if (id === 'email') {
            validateEmail(value);
        }
    };

    // 모든 유효성 검사 통과 여부
    const isFormValid = useMemo(() => {
        return (
            username && !usernameError && !usernameExistsError && !isUsernameChecking && // 아이디 관련 유효성 검사 추가
            password && confirmPassword && !passwordMatchError &&
            name && nickname && email && phoneNumber && !emailError && !isEmailChecking
        );
    }, [username, usernameError, usernameExistsError, isUsernameChecking, password, confirmPassword, passwordMatchError, name, nickname, email, phoneNumber, emailError, isEmailChecking]);


    const handleSubmit = async (e) => {
        e.preventDefault();
        dispatch({ type: 'CLEAR_MODAL_MESSAGE' }); // Clear previous modal message

        // 최종 유효성 검사
        // validateUsername과 validateEmail은 비동기 디바운싱이므로, handleSubmit에서는
        // 현재 상태의 에러 메시지와 로딩 상태를 확인하는 방식으로 처리합니다.
        const isUsernameValidFormat = /^[a-zA-Z0-9]*$/.test(username);
        const isPasswordMatch = password === confirmPassword;
        const isEmailFormatValid = /^\S+@\S+\.\S+$/.test(email);

        if (!isUsernameValidFormat || usernameError || usernameExistsError || isUsernameChecking ||
            !isPasswordMatch || passwordMatchError ||
            !isEmailFormatValid || emailError || isEmailChecking) {
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '입력된 정보를 확인해주세요.' });
            return;
        }

        try {
            await authApi.register({
                username,
                password,
                name,
                nickname,
                email,
                phoneNumber,
            });
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '회원가입 성공! 로그인 페이지로 이동합니다.' });
            dispatch({ type: 'RESET_FORM' });

            setTimeout(() => {
                navigate('/login');
            }, 2000);
        } catch (error) {
            console.error('회원가입 실패:', error);
            if (error.response && error.response.status === 409) {
                dispatch({ type: 'SET_MODAL_MESSAGE', payload: error.response.data || '이미 존재하는 아이디 또는 중복된 정보가 있습니다.' });
            } else {
                dispatch({ type: 'SET_MODAL_MESSAGE', payload: '회원가입 중 오류가 발생했습니다.' });
            }
        }
    };

    const handleModalClose = () => {
        dispatch({ type: 'CLEAR_MODAL_MESSAGE' });
    };

    return (
        <div className="container my-5 p-4 bg-white rounded-lg shadow-lg" style={{ maxWidth: '900px' }}> {/* maxWidth를 900px로 변경 */}
            <h2 className="text-center text-primary mb-4">회원가입</h2>
            <form onSubmit={handleSubmit}>
                <div className="mb-3">
                    <label htmlFor="username" className="form-label fw-bold">아이디:</label>
                    <input
                        type="text"
                        id="username"
                        value={username}
                        onChange={handleChange}
                        required
                        className={`form-control form-control-lg ${usernameError || usernameExistsError ? 'is-invalid' : ''}`}
                    />
                    {isUsernameChecking && <div className="form-text">아이디 중복 확인 중...</div>}
                    {usernameError && <div className="invalid-feedback">{usernameError}</div>}
                    {usernameExistsError && <div className="invalid-feedback">{usernameExistsError}</div>} {/* 중복 에러 메시지 */}
                </div>
                <div className="mb-3">
                    <label htmlFor="password" className="form-label fw-bold">비밀번호:</label>
                    <input
                        type="password"
                        id="password"
                        value={password}
                        onChange={handleChange}
                        required
                        className={`form-control form-control-lg ${passwordMatchError ? 'is-invalid' : ''}`}
                    />
                </div>
                <div className="mb-4">
                    <label htmlFor="confirmPassword" className="form-label fw-bold">비밀번호 확인:</label>
                    <input
                        type="password"
                        id="confirmPassword"
                        value={confirmPassword}
                        onChange={handleChange}
                        required
                        className={`form-control form-control-lg ${passwordMatchError ? 'is-invalid' : ''}`}
                    />
                    {passwordMatchError && <div className="invalid-feedback">{passwordMatchError}</div>}
                </div>
                {/* Additional registration fields */}
                <div className="mb-3">
                    <label htmlFor="name" className="form-label fw-bold">이름:</label>
                    <input
                        type="text"
                        id="name"
                        value={name}
                        onChange={handleChange}
                        required
                        className="form-control form-control-lg"
                    />
                </div>
                <div className="mb-3">
                    <label htmlFor="nickname" className="form-label fw-bold">닉네임:</label>
                    <input
                        type="text"
                        id="nickname"
                        value={nickname}
                        onChange={handleChange}
                        required
                        className="form-control form-control-lg"
                    />
                </div>
                <div className="mb-3">
                    <label htmlFor="email" className="form-label fw-bold">이메일:</label>
                    <input
                        type="email"
                        id="email"
                        value={email}
                        onChange={handleChange}
                        required
                        className={`form-control form-control-lg ${emailError ? 'is-invalid' : ''}`}
                    />
                    {isEmailChecking && <div className="form-text">이메일 중복 확인 중...</div>}
                    {emailError && <div className="invalid-feedback">{emailError}</div>}
                </div>
                <div className="mb-4">
                    <label htmlFor="phoneNumber" className="form-label fw-bold">휴대전화번호:</label>
                    <input
                        type="tel"
                        id="phoneNumber"
                        value={phoneNumber}
                        onChange={handleChange}
                        required
                        className="form-control form-control-lg"
                    />
                </div>
                {/* /Additional registration fields */}

                <div className="d-grid">
                    <button
                        type="submit"
                        className="btn btn-success btn-lg rounded-pill fw-bold shadow-sm"
                        disabled={!isFormValid} // 유효성 검사 통과 시에만 활성화
                    >
                        회원가입
                    </button>
                </div>
            </form>
            <p className="mt-4 text-secondary">
                이미 계정이 있으신가요?{' '}
                <Link to="/login" className="text-primary fw-bold text-decoration-none">로그인</Link>
            </p>
            <Modal message={modalMessage} onClose={handleModalClose} />
        </div>
    );
}

export default RegisterForm;
