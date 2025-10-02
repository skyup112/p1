import React, { useEffect, useContext, useReducer, useState } from 'react'; // Import useState
import { userApi } from '../services/apiService';
import { AuthContext } from '../context/AuthContext';
import Modal from './Modal';

// 1. Initial State Definition
const initialState = {
    member: null,
    loading: true,
    error: null,
    // Profile Update Fields
    name: '',
    nickname: '',
    email: '',
    phoneNumber: '',
    // Password Change Fields
    currentPassword: '',
    newPassword: '',
    confirmNewPassword: '',
    // Account Deletion Field
    deletePassword: '',
    modalMessage: '',
    showDeleteConfirm: false,
};

// 2. Reducer Function Definition
function memberProfileReducer(state, action) {
    switch (action.type) {
        case 'FETCH_START':
            return { ...state, loading: true, error: null };
        case 'FETCH_SUCCESS':
            return {
                ...state,
                loading: false,
                member: action.payload,
                name: action.payload.name || '',
                nickname: action.payload.nickname || '',
                email: action.payload.email || '',
                phoneNumber: action.payload.phoneNumber || '',
            };
        case 'FETCH_ERROR':
            return { ...state, loading: false, error: action.payload };
        case 'SET_FIELD':
            return { ...state, [action.field]: action.value };
        case 'SET_MODAL_MESSAGE':
            return { ...state, modalMessage: action.payload };
        case 'CLEAR_MODAL_MESSAGE':
            return { ...state, modalMessage: '' };
        case 'OPEN_DELETE_CONFIRM':
            return { ...state, showDeleteConfirm: true };
        case 'CLOSE_DELETE_CONFIRM':
            return { ...state, showDeleteConfirm: false, deletePassword: '' }; // Clear password on close
        case 'RESET_PASSWORD_FIELDS':
            return { ...state, currentPassword: '', newPassword: '', confirmNewPassword: '' };
        case 'UPDATE_PROFILE_SUCCESS':
            return { ...state, member: action.payload, modalMessage: '프로필 정보가 성공적으로 수정되었습니다!' };
        case 'LOGOUT_INITIATED': // For handling logout after account deletion
            return { ...initialState, loading: false }; // Reset state completely
        default:
            return state;
    }
}

