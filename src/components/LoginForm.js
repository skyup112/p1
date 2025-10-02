import React, { useReducer, useContext } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import Modal from './Modal';

// 1. Initial State Definition
const initialState = {
    username: '',
    password: '',
    modalMessage: '',
};

// 2. Reducer Function Definition
function loginFormReducer(state, action) {
    switch (action.type) {
        case 'SET_FIELD':
            return { ...state, [action.field]: action.value };
        case 'SET_MODAL_MESSAGE':
            return { ...state, modalMessage: action.payload };
        case 'CLEAR_MODAL_MESSAGE':
            return { ...state, modalMessage: '' };
        case 'RESET_FORM':
            return { ...initialState, modalMessage: state.modalMessage };
        default:
            return state;
    }
}

function LoginForm() {
    const [state, dispatch] = useReducer(loginFormReducer, initialState);
    const { login } = useContext(AuthContext);
    const navigate = useNavigate();

    const { username, password, modalMessage } = state;

    const handleSubmit = async (e) => {
        e.preventDefault();
        dispatch({ type: 'CLEAR_MODAL_MESSAGE' });

        try {
            await login(username, password);
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '로그인 성공!' });
            dispatch({ type: 'RESET_FORM' });
            navigate('/');
        } catch (error) {
            console.error('로그인 실패:', error);
            if (error.response && error.response.status === 401) {
                dispatch({ type: 'SET_MODAL_MESSAGE', payload: '아이디 또는 비밀번호가 올바르지 않습니다.' });
            } else {
                dispatch({ type: 'SET_MODAL_MESSAGE', payload: '로그인 중 오류가 발생했습니다.' });
            }
        }
    };

    const handleModalClose = () => {
        dispatch({ type: 'CLEAR_MODAL_MESSAGE' });
    };

    return (
        <div className="container my-3 p-4 bg-white rounded-lg shadow-lg">
            <h2 className="text-center text-primary mb-4">로그인</h2>
            <form onSubmit={handleSubmit}>
                <div className="row g-0 align-items-center mb-4">
                    {/* 아이디/비밀번호 입력 섹션 */}
                    <div className="col-8 d-flex flex-column align-items-end">
                        {/* 아이디 입력 블록 */}
                        <div className="mb-3 d-flex align-items-center" style={{ width: 'fit-content' }}>
                            <label
                                htmlFor="username"
                                className="form-label fw-bold me-2 mb-0"
                                style={{ minWidth: '4rem', textAlign: 'right' }}
                            >
                                아이디:
                            </label>
                            <input
                                type="text"
                                id="username"
                                value={username}
                                onChange={(e) => dispatch({ type: 'SET_FIELD', field: 'username', value: e.target.value })}
                                required
                                className="form-control form-control-lg"
                                style={{ width: '36rem' }}
                            />
                        </div>
                        {/* 비밀번호 입력 블록 */}
                        <div className="mb-0 d-flex align-items-center" style={{ width: 'fit-content' }}>
                            <label
                                htmlFor="password"
                                className="form-label fw-bold me-2 mb-0"
                                style={{ minWidth: '4rem', textAlign: 'right' }}
                            >
                                비밀번호:
                            </label>
                            <input
                                type="password"
                                id="password"
                                value={password}
                                onChange={(e) => dispatch({ type: 'SET_FIELD', field: 'password', value: e.target.value })}
                                required
                                className="form-control form-control-lg"
                                style={{ width: '36rem' }}
                            />
                        </div>
                    </div>

                    {/* 로그인 버튼 섹션 (col-auto로 변경하여 내용물 너비만큼만 차지) */}
                    {/* margin-left를 10px 추가하여 입력 블록과 간격 띄움 */}
                    <div className="col-auto d-flex align-items-center justify-content-end mt-2" style={{ marginLeft: '10px' }}>
                        <button
                            type="submit"
                            className="btn btn-primary fw-bold shadow-sm"
                            style={{
                                width: '6.75rem',
                                height: '6.75rem',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                lineHeight: '1.2',
                                fontSize: '1rem'
                            }}
                        >
                            로그인
                        </button>
                    </div>
                </div>
            </form>
            <p className="mt-4 text-secondary text-center">
                계정이 없으신가요?{' '}
                <Link to="/register" className="text-primary fw-bold text-decoration-none">회원가입</Link>
            </p>
            <Modal message={modalMessage} onClose={handleModalClose} />
        </div>
    );
}

export default LoginForm;