function MemberProfile() {
    const { user, loading: authLoading, logout } = useContext(AuthContext);
    const [state, dispatch] = useReducer(memberProfileReducer, initialState);
    const [activeTab, setActiveTab] = useState('profileView'); // State to manage active tab

    // Destructure state for easier access
    const {
        member, loading, error, name, nickname, email, phoneNumber,
        currentPassword, newPassword, confirmNewPassword, deletePassword,
        modalMessage, showDeleteConfirm
    } = state;

    // 사용자 프로필 정보 불러오기
    useEffect(() => {
        const fetchUserProfile = async () => {
            if (authLoading) return; // AuthContext 로딩 완료까지 대기
            if (!user) {
                dispatch({ type: 'FETCH_ERROR', payload: '로그인이 필요합니다.' });
                return;
            }

            dispatch({ type: 'FETCH_START' });
            try {
                const response = await userApi.getUserProfile();
                dispatch({ type: 'FETCH_SUCCESS', payload: response.data });
            } catch (err) {
                console.error('Error fetching user profile:', err);
                dispatch({ type: 'FETCH_ERROR', payload: '사용자 정보를 불러오는 데 실패했습니다.' });
            }
        };

        fetchUserProfile();
    }, [user, authLoading]);

    // 프로필 정보 수정 핸들러
    const handleUpdateProfile = async (e) => {
        e.preventDefault();
        dispatch({ type: 'CLEAR_MODAL_MESSAGE' });

        const updateRequest = {
            name,
            nickname,
            email,
            phoneNumber,
        };

        try {
            const response = await userApi.updateUserProfile(updateRequest);
            dispatch({ type: 'UPDATE_PROFILE_SUCCESS', payload: response.data });
        } catch (err) {
            console.error('프로필 업데이트 실패:', err);
            if (err.response && err.response.status === 409) {
                dispatch({ type: 'SET_MODAL_MESSAGE', payload: err.response.data || '이미 사용 중인 닉네임, 이메일 또는 휴대전화번호입니다.' });
            } else {
                dispatch({ type: 'SET_MODAL_MESSAGE', payload: '프로필 업데이트에 실패했습니다.' });
            }
        }
    };

    // 비밀번호 변경 핸들러
    const handleChangePassword = async (e) => {
        e.preventDefault();
        dispatch({ type: 'CLEAR_MODAL_MESSAGE' });

        if (newPassword !== confirmNewPassword) {
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '새 비밀번호와 확인 비밀번호가 일치하지 않습니다.' });
            return;
        }

        if (!newPassword.trim()) {
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '새 비밀번호를 입력해주세요.' });
            return;
        }

        const passwordChangeRequest = {
            currentPassword,
            newPassword,
        };

        try {
            await userApi.changePassword(passwordChangeRequest);
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '비밀번호가 성공적으로 변경되었습니다!' });
            dispatch({ type: 'RESET_PASSWORD_FIELDS' });
        } catch (err) {
            console.error('비밀번호 변경 실패:', err);
            if (err.response && err.response.status === 400) {
                dispatch({ type: 'SET_MODAL_MESSAGE', payload: err.response.data || '현재 비밀번호가 일치하지 않습니다.' });
            } else {
                dispatch({ type: 'SET_MODAL_MESSAGE', payload: '비밀번호 변경에 실패했습니다.' });
            }
        }
    };

    // 계정 탈퇴 확인 모달 열기
    const openDeleteConfirmModal = () => {
        dispatch({ type: 'OPEN_DELETE_CONFIRM' });
    };

    // 계정 탈퇴 핸들러
    const handleDeleteAccount = async (e) => {
        e.preventDefault(); // Prevent form submission
        dispatch({ type: 'CLEAR_MODAL_MESSAGE' });

        const deleteRequest = {
            password: deletePassword,
        };

        try {
            await userApi.deleteUserAccount(deleteRequest);
            dispatch({ type: 'SET_MODAL_MESSAGE', payload: '계정이 성공적으로 삭제되었습니다. 로그인 페이지로 이동합니다.' });
            dispatch({ type: 'CLOSE_DELETE_CONFIRM' }); // Close modal
            // Use a timeout to allow the modal message to be seen before redirecting/logging out
            setTimeout(() => {
                dispatch({ type: 'LOGOUT_INITIATED' }); // Reset state
                logout(); // Frontend logout (AuthContext)
            }, 2000);
        } catch (err) {
            console.error('계정 삭제 실패:', err);
            dispatch({ type: 'CLOSE_DELETE_CONFIRM' }); // Close modal regardless of success/failure

            if (err.response && err.response.status === 401) {
                dispatch({ type: 'SET_MODAL_MESSAGE', payload: err.response.data || '비밀번호가 일치하지 않습니다. 계정을 삭제할 수 없습니다.' });
            } else if (err.response && err.response.status === 403) {
                dispatch({ type: 'SET_MODAL_MESSAGE', payload: err.response.data || '관리자 계정은 탈퇴할 수 없습니다.' });
            } else {
                dispatch({ type: 'SET_MODAL_MESSAGE', payload: '계정 삭제에 실패했습니다.' });
            }
        }
    };

    const handleModalClose = () => {
        dispatch({ type: 'CLEAR_MODAL_MESSAGE' });
    };

    if (loading || authLoading) {
        return <div className="text-center py-4 text-secondary">프로필 정보를 불러오는 중...</div>;
    }

    if (error) {
        return <div className="text-center py-4 text-danger fw-bold">오류: {error}</div>;
    }

    if (!member) {
        return <div className="text-center py-4 text-info">프로필 정보를 찾을 수 없습니다.</div>;
    }

    return (
        // style={{ maxWidth: '900px' }}를 제거하고 my-5를 my-3으로 변경
        <div className="container my-3 p-4 bg-white rounded-lg shadow-lg">
            <h2 className="text-center text-primary mb-4 pb-2 border-bottom border-primary-subtle">내 정보 관리</h2>

            <div className="row">
                {/* Left Navigation Bar */}
                <div className="col-md-3">
                    <div className="d-flex flex-column nav nav-pills me-3" role="tablist" aria-orientation="vertical">
                        <button
                            className={`nav-link text-start mb-2 ${activeTab === 'profileView' ? 'active bg-primary text-white' : 'bg-light text-dark'}`}
                            onClick={() => setActiveTab('profileView')}
                            type="button"
                            role="tab"
                            aria-selected={activeTab === 'profileView'}
                        >
                           회원정보
                        </button>
                        <button
                            className={`nav-link text-start mb-2 ${activeTab === 'profileEdit' ? 'active bg-primary text-white' : 'bg-light text-dark'}`}
                            onClick={() => setActiveTab('profileEdit')}
                            type="button"
                            role="tab"
                            aria-selected={activeTab === 'profileEdit'}
                        >
                            회원정보 수정
                        </button>
                        <button
                            className={`nav-link text-start mb-2 ${activeTab === 'passwordChange' ? 'active bg-primary text-white' : 'bg-light text-dark'}`}
                            onClick={() => setActiveTab('passwordChange')}
                            type="button"
                            role="tab"
                            aria-selected={activeTab === 'passwordChange'}
                        >
                            비밀번호 변경
                        </button>
                        <button
                            className={`nav-link text-start ${activeTab === 'accountDelete' ? 'active bg-danger text-white' : 'bg-light text-dark'}`}
                            onClick={() => setActiveTab('accountDelete')}
                            type="button"
                            role="tab"
                            aria-selected={activeTab === 'accountDelete'}
                        >
                            회원 탈퇴
                        </button>
                    </div>
                </div>

                {/* Right Content Area */}
                <div className="col-md-9">
                    <div className="tab-content">
                        {/* 회원정보 상세보기 (View Mode) */}
                        {activeTab === 'profileView' && (
                            <div className="p-4 border border-light rounded-lg bg-light shadow-sm">
                                <h4 className="text-primary mb-3">내 정보</h4>
                                <div className="mb-2">
                                    <strong>아이디:</strong> {member.username}
                                </div>
                                <div className="mb-2">
                                    <strong>이름:</strong> {member.name}
                                </div>
                                <div className="mb-2">
                                    <strong>닉네임:</strong> {member.nickname}
                                </div>
                                <div className="mb-2">
                                    <strong>이메일:</strong> {member.email}
                                </div>
                                <div className="mb-2">
                                    <strong>휴대전화번호:</strong> {member.phoneNumber}
                                </div>
                                <div className="mb-2">
                                    <strong>가입일:</strong> {new Date(member.createdAt).toLocaleDateString()}
                                </div>
                                <div className="mt-4 d-grid">
                                    <button
                                        type="button"
                                        className="btn btn-primary rounded-pill px-4 py-2 fw-bold shadow-sm"
                                        onClick={() => setActiveTab('profileEdit')}
                                    >
                                        정보 수정하기
                                    </button>
                                </div>
                            </div>
                        )}

                        {/* 회원정보 수정 폼 */}
                        {activeTab === 'profileEdit' && (
                            <form onSubmit={handleUpdateProfile} className="p-4 border border-light rounded-lg bg-light shadow-sm">
                                <h4 className="text-primary mb-3">회원정보 수정</h4>
                                <div className="mb-3">
                                    <label htmlFor="username" className="form-label fw-bold">아이디:</label>
                                    <input
                                        type="text"
                                        id="username"
                                        className="form-control"
                                        value={member.username || ''}
                                        disabled // 아이디는 변경 불가
                                    />
                                </div>
                                <div className="mb-3">
                                    <label htmlFor="name" className="form-label fw-bold">이름:</label>
                                    <input
                                        type="text"
                                        id="name"
                                        className="form-control"
                                        value={name}
                                        onChange={(e) => dispatch({ type: 'SET_FIELD', field: 'name', value: e.target.value })}
                                        required
                                    />
                                </div>
                                <div className="mb-3">
                                    <label htmlFor="nickname" className="form-label fw-bold">닉네임:</label>
                                    <input
                                        type="text"
                                        id="nickname"
                                        className="form-control"
                                        value={nickname}
                                        onChange={(e) => dispatch({ type: 'SET_FIELD', field: 'nickname', value: e.target.value })}
                                        required
                                    />
                                </div>
                                <div className="mb-3">
                                    <label htmlFor="email" className="form-label fw-bold">이메일:</label>
                                    <input
                                        type="email"
                                        id="email"
                                        className="form-control"
                                        value={email}
                                        onChange={(e) => dispatch({ type: 'SET_FIELD', field: 'email', value: e.target.value })}
                                        required
                                    />
                                </div>
                                <div className="mb-4">
                                    <label htmlFor="phoneNumber" className="form-label fw-bold">휴대전화번호:</label>
                                    <input
                                        type="tel"
                                        id="phoneNumber"
                                        className="form-control"
                                        value={phoneNumber}
                                        onChange={(e) => dispatch({ type: 'SET_FIELD', field: 'phoneNumber', value: e.target.value })}
                                        required
                                    />
                                </div>
                                <div className="d-grid">
                                    <button type="submit" className="btn btn-primary rounded-pill px-4 py-2 fw-bold shadow-sm">
                                        정보 수정 완료
                                    </button>
                                </div>
                            </form>
                        )}

                        {/* 비밀번호 변경 폼 */}
                        {activeTab === 'passwordChange' && (
                            <form onSubmit={handleChangePassword} className="p-4 border border-light rounded-lg bg-light shadow-sm">
                                <h4 className="text-primary mb-3">비밀번호 변경</h4>
                                <div className="mb-3">
                                    <label htmlFor="currentPassword" className="form-label fw-bold">현재 비밀번호:</label>
                                    <input
                                        type="password"
                                        id="currentPassword"
                                        className="form-control"
                                        value={currentPassword}
                                        onChange={(e) => dispatch({ type: 'SET_FIELD', field: 'currentPassword', value: e.target.value })}
                                        required
                                    />
                                </div>
                                <div className="mb-3">
                                    <label htmlFor="newPassword" className="form-label fw-bold">새 비밀번호:</label>
                                    <input
                                        type="password"
                                        id="newPassword"
                                        className="form-control"
                                        value={newPassword}
                                        onChange={(e) => dispatch({ type: 'SET_FIELD', field: 'newPassword', value: e.target.value })}
                                        required
                                    />
                                </div>
                                <div className="mb-4">
                                    <label htmlFor="confirmNewPassword" className="form-label fw-bold">새 비밀번호 확인:</label>
                                    <input
                                        type="password"
                                        id="confirmNewPassword"
                                        className="form-control"
                                        value={confirmNewPassword}
                                        onChange={(e) => dispatch({ type: 'SET_FIELD', field: 'confirmNewPassword', value: e.target.value })}
                                        required
                                    />
                                </div>
                                <div className="d-grid">
                                    <button type="submit" className="btn btn-success rounded-pill px-4 py-2 fw-bold shadow-sm">
                                        비밀번호 변경
                                    </button>
                                </div>
                            </form>
                        )}

                        {/* 계정 탈퇴 섹션 */}
                        {activeTab === 'accountDelete' && (
                            <div className="p-4 border border-light rounded-lg bg-light shadow-sm">
                                <h4 className="text-danger mb-3">계정 탈퇴</h4>
                                <p className="text-muted">계정을 삭제하면 모든 정보가 영구적으로 삭제되며 복구할 수 없습니다.</p>
                                <div className="d-grid">
                                    <button
                                        type="button"
                                        className="btn btn-danger rounded-pill px-4 py-2 fw-bold shadow-sm"
                                        onClick={openDeleteConfirmModal}
                                    >
                                        계정 삭제
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            <Modal message={modalMessage} onClose={handleModalClose} />

            {/* 계정 탈퇴 확인 모달 ( unchanged ) */}
            {showDeleteConfirm && (
                <div className="modal d-block" tabIndex="-1" role="dialog" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <div className="modal-dialog modal-dialog-centered" role="document">
                        <div className="modal-content">
                            <div className="modal-header bg-danger text-white">
                                <h5 className="modal-title">계정 삭제 확인</h5>
                                <button type="button" className="btn-close btn-close-white" aria-label="Close" onClick={() => dispatch({ type: 'CLOSE_DELETE_CONFIRM' })}></button>
                            </div>
                            <form onSubmit={handleDeleteAccount}>
                                <div className="modal-body">
                                    <p className="lead text-center">정말로 계정을 삭제하시겠습니까?</p>
                                    <p className="text-muted text-center">이 작업은 되돌릴 수 없습니다. 확인을 위해 비밀번호를 입력해주세요.</p>
                                    <div className="mb-3">
                                        <label htmlFor="deletePassword" className="form-label fw-bold">비밀번호:</label>
                                        <input
                                            type="password"
                                            id="deletePassword"
                                            className="form-control"
                                            value={deletePassword}
                                            onChange={(e) => dispatch({ type: 'SET_FIELD', field: 'deletePassword', value: e.target.value })}
                                            required
                                        />
                                    </div>
                                </div>
                                <div className="modal-footer d-flex justify-content-center">
                                    <button type="submit" className="btn btn-danger rounded-pill px-4 fw-bold">
                                        삭제
                                    </button>
                                    <button type="button" onClick={() => dispatch({ type: 'CLOSE_DELETE_CONFIRM' })} className="btn btn-secondary rounded-pill px-4 fw-bold">
                                        취소
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default MemberProfile;